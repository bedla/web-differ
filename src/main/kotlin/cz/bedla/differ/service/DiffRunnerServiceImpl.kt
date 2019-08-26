package cz.bedla.differ.service

import cz.bedla.differ.dto.*
import cz.bedla.differ.utils.findEntity
import cz.bedla.differ.utils.findPropertyAs
import cz.bedla.differ.utils.getDiffs
import cz.bedla.differ.utils.setPropertyZonedDateTime
import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.PersistentEntityStore
import jetbrains.exodus.entitystore.StoreTransaction
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.ExecutionException
import javax.net.ssl.SSLSocketFactory


class DiffRunnerServiceImpl(
    private val persistentEntityStore: PersistentEntityStore,
    private val emailSender: EmailSender
) : DiffRunnerService, InitializingBean {

    private lateinit var sslSocketFactory: SSLSocketFactory

    override fun afterPropertiesSet() {
        sslSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
    }

    override fun run(webPageId: String) {
        val canRun = checkAndUpdate(webPageId)
        if (canRun) {
            findDifference(webPageId)
        } else {
            error("WebPage($webPageId) not found")
        }
    }

    private fun findDifference(webPageId: String) {
        persistentEntityStore.executeInTransaction { tx ->
            val webPageEntity = tx.findWebPage(webPageId) ?: error("Unable to find WebPage($webPageId)")
            val webPage = webPageEntity.createWebPageDetail(emptyList() /* ignore diffs */)
            val url = webPage.url
            val selector = webPage.selector
            val user = webPageEntity.getLink("user")?.createUser()
                ?: error("Unable to find user for webPageId=$webPageId")

            try {
                val doc = Jsoup.connect(url)
                    .sslSocketFactory(if (url.startsWith("https://")) sslSocketFactory else null)
                    .userAgent(userAgent)
                    .timeout(5 * 1000)
                    .get() ?: error("Unable to get document from URL $url")

                val currentText = doc.select(selector).first()?.text()
                val lastContent = tx.findLastContent(webPageEntity)
                if (currentText == null) {
                    log.info("Unable to find selector '$selector' at web-page URL '$url'")
                    tx.saveInvalidSelectorRun(webPageEntity, selector)
                } else {
                    if (lastContent == null) {
                        log.info("First run for web-page URL '$url'")
                        tx.saveFirstRun(webPageEntity, currentText)
                    } else {
                        if (currentText == lastContent) {
                            log.info("There is no diff on web-page.id=${webPageId} URL '$url' for selector '$selector' value '$currentText'")
                        } else {
                            log.info("Diff found for web-page URL '$url' for selector '$selector': '$lastContent' -> '$currentText'")
                            tx.saveDiff(webPageEntity, currentText)
                            val future = emailSender.sendEmail(user, webPageEntity.createWebPageDetail(tx.getDiffs(webPageEntity)))
                            future.get()
                        }
                    }
                }
            } catch (e: Exception) {
                tx.saveExceptionRun(webPageEntity,
                    url,
                    if (e is ExecutionException) (e.cause ?: e) else e)
                if (tx.countLatestContinuousErrors(webPageEntity) >= 5) {
                    tx.saveStopFurtherExecution(webPageEntity, 5)
                }
            }
        }
    }

    private fun StoreTransaction.saveDiff(webPageEntity: Entity, diff: String) {
        val entity = newEntity("Diff")
        entity.setProperty("content", diff)
        entity.setPropertyZonedDateTime("created", ZonedDateTime.now())
        entity.setLink("webPage", webPageEntity)
    }

    private fun StoreTransaction.saveFirstRun(webPageEntity: Entity, content: String) {
        saveDiff(webPageEntity, content)
    }

    private fun StoreTransaction.saveInvalidSelectorRun(webPageEntity: Entity, selector: String) {
        val entity = newEntity("Diff")
        entity.setProperty("invalidSelector", selector)
        entity.setPropertyZonedDateTime("created", ZonedDateTime.now())
        entity.setLink("webPage", webPageEntity)
    }

    private fun StoreTransaction.saveExceptionRun(webPageEntity: Entity, url: String, e: Throwable) {
        val uuid = UUID.randomUUID().toString()

        val entity = newEntity("Diff")
        entity.setProperty("exceptionUuid", uuid)
        entity.setProperty("exceptionName", e.javaClass.name)
        entity.setProperty("exceptionMessage", e.message ?: "")
        entity.setPropertyZonedDateTime("created", ZonedDateTime.now())
        entity.setLink("webPage", webPageEntity)

        log.error("There was error with diff run uuid=$uuid and URL=$url", e)
    }

    private fun checkAndUpdate(webPageId: String): Boolean {
        return persistentEntityStore.computeInTransaction { tx ->
            val entity = tx.findWebPage(webPageId)
            if (entity != null) {
                entity.setPropertyZonedDateTime("lastRun", ZonedDateTime.now())
                true
            } else {
                false
            }
        }
    }

    private fun StoreTransaction.findLastContent(webPageEntity: Entity): String? {
        val diffs = findLinks("Diff", webPageEntity, "webPage")
            .intersect(findWithProp("Diff", "content"))
        val sorted = sort("Diff", "created", diffs, false)
        return sorted.first?.findPropertyAs("content")
    }

    private fun StoreTransaction.countLatestContinuousErrors(webPageEntity: Entity): Int {
        val diffs = findLinks("Diff", webPageEntity, "webPage")
        val errors = sort("Diff", "created", diffs, false).asSequence()
            .map { it.createDiff() }
            .takeWhile { it is DiffInvalidSelector || it is DiffError }
            .toList()
        return errors.size
    }

    private fun StoreTransaction.saveStopFurtherExecution(webPageEntity: Entity, countErrors: Int) {
        log.info("Execution of $webPageEntity automatically stopped after $countErrors continuous errors")

        val entity = newEntity("Diff")
        entity.setProperty("countErrors", countErrors)
        entity.setPropertyZonedDateTime("created", ZonedDateTime.now())
        entity.setLink("webPage", webPageEntity)

        webPageEntity.setProperty("enabled", false)
    }

    private fun StoreTransaction.findWebPage(webPageId: String): Entity? {
        val entity = findEntity(webPageId) ?: return null
        return if (entity.type == "WebPage") entity else null
    }

    companion object {
        private const val userAgent = """Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36"""

        private val log = LoggerFactory.getLogger(DiffRunnerServiceImpl::class.java)!!
    }
}

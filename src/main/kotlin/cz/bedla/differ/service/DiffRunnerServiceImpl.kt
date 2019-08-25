package cz.bedla.differ.service

import cz.bedla.differ.utils.findEntity
import cz.bedla.differ.utils.findPropertyAs
import cz.bedla.differ.utils.getPropertyAs
import cz.bedla.differ.utils.setPropertyZonedDateTime
import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.PersistentEntityStore
import jetbrains.exodus.entitystore.StoreTransaction
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import java.time.ZonedDateTime
import java.util.*
import javax.net.ssl.SSLSocketFactory


class DiffRunnerServiceImpl(
    private val persistentEntityStore: PersistentEntityStore
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
            val url: String = webPageEntity.getPropertyAs("url")
            val selector: String = webPageEntity.getPropertyAs("selector")

            try {
                val doc = Jsoup.connect(url)
                    .sslSocketFactory(if (url.startsWith("https://")) sslSocketFactory else null)
                    .userAgent(userAgent)
                    .timeout(1000)
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
                            log.info("There is not difference at web-page URL '$url' for selector '$selector' value '$currentText'")
                        } else {
                            tx.saveDiff(webPageEntity, currentText)
                        }
                    }
                }
            } catch (e: Exception) {
                tx.saveExceptionRun(webPageEntity, e)
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

    private fun StoreTransaction.saveExceptionRun(webPageEntity: Entity, e: Exception) {
        val uuid = UUID.randomUUID().toString()

        val entity = newEntity("Diff")
        entity.setProperty("exceptionUuid", uuid)
        entity.setProperty("exceptionName", e.javaClass.name)
        entity.setProperty("exceptionMessage", e.message ?: "")
        entity.setPropertyZonedDateTime("created", ZonedDateTime.now())
        entity.setLink("webPage", webPageEntity)

        log.error("There was error with diff run uuid=$uuid", e)
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

    private fun StoreTransaction.findWebPage(webPageId: String): Entity? {
        val entity = findEntity(webPageId) ?: return null
        return if (entity.type == "WebPage") entity else null
    }

    companion object {
        private val userAgent = """Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36"""

        private val log = LoggerFactory.getLogger(DiffRunnerServiceImpl::class.java)
    }
}

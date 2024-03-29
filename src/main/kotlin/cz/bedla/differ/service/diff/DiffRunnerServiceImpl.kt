package cz.bedla.differ.service.diff

import cz.bedla.differ.dto.*
import cz.bedla.differ.service.HtmlPageService
import cz.bedla.differ.service.email.EmailSender
import cz.bedla.differ.utils.findEntity
import cz.bedla.differ.utils.findPropertyAs
import cz.bedla.differ.utils.getDiffs
import cz.bedla.differ.utils.setPropertyZonedDateTime
import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.PersistentEntityStore
import jetbrains.exodus.entitystore.StoreTransaction
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class DiffRunnerServiceImpl(
    private val persistentEntityStore: PersistentEntityStore,
    private val emailSender: EmailSender,
    private val htmlPageService: HtmlPageService,
    private val clock: Clock,
    private val uuidGenerator: () -> UUID
) : DiffRunnerService {

    override fun run(webPageId: String) {
        val canRun = checkAndUpdate(webPageId)
        if (canRun) {
            findDifference(webPageId)
        } else {
            error("WebPage($webPageId) not found")
        }
    }

    private fun findDifference(webPageId: String) {
        val userWebPage = persistentEntityStore.getUserWebPage(webPageId)
        val result = execute(userWebPage)

        result.save()
        when (result) {
            is Result.Diff -> sendEmail(webPageId)
        }

        checkErrors(userWebPage)
    }

    private fun sendEmail(webPageId: String) {
        val userWebPage = persistentEntityStore.getUserWebPage(webPageId, withDiffs = true)
        val future = emailSender.sendEmail(userWebPage.user, userWebPage.webPage)
        try {
            future.get(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Result.Error(webPageId, userWebPage.webPage.url, e,
                Result.Error.ErrorContext(persistentEntityStore, clock, uuidGenerator)).save()
        }
    }

    private fun checkErrors(userWebPage: UserWebPage) = persistentEntityStore.executeInTransaction { tx ->
        val webPageEntity = tx.getWebPage(userWebPage.webPage.id)
        val errorsCount = tx.countLatestContinuousErrors(webPageEntity)
        if (errorsCount >= maxErrors) {
            tx.saveStopFurtherExecution(webPageEntity, clock) { it.setProperty("countErrors", errorsCount) }
        }

        val latestDiff = tx.getDiffs(webPageEntity).firstOrNull()
        if (latestDiff is DiffError && latestDiff.exceptionName.contains("GoogleJsonResponseException")) {
            tx.saveStopFurtherExecution(webPageEntity, clock) { it.setProperty("apiErrorGmail", true) }
        }
    }

    private fun execute(userWebPage: UserWebPage): Result<*> {
        val webPageId = userWebPage.webPage.id
        val url = userWebPage.webPage.url
        try {
            val selector = userWebPage.webPage.selector
            val currentText = htmlPageService.contentOfSelector(url, selector)
                .let { if ((it?.length ?: -1) > 1000) null else it }
            val lastContent = userWebPage.lastContent
            if (currentText == null) {
                log.info("Unable to find web-page.id=${webPageId} selector '$selector' at web-page URL '$url'")
                return Result.InvalidSelector(webPageId,
                    selector,
                    Result.InvalidSelector.InvalidSelectorContext(persistentEntityStore, clock))
            } else {
                return if (lastContent == null) {
                    log.info("First run for web-page.id=${webPageId} URL '$url' and content '${StringUtils.left(currentText, 100)}'")
                    Result.FirstRun(webPageId, currentText, Result.FirstRun.FirstRunContext(persistentEntityStore, clock))
                } else {
                    if (currentText == lastContent) {
                        log.info("There is no diff on web-page.id=${webPageId} URL '$url' for selector '$selector' value '${StringUtils.left(currentText, 100)}'")
                        Result.Equals(webPageId, currentText)
                    } else {
                        log.info("Diff found for web-page.id=${webPageId} URL '$url' for selector '$selector': '${StringUtils.left(lastContent, 100)}' -> '${StringUtils.left(currentText, 100)}'")
                        Result.Diff(webPageId, lastContent, currentText, Result.Diff.DiffContext(persistentEntityStore, clock))
                    }
                }
            }
        } catch (e: Exception) {
            return Result.Error(webPageId, url, e, Result.Error.ErrorContext(persistentEntityStore, clock, uuidGenerator))
        }
    }

    private fun checkAndUpdate(webPageId: String): Boolean {
        return persistentEntityStore.computeInTransaction { tx ->
            val entity = tx.findWebPage(webPageId)
            if (entity != null) {
                entity.setPropertyZonedDateTime("lastRun", ZonedDateTime.now(clock))
                true
            } else {
                false
            }
        }
    }

    data class UserWebPage(
        val user: User,
        val webPage: WebPageDetail,
        val lastContent: String?
    )

    private sealed class Result<T : Result.Context> {
        abstract fun save()

        open class Context

        internal data class InvalidSelector(
            val webPageId: String,
            val invalidSelector: String,
            val context: InvalidSelectorContext
        ) : Result<InvalidSelector.InvalidSelectorContext>() {
            override fun save() = context.persistentEntityStore.executeInTransaction { tx ->
                val webPageEntity = tx.getWebPage(webPageId)
                tx.saveInvalidSelectorRun(webPageEntity, invalidSelector, context.clock)
            }

            class InvalidSelectorContext(
                val persistentEntityStore: PersistentEntityStore,
                val clock: Clock
            ) : Context()
        }

        internal data class FirstRun(
            val webPageId: String,
            val text: String,
            val context: FirstRunContext
        ) : Result<FirstRun.FirstRunContext>() {
            override fun save() = context.persistentEntityStore.executeInTransaction { tx ->
                val webPageEntity = tx.getWebPage(webPageId)
                tx.saveFirstRun(webPageEntity, text, context.clock)
            }

            class FirstRunContext(
                val persistentEntityStore: PersistentEntityStore,
                val clock: Clock
            ) : Context()
        }

        internal data class Equals(
            val webPageId: String,
            val text: String
        ) : Result<Context>() {
            override fun save() {
                // no need to save anything when same content
            }
        }

        internal data class Diff(
            val webPageId: String,
            val oldText: String,
            val newText: String,
            val context: DiffContext
        ) : Result<Diff.DiffContext>() {
            override fun save() = context.persistentEntityStore.executeInTransaction { tx ->
                val webPageEntity = tx.getWebPage(webPageId)
                tx.saveDiff(webPageEntity, newText, context.clock)
            }

            class DiffContext(
                val persistentEntityStore: PersistentEntityStore,
                val clock: Clock
            ) : Context()
        }

        internal data class Error(
            val webPageId: String,
            val url: String,
            val e: Exception,
            val context: ErrorContext
        ) : Result<Error.ErrorContext>() {
            override fun save() = context.persistentEntityStore.executeInTransaction { tx ->
                val webPageEntity = tx.getWebPage(webPageId)
                tx.saveExceptionRun(webPageEntity,
                    url,
                    context.clock,
                    context.uuidGenerator,
                    if (e is ExecutionException) (e.cause ?: e) else e)
            }

            class ErrorContext(
                val persistentEntityStore: PersistentEntityStore,
                val clock: Clock,
                val uuidGenerator: () -> UUID
            ) : Context()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DiffRunnerServiceImpl::class.java)!!

        private const val maxErrors = 5

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

        private fun StoreTransaction.saveStopFurtherExecution(
            webPageEntity: Entity,
            clock: Clock,
            modifier: (Entity) -> Unit
        ) {
            log.info("Execution of $webPageEntity automatically stopped because of some errors")

            val entity = newEntity("Diff")
            modifier(entity)
            entity.setPropertyZonedDateTime("created", ZonedDateTime.now(clock))
            entity.setLink("webPage", webPageEntity)

            webPageEntity.setProperty("enabled", false)
        }

        private fun StoreTransaction.findWebPage(webPageId: String): Entity? {
            val entity = findEntity(webPageId) ?: return null
            return if (entity.type == "WebPage") entity else null
        }

        private fun StoreTransaction.getWebPage(webPageId: String): Entity {
            return findEntity(webPageId) ?: error("Unable to find WebPage($webPageId)")
        }

        private fun PersistentEntityStore.getUserWebPage(webPageId: String, withDiffs: Boolean = false): UserWebPage = computeInTransaction { tx ->
            val webPageEntity = tx.getWebPage(webPageId)
            val webPage = webPageEntity.createWebPageDetail(
                if (withDiffs) tx.getDiffs(webPageEntity) else emptyList())
            val user = webPageEntity.getLink("user")?.createUser()
                ?: error("Unable to find user for webPageId=$webPageId")
            return@computeInTransaction UserWebPage(user, webPage, tx.findLastContent(webPageEntity))
        }

        private fun StoreTransaction.saveDiff(webPageEntity: Entity, diff: String, clock: Clock) {
            val entity = newEntity("Diff")
            entity.setProperty("content", diff)
            entity.setPropertyZonedDateTime("created", ZonedDateTime.now(clock))
            entity.setLink("webPage", webPageEntity)
        }

        private fun StoreTransaction.saveFirstRun(webPageEntity: Entity, content: String, clock: Clock) {
            saveDiff(webPageEntity, content, clock)
        }

        private fun StoreTransaction.saveInvalidSelectorRun(webPageEntity: Entity, selector: String, clock: Clock) {
            val entity = newEntity("Diff")
            entity.setProperty("invalidSelector", selector)
            entity.setPropertyZonedDateTime("created", ZonedDateTime.now(clock))
            entity.setLink("webPage", webPageEntity)
        }

        private fun StoreTransaction.saveExceptionRun(webPageEntity: Entity, url: String, clock: Clock, uuidGenerator: () -> UUID, e: Throwable) {
            val uuid = uuidGenerator().toString()

            val entity = newEntity("Diff")
            entity.setProperty("exceptionUuid", uuid)
            entity.setProperty("exceptionName", e.javaClass.name)
            entity.setProperty("exceptionMessage", e.message ?: "")
            entity.setPropertyZonedDateTime("created", ZonedDateTime.now(clock))
            entity.setLink("webPage", webPageEntity)

            log.error("There was error with diff run uuid='$uuid' and URL='$url'", e)
        }
    }
}

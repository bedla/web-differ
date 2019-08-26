package cz.bedla.differ.service

import cz.bedla.differ.utils.findEntity
import jetbrains.exodus.entitystore.PersistentEntityStore
import org.slf4j.LoggerFactory
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.Scheduled
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean

class DiffRunnerExecutorImpl(
    private val taskExecutor: TaskExecutor,
    private val persistentEntityStore: PersistentEntityStore,
    private val diffRunnerService: DiffRunnerService
) : DiffRunnerExecutor {
    private val canRun = AtomicBoolean(false)

    override fun start() {
        canRun.set(true)
    }

    @Scheduled(cron = "0 */1 * * * *")
    fun execute() {
        log.info("Differ execution is about to run")
        runExecution()
    }

    private fun runExecution() = persistentEntityStore.executeInTransaction { tx ->
        val enabled = tx.find("WebPage", "enabled", true)
        val lastRunMillis = ZonedDateTime.now().minusMinutes(5).toInstant().toEpochMilli()
        val olderRun = tx.find("WebPage", "lastRun", Long.MIN_VALUE, lastRunMillis)
        val toRun = enabled.intersect(olderRun)
        log.info("${toRun.size()} diff(s) is going to be run")
        for (entity in toRun) {
            val entityId = entity.id.toString()
            executeAsync(entityId)
        }
    }

    private fun executeAsync(webPageId: String) {
        if (canRun.get()) {
            taskExecutor.execute {
                diffRunnerService.run(webPageId)
            }
        } else {
            log.info("Unable to executeAsync WebPage($webPageId) because service is disabled")
        }
    }

    override fun scheduleNow(webPageId: String) = persistentEntityStore.executeInTransaction { tx ->
        val entity = tx.findEntity(webPageId)
        if (entity?.type == "WebPage") {
            executeAsync(entity.id.toString())
        } else {
            error("Unable to find WebPage.id=$webPageId to execute")
        }
    }

    override fun close() {
        canRun.set(false)
    }

    companion object {
        private val log = LoggerFactory.getLogger(DiffRunnerExecutorImpl::class.java)!!
    }
}

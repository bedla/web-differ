package cz.bedla.differ.service

import cz.bedla.differ.utils.findEntity
import cz.bedla.differ.utils.setPropertyZonedDateTime
import jetbrains.exodus.entitystore.PersistentEntityStore
import java.time.ZonedDateTime

class DiffRunnerServiceImpl(
    private val persistentEntityStore: PersistentEntityStore
) : DiffRunnerService {
    override fun run(webPageId: String) {
        val canRun = checkAndUpdate(webPageId)
        if (canRun) {
            println("--- +++ ---")
        } else {
            error("WebPage($webPageId) not found")
        }
    }

    private fun checkAndUpdate(webPageId: String): Boolean {
        return persistentEntityStore.computeInTransaction { tx ->
            val entity = tx.findEntity(webPageId) ?: return@computeInTransaction false
            if (entity.type == "WebPage") {
                entity.setPropertyZonedDateTime("lastRun", ZonedDateTime.now())
                true
            } else {
                false
            }
        }
    }
}

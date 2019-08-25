package cz.bedla.differ.service

import cz.bedla.differ.dto.*
import cz.bedla.differ.utils.findEntity
import cz.bedla.differ.utils.setPropertyZonedDateTime
import jetbrains.exodus.entitystore.PersistentEntityStore
import java.time.ZonedDateTime

class WebPageServiceImpl(
    private val persistentEntityStore: PersistentEntityStore
) : WebPageService {
    override fun find(id: String, userId: String): WebPageDetail? = persistentEntityStore.computeInTransaction { tx ->
        val entity = tx.findEntity(id) ?: return@computeInTransaction null
        val userEntity = tx.getUserEntity(userId)
        if (entity.getLink("user") == userEntity) {
            entity.createWebPageDetail()
        } else {
            null
        }
    }

    override fun findAll(userId: String): List<WebPageSimple> = persistentEntityStore.computeInTransaction { tx ->
        val userEntity = tx.getUserEntity(userId)
        val webPages = tx.findLinks("WebPage", userEntity, "user")
        tx.sort("WebPage", "name", webPages, true)
            .asSequence()
            .filterNotNull()
            .map { it.createWebPage() }
            .toList()
    }

    override fun create(userId: String, request: CreateWebPage): String = persistentEntityStore.computeInTransaction { tx ->
        val entity = tx.newEntity("WebPage")
        entity.setProperty("name", request.name)
        entity.setProperty("url", request.url)
        entity.setProperty("selector", request.selector)
        entity.setProperty("enabled", request.enabled)
        entity.setPropertyZonedDateTime("created", ZonedDateTime.now())
        val userEntity = tx.getUserEntity(userId)
        entity.addLink("user", userEntity)
        entity.id.toString()
    }

    override fun update(userId: String, id: String, request: UpdateWebPage): Boolean = persistentEntityStore.computeInTransaction { tx ->
        val entity = tx.findEntity(id) ?: return@computeInTransaction false
        val userEntity = tx.getUserEntity(userId)
        if (entity.getLink("user") == userEntity) {
            entity.setProperty("name", request.name)
            entity.setProperty("url", request.url)
            entity.setProperty("selector", request.selector)
            entity.setProperty("enabled", request.enabled)
            true
        } else {
            error("User entity $entity does not belong to user '$userId'")
        }
    }

    override fun delete(userId: String, id: String): Boolean = persistentEntityStore.computeInTransaction { tx ->
        val entity = tx.findEntity(id) ?: return@computeInTransaction false
        val userEntity = tx.getUserEntity(userId)
        if (entity.getLink("user") == userEntity) {
            entity.delete()
        } else {
            error("User entity $entity does not belong to user '$userId'")
        }
    }

    override fun execute(userId: String, id: String): Boolean = persistentEntityStore.computeInTransaction { tx ->
        val entity = tx.findEntity(id) ?: return@computeInTransaction false
        val userEntity = tx.getUserEntity(userId)
        if (entity.getLink("user") == userEntity) {
            false
        } else {
            error("User entity $entity does not belong to user '$userId'")
        }
    }
}

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


    override fun findAll(userId: String): List<WebPage> = persistentEntityStore.computeInTransaction { tx ->
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
        entity.setProperty("enabled", request.enabled)
        entity.setPropertyZonedDateTime("created", ZonedDateTime.now())
        val userEntity = tx.getUserEntity(userId)
        entity.addLink("user", userEntity)
        entity.id.toString()
    }

    override fun update(userId: String, id: String, request: UpdateWebPage): Unit = persistentEntityStore.computeInTransaction { tx ->
        val entity = tx.getEntity(tx.toEntityId(id))
        val userEntity = tx.getUserEntity(userId)
        if (entity.getLink("user") == userEntity) {
            entity.setProperty("name", request.name)
            entity.setProperty("url", request.url)
            entity.setProperty("enabled", request.enabled)
        } else {
            error("User entity $entity does not belong to user '$userId'")
        }
    }
}

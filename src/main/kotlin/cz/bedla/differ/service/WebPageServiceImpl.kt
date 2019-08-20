package cz.bedla.differ.service

import cz.bedla.differ.dto.CreateWebPage
import cz.bedla.differ.dto.UpdateWebPage
import cz.bedla.differ.dto.WebPage
import cz.bedla.differ.dto.WebPageDetail
import cz.bedla.differ.utils.setPropertyZonedDateTime
import jetbrains.exodus.entitystore.PersistentEntityStore
import jetbrains.exodus.entitystore.StoreTransaction
import java.time.ZonedDateTime

class WebPageServiceImpl(
    private val persistentEntityStore: PersistentEntityStore
) : WebPageService {
    override fun get(id: String, userId: String): WebPageDetail? = persistentEntityStore.computeInTransaction { tx ->
        val entity = tx.getEntity(tx.toEntityId(id))
        val userEntity = findUser(userId, tx)
        return@computeInTransaction if (entity.getLink("user") == userEntity) {
            WebPageDetail.createFromEntity(entity)
        } else {
            null
        }
    }


    override fun findAll(userId: String): List<WebPage> = persistentEntityStore.computeInTransaction { tx ->
        val userIterable = tx.find("User", "subject", userId)
        val webPages = tx.findLinks("WebPage", userIterable, "user")
        tx.sort("WebPage", "name", webPages, true)
            .asSequence()
            .filterNotNull()
            .map { WebPage.createFromEntity(it) }
            .toList()
    }

    override fun create(userId: String, request: CreateWebPage): String = persistentEntityStore.computeInTransaction { tx ->
        val entity = tx.newEntity("WebPage")
        entity.setProperty("name", request.name)
        entity.setProperty("url", request.url)
        entity.setProperty("enabled", request.enabled)
        entity.setPropertyZonedDateTime("created", ZonedDateTime.now())
        val userEntity = tx.find("User", "subject", userId).first
            ?: error("Unable to find entity user.subject=$userId")
        entity.addLink("user", userEntity)
        entity.id.toString()
    }

    override fun update(userId: String, id: String, request: UpdateWebPage): Unit = persistentEntityStore.computeInTransaction { tx ->
        val entity = tx.getEntity(tx.toEntityId(id))
        val userEntity = findUser(userId, tx)
        if (entity.getLink("user") == userEntity) {
            entity.setProperty("name", request.name)
            entity.setProperty("url", request.url)
            entity.setProperty("enabled", request.enabled)
        } else {
            error("User entity $entity does not belong to user '$userId'")
        }
    }

    private fun findUser(userId: String, tx: StoreTransaction) =
        (tx.find("User", "subject", userId).first
            ?: error("Unable to find entity user.subject=$userId"))
}

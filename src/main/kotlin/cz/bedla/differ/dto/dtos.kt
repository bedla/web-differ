package cz.bedla.differ.dto

import cz.bedla.differ.utils.getPropertyAsBoolean
import cz.bedla.differ.utils.getPropertyAsString
import cz.bedla.differ.utils.getPropertyAsZonedDateTime
import jetbrains.exodus.entitystore.Entity
import java.time.ZonedDateTime

data class CreateWebPage(
    val name: String,
    val url: String,
    val enabled: Boolean
)


data class UpdateWebPage(
    val name: String,
    val url: String,
    val enabled: Boolean
)

data class WebPage(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean,
    val created: ZonedDateTime
) {
    companion object {
        fun createFromEntity(entity: Entity): WebPage {
            val id = entity.id.toString()
            val name = entity.getPropertyAsString("name")
            val url = entity.getPropertyAsString("url")
            val enabled = entity.getPropertyAsBoolean("enabled")
            val created = entity.getPropertyAsZonedDateTime("created")
            return WebPage(id, name, url, enabled, created)
        }
    }
}

data class WebPageDetail(
    val name: String,
    val url: String,
    val enabled: Boolean,
    val created: ZonedDateTime
) {
    companion object {
        fun createFromEntity(entity: Entity): WebPageDetail {
            val name = entity.getPropertyAsString("name")
            val url = entity.getPropertyAsString("url")
            val enabled = entity.getPropertyAsBoolean("enabled")
            val created = entity.getPropertyAsZonedDateTime("created")
            return WebPageDetail(name, url, enabled, created)
        }
    }
}


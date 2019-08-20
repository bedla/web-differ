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
)

fun Entity.createWebPage(): WebPage {
    val id = id.toString()
    val name = getPropertyAsString("name")
    val url = getPropertyAsString("url")
    val enabled = getPropertyAsBoolean("enabled")
    val created = getPropertyAsZonedDateTime("created")
    return WebPage(id, name, url, enabled, created)
}


data class WebPageDetail(
    val name: String,
    val url: String,
    val enabled: Boolean,
    val created: ZonedDateTime
)

fun Entity.createWebPageDetail(): WebPageDetail {
    val name = getPropertyAsString("name")
    val url = getPropertyAsString("url")
    val enabled = getPropertyAsBoolean("enabled")
    val created = getPropertyAsZonedDateTime("created")
    return WebPageDetail(name, url, enabled, created)
}

data class User(
    val subject: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val pictureUrl: String
)

fun Entity.createUser(): User {
    val subject = getPropertyAsString("subject")
    val firstName = getPropertyAsString("firstName")
    val lastName = getPropertyAsString("lastName")
    val email = getPropertyAsString("email")
    val pictureUrl = getPropertyAsString("pictureUrl")
    return User(subject, firstName, lastName, email, pictureUrl)
}


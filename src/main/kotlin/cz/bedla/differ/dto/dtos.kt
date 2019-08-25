package cz.bedla.differ.dto

import cz.bedla.differ.utils.getPropertyAs
import cz.bedla.differ.utils.getPropertyAsZonedDateTime
import jetbrains.exodus.entitystore.Entity
import java.time.ZonedDateTime

data class CreateWebPage(
    val name: String,
    val url: String,
    val selector: String,
    val enabled: Boolean
)

data class UpdateWebPage(
    val name: String,
    val url: String,
    val selector: String,
    val enabled: Boolean
)

data class WebPageSimple(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean,
    val created: ZonedDateTime
)

fun Entity.createWebPage(): WebPageSimple {
    val id = id.toString()
    val name: String = getPropertyAs("name")
    val url: String = getPropertyAs("url")
    val enabled: Boolean = getPropertyAs("enabled")
    val created = getPropertyAsZonedDateTime("created")
    return WebPageSimple(id, name, url, enabled, created)
}


data class WebPageDetail(
    val name: String,
    val url: String,
    val selector: String,
    val enabled: Boolean,
    val created: ZonedDateTime
)

fun Entity.createWebPageDetail(): WebPageDetail {
    val name: String = getPropertyAs("name")
    val url: String = getPropertyAs("url")
    val selector: String = getPropertyAs("selector")
    val enabled: Boolean = getPropertyAs("enabled")
    val created = getPropertyAsZonedDateTime("created")
    return WebPageDetail(name, url, selector, enabled, created)
}

data class User(
    val subject: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val pictureUrl: String,
    val active: Boolean
)

fun Entity.createUser(): User {
    val subject: String = getPropertyAs("subject")
    val firstName: String = getPropertyAs("firstName")
    val lastName: String = getPropertyAs("lastName")
    val email: String = getPropertyAs("email")
    val pictureUrl: String = getPropertyAs("pictureUrl")
    val active: Boolean = getPropertyAs("active")
    return User(subject, firstName, lastName, email, pictureUrl, active)
}


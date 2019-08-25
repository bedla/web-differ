package cz.bedla.differ.dto

import cz.bedla.differ.utils.findPropertyAs
import cz.bedla.differ.utils.findPropertyAsZonedDateTime
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
    val created: ZonedDateTime,
    val lastRun: ZonedDateTime?
)

fun Entity.createWebPage(): WebPageSimple {
    val id = id.toString()
    val name: String = getPropertyAs("name")
    val url: String = getPropertyAs("url")
    val enabled: Boolean = getPropertyAs("enabled")
    val created = getPropertyAsZonedDateTime("created")
    val lastRun = findPropertyAsZonedDateTime("lastRun")
    return WebPageSimple(id, name, url, enabled, created, lastRun)
}


data class WebPageDetail(
    val name: String,
    val url: String,
    val selector: String,
    val enabled: Boolean,
    val created: ZonedDateTime,
    val lastRun: ZonedDateTime?,
    val diffs: List<Diff>
)

fun Entity.createWebPageDetail(diffs: List<Diff>): WebPageDetail {
    val name: String = getPropertyAs("name")
    val url: String = getPropertyAs("url")
    val selector: String = getPropertyAs("selector")
    val enabled: Boolean = getPropertyAs("enabled")
    val created = getPropertyAsZonedDateTime("created")
    val lastRun = findPropertyAsZonedDateTime("lastRun")
    return WebPageDetail(name, url, selector, enabled, created, lastRun, diffs)
}

data class Diff(
    val type: Type,
    val content: String?,
    val invalidSelector: String?,
    val exceptionName: String?,
    val created: ZonedDateTime
) {
    enum class Type { ERROR_SELECTOR, ERROR_EXCEPTION, CONTENT, NAN }
}

fun Entity.createDiff(): Diff {
    val invalidSelector: String? = findPropertyAs("invalidSelector")
    val exceptionName: String? = findPropertyAs("exceptionName")
    val content: String? = findPropertyAs("content")
    val created = getPropertyAsZonedDateTime("created")

    val type = when {
        invalidSelector != null -> Diff.Type.ERROR_SELECTOR
        exceptionName != null -> Diff.Type.ERROR_EXCEPTION
        content != null -> Diff.Type.CONTENT
        else -> Diff.Type.NAN
    }
    return Diff(type, content, invalidSelector, exceptionName, created)
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

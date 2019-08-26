package cz.bedla.differ.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
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

fun Entity.createWebPageSimple(): WebPageSimple {
    val id = id.toString()
    val name: String = getPropertyAs("name")
    val url: String = getPropertyAs("url")
    val enabled: Boolean = getPropertyAs("enabled")
    val created = getPropertyAsZonedDateTime("created")
    val lastRun = findPropertyAsZonedDateTime("lastRun")
    return WebPageSimple(id, name, url, enabled, created, lastRun)
}


data class WebPageDetail(
    val id: String,
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
    return WebPageDetail(id.toString(), name, url, selector, enabled, created, lastRun, diffs)
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "@type")
@JsonSubTypes(value = [
    JsonSubTypes.Type(value = DiffContent::class, name = "CONTENT"),
    JsonSubTypes.Type(value = DiffInvalidSelector::class, name = "INVALID_SELECTOR"),
    JsonSubTypes.Type(value = DiffError::class, name = "ERROR"),
    JsonSubTypes.Type(value = DiffStopExecution::class, name = "STOP")
])
interface Diff {
    val created: ZonedDateTime
}

data class DiffContent(
    override val created: ZonedDateTime,
    val content: String
) : Diff

data class DiffInvalidSelector(
    override val created: ZonedDateTime,
    val selector: String
) : Diff

data class DiffError(
    override val created: ZonedDateTime,
    val exceptionUuid: String,
    val exceptionName: String
) : Diff

data class DiffStopExecution(
    override val created: ZonedDateTime,
    val countErrors: Int
) : Diff

fun Entity.createDiff(): Diff {
    val invalidSelector: String? = findPropertyAs("invalidSelector")
    val exceptionName: String? = findPropertyAs("exceptionName")
    val exceptionUuid: String? = findPropertyAs("exceptionUuid")
    val content: String? = findPropertyAs("content")
    val countErrors: Int? = findPropertyAs("countErrors")
    val created = getPropertyAsZonedDateTime("created")

    return when {
        invalidSelector != null -> DiffInvalidSelector(created, invalidSelector)
        exceptionName != null -> DiffError(
            created,
            exceptionUuid ?: error("Exception UUID no found for entity $this"),
            exceptionName)
        content != null -> DiffContent(created, content)
        countErrors != null -> DiffStopExecution(created, countErrors)
        else -> error("Unrecognized diff for entity $this")
    }
}

data class User(
    val subject: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val pictureUrl: String,
    val active: Boolean,
    val oauth: OAuth
) {
    data class OAuth(val accessToken: String, val refreshToken: String?)
}

fun Entity.createUser(): User {
    val subject: String = getPropertyAs("subject")
    val firstName: String = getPropertyAs("firstName")
    val lastName: String = getPropertyAs("lastName")
    val email: String = getPropertyAs("email")
    val pictureUrl: String = getPropertyAs("pictureUrl")
    val active: Boolean = getPropertyAs("active")
    val accessToken: String = getPropertyAs("oauth-accessToken")
    val refreshToken: String? = findPropertyAs("oauth-refreshToken")
    return User(subject, firstName, lastName, email, pictureUrl, active, User.OAuth(accessToken, refreshToken))
}

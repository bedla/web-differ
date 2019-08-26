package cz.bedla.differ.utils

import cz.bedla.differ.dto.Diff
import cz.bedla.differ.dto.createDiff
import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.EntityRemovedInDatabaseException
import jetbrains.exodus.entitystore.StoreTransaction
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.util.matcher.NegatedRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

inline fun <reified T> Entity.findPropertyAs(key: String): T? {
    return getProperty(key) as? T
}

inline fun <reified T> Entity.getPropertyAs(key: String): T {
    return findPropertyAs(key) ?: error("Unable to find property '$key' in entity $this")
}

fun Entity.getPropertyAsZonedDateTime(key: String): ZonedDateTime =
    findPropertyAsZonedDateTime(key)
        ?: error("Unable to find ZonedDateTime property '$key' in entity $this")

fun Entity.findPropertyAsZonedDateTime(key: String): ZonedDateTime? {
    val value: Long = findPropertyAs(key) ?: return null
    val instant = Instant.ofEpochMilli(value);
    // TODO make time-zone configurable
    return ZonedDateTime.ofInstant(instant, ZoneId.of("Europe/Prague"));
}

fun Entity.setPropertyZonedDateTime(key: String, value: ZonedDateTime): Boolean =
    setProperty(key, value.toInstant().toEpochMilli())

fun OAuth2User.getAttribute(key: String): String {
    return attributes[key] as? String ?: error("no '$key' found at attributes $this")
}

fun OAuth2User.findAttribute(key: String): String? {
    return attributes[key] as? String
}

fun StoreTransaction.findEntity(id: String): Entity? =
    try {
        getEntity(toEntityId(id))
    } catch (e: EntityRemovedInDatabaseException) {
        null
    }

fun StoreTransaction.getDiffs(webPageEntity: Entity): List<Diff> {
    val diffs = findLinks("Diff", webPageEntity, "webPage")
    return sort("Diff", "created", diffs, false)
        .map { it.createDiff() }
}

fun ZonedDateTime.prettyToString(): String =
    DateTimeFormatter
        .ofPattern("yyyy-MM-dd hh:mm:ss")
        .format(toLocalDateTime())

fun notActuatorMatcher(): RequestMatcher = NegatedRequestMatcher(actuatorMatcher())

fun actuatorMatcher(): RequestMatcher = EndpointRequest.toAnyEndpoint()

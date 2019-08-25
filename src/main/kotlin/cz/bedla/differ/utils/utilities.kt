package cz.bedla.differ.utils

import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.EntityRemovedInDatabaseException
import jetbrains.exodus.entitystore.StoreTransaction
import org.springframework.security.oauth2.core.user.OAuth2User
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

inline fun <reified T> Entity.findPropertyAs(key: String): T? {
    return getProperty(key) as? T
}

inline fun <reified T> Entity.getPropertyAs(key: String): T {
    return findPropertyAs(key) ?: error("Unable to find property '$key' in entity $this")
}

fun Entity.getPropertyAsZonedDateTime(key: String): ZonedDateTime {
    val value: Long = getPropertyAs(key)
    val instant = Instant.ofEpochSecond(value);
    // TODO make time-zone configurable
    return ZonedDateTime.ofInstant(instant, ZoneId.of("Europe/Prague"));
}

fun Entity.setPropertyZonedDateTime(key: String, value: ZonedDateTime): Boolean = setProperty(key, value.toEpochSecond())

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


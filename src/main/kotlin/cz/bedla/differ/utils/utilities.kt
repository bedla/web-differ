package cz.bedla.differ.utils

import jetbrains.exodus.entitystore.Entity
import org.springframework.security.oauth2.core.user.OAuth2User
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

fun Entity.getPropertyAsString(key: String): String {
    return getProperty(key) as? String ?: error("Unable to find property '$key' in entity $this")
}

fun Entity.getPropertyAsLong(key: String): Long {
    return getProperty(key) as? Long ?: error("Unable to find property '$key' in entity $this")
}

fun Entity.getPropertyAsBoolean(key: String): Boolean {
    return getProperty(key) as? Boolean ?: error("Unable to find property '$key' in entity $this")
}

fun Entity.getPropertyAsZonedDateTime(key: String): ZonedDateTime {
    val value = getPropertyAsLong(key)
    val instant = Instant.ofEpochSecond(value);
    // TODO make time-zone configurable
    return ZonedDateTime.ofInstant(instant, ZoneId.of("Europe/Prague"));
}

fun Entity.setPropertyZonedDateTime(key: String, value: ZonedDateTime): Boolean = setProperty(key, value.toEpochSecond())

fun OAuth2User.getAttribute(key: String): String {
    return attributes[key] as? String ?: error("no '$key' found at attributes $this")
}

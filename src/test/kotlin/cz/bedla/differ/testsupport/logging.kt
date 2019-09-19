package cz.bedla.differ.testsupport

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

fun <T : Any> KClass<T>.logDumper(): ListAppender<ILoggingEvent> {
    val logger = LoggerFactory.getLogger(this.java) as Logger
    val listAppender = ListAppender<ILoggingEvent>()
    listAppender.start()
    logger.addAppender(listAppender)
    return listAppender
}

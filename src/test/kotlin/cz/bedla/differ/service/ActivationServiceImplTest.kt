package cz.bedla.differ.service

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory


class ActivationServiceImplTest {
    private lateinit var fixture: ActivationServiceImpl

    @BeforeEach
    internal fun setUp() {
        fixture = ActivationServiceImpl()
    }

    @Test
    fun activationCodePrintedToLog() {
        val logger = LoggerFactory.getLogger(ActivationServiceImpl::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        logger.addAppender(listAppender)

        fixture.afterPropertiesSet()

        assertThat(listAppender.list)
            .hasOnlyOneElementSatisfying {
                assertThat(it.level).isEqualTo(Level.INFO)
                assertThat(it.message).contains("activation code = '${fixture.activationCode}'")
            }
    }
}

package cz.bedla.differ.service

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import cz.bedla.differ.testsupport.logDumper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ActivationServiceImplTest {
    private lateinit var fixture: ActivationServiceImpl
    private lateinit var logDump: ListAppender<ILoggingEvent>

    @BeforeEach
    internal fun setUp() {
        fixture = ActivationServiceImpl()
        logDump = ActivationServiceImpl::class.logDumper()
    }

    @Test
    fun activationCodePrintedToLog() {
        fixture.afterPropertiesSet()

        assertThat(logDump.list)
            .hasOnlyOneElementSatisfying {
                assertThat(it.level).isEqualTo(Level.INFO)
                assertThat(it.message).contains("activation code = '${fixture.activationCode}'")
            }
    }
}

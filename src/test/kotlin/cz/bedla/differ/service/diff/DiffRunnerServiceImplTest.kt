package cz.bedla.differ.service.diff

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpHeaders
import cz.bedla.differ.dto.*
import cz.bedla.differ.service.*
import cz.bedla.differ.service.email.EmailSender
import cz.bedla.differ.testsupport.MyClock
import cz.bedla.differ.testsupport.MyClock.Companion.invokeWith
import cz.bedla.differ.testsupport.logDumper
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import jetbrains.exodus.entitystore.PersistentEntityStore
import jetbrains.exodus.entitystore.PersistentEntityStores
import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Files
import java.time.Clock
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

@SpringBootTest
class DiffRunnerServiceImplTest {
    @Autowired
    private lateinit var fixture: DiffRunnerService

    @Autowired
    private lateinit var webPageService: WebPageService

    @Autowired
    private lateinit var emailSender: EmailSender

    @Autowired
    private lateinit var htmlPageService: HtmlPageService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var clock: Clock

    private lateinit var logDump: ListAppender<ILoggingEvent>

    @BeforeEach
    fun setUp() {
        clearMocks(emailSender, htmlPageService)
        logDump = DiffRunnerServiceImpl::class.logDumper()
    }

    @Test
    fun `first run`() {
        val baseNumber = "01"
        val userId = "s$baseNumber"
        val webPageName = "page$baseNumber"

        createUser(userId)
        val webPageId = webPageService.create(userId, CreateWebPage(webPageName, "http://page1.com", ".selector", true))

        every { htmlPageService.contentOfSelector(any(), any()) } returns "content"

        invokeWith(clock, { it.plusDays(1) }) {
            fixture.run(webPageId)
        }

        val result = webPageService.find(webPageId, userId)

        assertThat(result)
            .isNotNull
            .satisfies(Consumer<WebPageDetail?>{
                it ?: error("non-null satisfy")
                assertThat(it.id).isEqualTo(webPageId)
                assertThat(it.name).isEqualTo(webPageName)
                assertThat(it.url).isEqualTo("http://page1.com")
                assertThat(it.selector).isEqualTo(".selector")
                assertThat(it.enabled).isTrue()
                assertThat(it.created).isEqualTo(ZonedDateTime.now(clock))
                assertThat(it.lastRun).isEqualTo(ZonedDateTime.now(clock).plusDays(1))
                assertThat(it.diffs)
                    .containsExactly(DiffContent(ZonedDateTime.now(clock).plusDays(1), "content"))
            })

        assertThat(logDump.list)
            .hasOnlyOneElementSatisfying {
                assertThat(it.message)
                    .contains("First run for web-page.id=")
                    .contains("URL 'http://page1.com' and content 'content'")
            }
    }

    @Test
    fun `no content found by selector`() {
        val baseNumber = "02"
        val userId = "s$baseNumber"
        val webPageName = "page$baseNumber"

        createUser(userId)
        val webPageId = webPageService.create(userId, CreateWebPage(webPageName, "http://page1.com", ".selector", true))

        every { htmlPageService.contentOfSelector(any(), any()) } returns null

        fixture.run(webPageId)

        val result = webPageService.find(webPageId, userId)
        assertThat(result)
            .isNotNull
            .satisfies(Consumer<WebPageDetail?>{
                it ?: error("non-null satisfy")
                assertThat(it.diffs)
                    .containsExactly(DiffInvalidSelector(ZonedDateTime.now(clock), ".selector"))
            })

        assertThat(logDump.list)
            .hasOnlyOneElementSatisfying {
                assertThat(it.message)
                    .contains("Unable to find web-page.id=")
                    .contains("selector '.selector' at web-page URL 'http://page1.com'")
            }
    }

    @Test
    fun `content too big`() {
        val baseNumber = "03"
        val userId = "s$baseNumber"
        val webPageName = "page$baseNumber"

        createUser(userId)
        val webPageId = webPageService.create(userId, CreateWebPage(webPageName, "http://page1.com", ".selector", true))

        every { htmlPageService.contentOfSelector(any(), any()) } returns StringUtils.repeat("x", 1001)

        fixture.run(webPageId)

        val result = webPageService.find(webPageId, userId)
        assertThat(result)
            .isNotNull
            .satisfies(Consumer<WebPageDetail?>{
                it ?: error("non-null satisfy")
                assertThat(it.diffs)
                    .containsExactly(DiffInvalidSelector(ZonedDateTime.now(clock), ".selector"))
            })

        assertThat(logDump.list)
            .hasOnlyOneElementSatisfying {
                assertThat(it.message)
                    .contains("Unable to find web-page.id=")
                    .contains("selector '.selector' at web-page URL 'http://page1.com'")
            }
    }

    @Test
    fun `content equals`() {
        val baseNumber = "04"
        val userId = "s$baseNumber"
        val webPageName = "page$baseNumber"

        createUser(userId)
        val webPageId = webPageService.create(userId, CreateWebPage(webPageName, "http://page1.com", ".selector", true))

        every { htmlPageService.contentOfSelector(any(), any()) } returnsMany listOf("xxx", "xxx")

        fixture.run(webPageId)
        logDump.list.clear()
        fixture.run(webPageId)

        val result = webPageService.find(webPageId, userId)
        assertThat(result)
            .isNotNull
            .satisfies(Consumer<WebPageDetail?>{
                it ?: error("non-null satisfy")
                assertThat(it.diffs)
                    .containsExactly(DiffContent(ZonedDateTime.now(clock), "xxx"))
            })

        assertThat(logDump.list)
            .hasOnlyOneElementSatisfying {
                assertThat(it.message)
                    .contains("There is no diff on web-page.id=")
                    .contains("URL 'http://page1.com' for selector '.selector' value 'xxx'")
            }
    }

    @Test
    fun `content is different`() {
        val baseNumber = "05"
        val userId = "s$baseNumber"
        val webPageName = "page$baseNumber"

        createUser(userId)
        val webPageId = webPageService.create(userId, CreateWebPage(webPageName, "http://page1.com", ".selector", true))

        every { htmlPageService.contentOfSelector(any(), any()) } returnsMany listOf("xxx", "yyy")
        every { emailSender.sendEmail(any(), any()) } returns CompletableFuture.completedFuture(true)

        fixture.run(webPageId)
        logDump.list.clear()
        fixture.run(webPageId)

        val result = webPageService.find(webPageId, userId)
        assertThat(result)
            .isNotNull
            .satisfies(Consumer<WebPageDetail?>{
                it ?: error("non-null satisfy")
                assertThat(it.diffs)
                    .containsExactly(
                        DiffContent(ZonedDateTime.now(clock), "yyy"),
                        DiffContent(ZonedDateTime.now(clock), "xxx")
                    )
            })

        assertThat(logDump.list)
            .hasOnlyOneElementSatisfying {
                assertThat(it.message)
                    .contains("Diff found for web-page.id=")
                    .contains("URL 'http://page1.com' for selector '.selector': 'xxx' -> 'yyy'")
            }
    }

    @Test
    fun `exception when downloading web-page`() {
        val baseNumber = "06"
        val userId = "s$baseNumber"
        val webPageName = "page$baseNumber"

        createUser(userId)
        val webPageId = webPageService.create(userId, CreateWebPage(webPageName, "http://page1.com", ".selector", true))

        every { htmlPageService.contentOfSelector(any(), any()) } throws IllegalStateException("foo bar")

        fixture.run(webPageId)

        val result = webPageService.find(webPageId, userId)
        assertThat(result)
            .isNotNull
            .satisfies(Consumer<WebPageDetail?>{
                it ?: error("non-null satisfy")
                assertThat(it.diffs)
                    .containsExactly(
                        DiffError(ZonedDateTime.now(clock), "12345678-aaaa-bbbb-cccc-210987654321", IllegalStateException::class.java.name, "foo bar")
                    )
            })

        assertThat(logDump.list)
            .hasOnlyOneElementSatisfying {
                assertThat(it.message)
                    .contains("There was error with diff run uuid='12345678-aaaa-bbbb-cccc-210987654321' and URL='http://page1.com'")
            }
    }

    @Test
    fun `exception when sending notification email`() {
        val baseNumber = "07"
        val userId = "s$baseNumber"
        val webPageName = "page$baseNumber"

        createUser(userId)
        val webPageId = webPageService.create(userId, CreateWebPage(webPageName, "http://page1.com", ".selector", true))

        every { htmlPageService.contentOfSelector(any(), any()) } returnsMany listOf("xxx", "yyy")
        every { emailSender.sendEmail(any(), any()) } returns CompletableFuture.supplyAsync<Boolean> { throw IllegalStateException("email error") }

        fixture.run(webPageId)
        logDump.list.clear()
        fixture.run(webPageId)

        val result = webPageService.find(webPageId, userId)
        assertThat(result)
            .isNotNull
            .satisfies(Consumer<WebPageDetail?>{
                it ?: error("non-null satisfy")
                assertThat(it.diffs)
                    .containsExactly(
                        DiffError(ZonedDateTime.now(clock), "12345678-aaaa-bbbb-cccc-210987654321", IllegalStateException::class.java.name, "email error"),
                        DiffContent(ZonedDateTime.now(clock), "yyy"),
                        DiffContent(ZonedDateTime.now(clock), "xxx")
                    )
            })

        assertThat(logDump.list[0].message)
            .contains("Diff found for web-page.id=")
            .contains("URL 'http://page1.com' for selector '.selector': 'xxx' -> 'yyy'")
        assertThat(logDump.list[1].message)
            .contains("There was error with diff run uuid='12345678-aaaa-bbbb-cccc-210987654321' and URL='http://page1.com'")
    }

    @Test
    fun `more errors disables web-page`() {
        val baseNumber = "08"
        val userId = "s$baseNumber"
        val webPageName = "page$baseNumber"

        createUser(userId)
        val webPageId = webPageService.create(userId, CreateWebPage(webPageName, "http://page1.com", ".selector", true))

        every { htmlPageService.contentOfSelector(any(), any()) } throws IllegalStateException("foo bar")

        repeat(5) {
            fixture.run(webPageId)
        }

        val result = webPageService.find(webPageId, userId)
        assertThat(result)
            .isNotNull
            .satisfies(Consumer<WebPageDetail?>{
                it ?: error("non-null satisfy")
                assertThat(it.enabled)
                    .isFalse()
                assertThat(it.diffs)
                    .containsExactly(
                        DiffStopExecutionCount(ZonedDateTime.now(clock), countErrors = 5),
                        DiffError(ZonedDateTime.now(clock), "12345678-aaaa-bbbb-cccc-210987654321", IllegalStateException::class.java.name, "foo bar"),
                        DiffError(ZonedDateTime.now(clock), "12345678-aaaa-bbbb-cccc-210987654321", IllegalStateException::class.java.name, "foo bar"),
                        DiffError(ZonedDateTime.now(clock), "12345678-aaaa-bbbb-cccc-210987654321", IllegalStateException::class.java.name, "foo bar"),
                        DiffError(ZonedDateTime.now(clock), "12345678-aaaa-bbbb-cccc-210987654321", IllegalStateException::class.java.name, "foo bar"),
                        DiffError(ZonedDateTime.now(clock), "12345678-aaaa-bbbb-cccc-210987654321", IllegalStateException::class.java.name, "foo bar")
                    )
            })
    }

    @Test
    fun `Google API error disables web-page`() {
        val baseNumber = "09"
        val userId = "s$baseNumber"
        val webPageName = "page$baseNumber"

        createUser(userId)
        val webPageId = webPageService.create(userId, CreateWebPage(webPageName, "http://page1.com", ".selector", true))

        every { htmlPageService.contentOfSelector(any(), any()) } throws GoogleJsonResponseException(
            com.google.api.client.http.HttpResponseException.Builder(500, "big error", HttpHeaders()), GoogleJsonError())

        fixture.run(webPageId)

        val result = webPageService.find(webPageId, userId)
        assertThat(result)
            .isNotNull
            .satisfies(Consumer<WebPageDetail?>{
                it ?: error("non-null satisfy")
                assertThat(it.enabled)
                    .isFalse()
                assertThat(it.diffs)
                    .containsExactly(
                        DiffError(ZonedDateTime.now(clock), "12345678-aaaa-bbbb-cccc-210987654321", GoogleJsonResponseException::class.java.name, ""),
                        DiffStopExecutionApiError(ZonedDateTime.now(clock))
                    )
            })
    }

    @Test
    fun webPageNotFound() {
        assertThatThrownBy {
            fixture.run("999-999")
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessage("WebPage(999-999) not found")
    }

    private fun createUser(subject: String) {
        userService.createOrUpdateUser(subject,
            "no-picture",
            "Foo",
            "Bar",
            "foo@bar.cz",
            "a123",
            "r123")
    }

    @Configuration
    class Config {
        @Bean
        fun diffRunnerService(): DiffRunnerService =
            DiffRunnerServiceImpl(
                persistentEntityStore(),
                emailSender(),
                htmlPageService(),
                clock()) {
                UUID.fromString("12345678-aaaa-bbbb-cccc-210987654321")
            }

        @Bean(destroyMethod = "close")
        fun persistentEntityStore(): PersistentEntityStore {
            val databaseDir = Files.createTempDirectory("web-differ-junit")
            return PersistentEntityStores.newInstance(databaseDir.toFile())
        }

        @Bean
        fun webPageService(): WebPageService = WebPageServiceImpl(persistentEntityStore(), mockk(), clock())

        @Bean
        fun userService(): UserService = UserServiceImpl(persistentEntityStore())

        @Bean
        fun emailSender(): EmailSender = mockk()

        @Bean
        fun htmlPageService(): HtmlPageService = mockk()

        @Bean
        fun clock(): Clock = MyClock()
    }
}

package cz.bedla.differ.service.email

import com.google.api.client.auth.oauth2.TokenResponse
import com.google.api.client.http.LowLevelHttpRequest
import com.google.api.client.http.LowLevelHttpResponse
import com.google.api.client.json.Json
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.testing.http.MockHttpTransport
import com.google.api.client.testing.http.MockLowLevelHttpRequest
import com.google.api.client.testing.http.MockLowLevelHttpResponse
import com.google.api.services.gmail.model.Message
import com.google.common.io.BaseEncoding
import cz.bedla.differ.configuration.ApplicationProperties
import cz.bedla.differ.dto.DiffContent
import cz.bedla.differ.dto.User
import cz.bedla.differ.dto.WebPageDetail
import cz.bedla.differ.security.AccessTokenRefresher
import cz.bedla.differ.service.UserService
import cz.bedla.differ.testsupport.MyClock
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.test.context.TestPropertySource
import java.time.Clock
import java.time.ZonedDateTime
import java.util.*


@SpringBootTest
@TestPropertySource(
    locations = [
        "classpath:oauth.properties"
    ],
    properties = [
        "differ.actuator.user=actuator-user",
        "differ.actuator.password=actuator-password",
        "differ.oAuthClient.id=client-id",
        "differ.oAuthClient.secret=client-secret"
    ])
class EmailSenderImplTest {
    @Autowired
    private lateinit var fixture: EmailSender

    @Autowired
    private lateinit var myMockHttpTransport: MyMockHttpTransport

    @Autowired
    private lateinit var jsonFactory: JsonFactory

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var clock: Clock

    @BeforeEach
    fun setUp() {
        clearMocks(userService)
    }

    @Test
    fun `compose and send email using GMail`() {
        every { userService.updateOAuthTokens(any(), any(), any()) } just Runs

        val emailRequest = object : MockLowLevelHttpRequest() {
            override fun execute(): LowLevelHttpResponse = jsonResponse(Message()
                .also {
                    it.factory = jsonFactory
                    it.id = "sent-id-999"
                }.toString())
        }

        myMockHttpTransport.requestBuilder = { method: String, url: String ->
            when {
                isOAuthTokenEndpoint(method, url) -> mockRefreshTokenResponse()
                isGmailSendEndpoint(method, url, "subject123") -> emailRequest
                else -> error("Unsupported $method $url")
            }
        }

        val result = fixture.sendEmail(User(
            subject = "subject123",
            firstName = "FirstName",
            lastName = "LastName",
            email = "nemam@email.cz",
            pictureUrl = "pictureUrl",
            active = true,
            oauth = User.OAuth("accessToken", "refreshToken")),
            WebPageDetail(
                id = "id-001",
                name = "monitor name",
                url = "http://some.url.cz/here",
                selector = ".css-selector",
                enabled = true,
                created = ZonedDateTime.now(clock).minusDays(3),
                lastRun = ZonedDateTime.now(clock),
                diffs = listOf(
                    DiffContent(ZonedDateTime.now(clock).minusDays(2), "new-value"),
                    DiffContent(ZonedDateTime.now(clock).minusDays(2), "old-value"))))
            .get()
        assertThat(result)
            .isTrue()

        assertThat(emailRequest.getEmail())
            .contains("From: nemam@email.cz")
            .contains("To: nemam@email.cz")
            .contains("Subject: WebDiffer - change for monitor name")
            .contains("Change detected on web-page: monitor name")
            .contains("URL: http://some.url.cz/here")
            .contains("Created: 2019-09-15 08:35:05")
            .contains("Last-run: 2019-09-18 08:35:05")
            .contains("Difference: old-value -> new-value")

        verify {
            userService.updateOAuthTokens("access-token-new", "access-token-new", "refresh-token-new")
        }
        confirmVerified(userService)
    }

    private fun MockLowLevelHttpRequest.getEmail(): String {
        val raw = jsonFactory.fromString(contentAsString, Map::class.java)["raw"].toString()
        return BaseEncoding.base64Url().decode(raw).toString(Charsets.UTF_8)
    }

    private fun isGmailSendEndpoint(method: String, url: String, subject: String) =
        method == "POST" && url == "https://www.googleapis.com/gmail/v1/users/$subject/messages/send"

    private fun isOAuthTokenEndpoint(method: String, url: String) =
        method == "POST" && url == "https://www.googleapis.com/oauth2/v4/token"

    private fun mockRefreshTokenResponse(): MockLowLevelHttpRequest {
        return object : MockLowLevelHttpRequest() {
            override fun execute(): LowLevelHttpResponse = jsonResponse(TokenResponse().also {
                it.factory = jsonFactory
                it.accessToken = "access-token-new"
                it.refreshToken = "refresh-token-new"
            }.toString())
        }
    }

    @Configuration
    @EnableConfigurationProperties(value = [
        ApplicationProperties::class,
        OAuth2ClientProperties::class
    ])
    class Config(
        private val applicationProperties: ApplicationProperties,
        private val oAuth2ClientProperties: OAuth2ClientProperties
    ) {
        @Bean
        fun emailSender(): EmailSender {
            return EmailSenderImpl(
                applicationProperties,
                clientRegistrationRepository(),
                accessTokenRefresher(),
                myMockHttpTransport(),
                jsonFactory()
            )
        }

        @Bean
        fun clientRegistrationRepository(): InMemoryClientRegistrationRepository {
            val registrations = ArrayList(
                OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(oAuth2ClientProperties).values)
            return InMemoryClientRegistrationRepository(registrations)
        }

        @Bean
        fun accessTokenRefresher(): AccessTokenRefresher {
            return AccessTokenRefresher(userService())
        }

        @Bean
        fun userService(): UserService {
            return mockk()
        }

        @Bean
        fun myMockHttpTransport(): MyMockHttpTransport = MyMockHttpTransport()

        @Bean
        fun jsonFactory(): JsonFactory = JacksonFactory.getDefaultInstance()!!

        @Bean
        fun clock(): Clock = MyClock()
    }

    companion object {
        fun jsonResponse(content: String): LowLevelHttpResponse {
            val response = MockLowLevelHttpResponse()
            response.statusCode = 200
            response.contentType = Json.MEDIA_TYPE
            response.setContent(content)
            return response
        }
    }

    class MyMockHttpTransport : MockHttpTransport() {
        lateinit var requestBuilder: (String, String) -> LowLevelHttpRequest

        override fun buildRequest(method: String, url: String): LowLevelHttpRequest {
            return requestBuilder(method, url);
        }
    }
}

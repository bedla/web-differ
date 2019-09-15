package cz.bedla.differ.service.email

import com.google.api.client.auth.oauth2.BearerToken
import com.google.api.client.auth.oauth2.ClientParametersAuthentication
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.util.Base64
import com.google.api.services.gmail.Gmail
import cz.bedla.differ.configuration.ApplicationProperties
import cz.bedla.differ.dto.DiffContent
import cz.bedla.differ.dto.User
import cz.bedla.differ.dto.WebPageDetail
import cz.bedla.differ.security.AccessTokenRefresher
import cz.bedla.differ.utils.prettyToString
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


class EmailSenderImpl(
    private val applicationProperties: ApplicationProperties,
    private val clientRegistrationRepository: ClientRegistrationRepository,
    private val accessTokenRefresher: AccessTokenRefresher,
    private val httpTransport: HttpTransport,
    private val jsonFactory: JsonFactory
) : EmailSender {
    @Async
    override fun sendEmail(user: User, webPage: WebPageDetail): CompletableFuture<Boolean> {
        log.info("Sending email for user=${user.email} and web-page=${webPage.name}")

        val service = Gmail.Builder(httpTransport, jsonFactory, createCredential(user))
            .setApplicationName("WebDiffer")
            .build()!!

        val email = createEmail(user.email,
            user.email,
            "WebDiffer - change for ${webPage.name}",
            createBody(webPage))
        val response = service.users()
            .messages()
            .send(user.subject, createMessageWithEmail(email))
            .execute()!!;

        log.info("Email ${response.id} sent")
        return CompletableFuture.completedFuture(true)
    }

    private fun createBody(webPage: WebPageDetail): String {
        val last = webPage.diffs
            .filterIsInstance(DiffContent::class.java)
            .take(2)
        val difference = if (last.size == 2) {
            "${last[1].content} -> ${last[0].content}"
        } else {
            error("There has to be at least 2 changes ($last) for WebPage(${webPage.id})")
        }

        return """
            Change detected on web-page: ${webPage.name}
            
            URL: ${webPage.url}
            Selector: ${webPage.selector}
            Created: ${webPage.created.prettyToString()}
            Last-run: ${webPage.lastRun?.prettyToString() ?: ""}
            
            Difference: $difference
        """.trimIndent()
    }

    private fun createCredential(user: User): Credential {
        val oauth = user.oauth
        val accessToken = oauth.accessToken
        val refreshToken = oauth.refreshToken
        return Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
            .setJsonFactory(jsonFactory)
            .setTransport(httpTransport)
            .setClientAuthentication(ClientParametersAuthentication(
                applicationProperties.oAuthClient.id,
                applicationProperties.oAuthClient.secret
            ))
            .setTokenServerEncodedUrl(clientRegistrationRepository.findByRegistrationId("google").providerDetails.tokenUri)
            .addRefreshListener(accessTokenRefresher)
            .build()
            .setAccessToken(accessToken)
            .setRefreshToken(refreshToken)
            .also { it.refreshToken() }
    }

    private fun createEmail(to: String,
                            from: String,
                            subject: String,
                            bodyText: String): MimeMessage {
        val props = Properties()
        val session = Session.getDefaultInstance(props, null)
        val email = MimeMessage(session)
        email.setFrom(InternetAddress(from))
        email.addRecipient(javax.mail.Message.RecipientType.TO, InternetAddress(to))
        email.subject = subject
        email.setText(bodyText)
        return email
    }

    private fun createMessageWithEmail(emailContent: MimeMessage): com.google.api.services.gmail.model.Message {
        val buffer = ByteArrayOutputStream()
        emailContent.writeTo(buffer)
        val bytes = buffer.toByteArray()
        val encodedEmail = Base64.encodeBase64URLSafeString(bytes)
        val message = com.google.api.services.gmail.model.Message()
        message.raw = encodedEmail
        return message
    }

    companion object {
        val log = LoggerFactory.getLogger(EmailSenderImpl::class.java)!!
    }
}

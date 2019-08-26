package cz.bedla.differ.service

import com.google.api.client.auth.oauth2.BearerToken
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.Base64
import com.google.api.services.gmail.Gmail
import cz.bedla.differ.dto.DiffContent
import cz.bedla.differ.dto.User
import cz.bedla.differ.dto.WebPageDetail
import cz.bedla.differ.utils.prettyToString
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


open class EmailSenderImpl : EmailSender {
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
        return Credential(BearerToken.authorizationHeaderAccessMethod())
            .setAccessToken(accessToken)
// TODO refresh-token needs setJsonFactory, setTransport, setClientAuthentication and setTokenServerUrl
//            .setRefreshToken(refreshToken)
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
        val jsonFactory = JacksonFactory.getDefaultInstance()!!
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()!!
    }
}

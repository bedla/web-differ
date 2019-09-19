package cz.bedla.differ.configuration

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import cz.bedla.differ.security.AccessTokenRefresher
import cz.bedla.differ.service.*
import cz.bedla.differ.service.diff.DiffRunnerService
import cz.bedla.differ.service.diff.DiffRunnerServiceImpl
import cz.bedla.differ.service.email.EmailSender
import cz.bedla.differ.service.email.EmailSenderImpl
import jetbrains.exodus.entitystore.PersistentEntityStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import java.time.Clock
import java.util.*
import javax.net.ssl.SSLSocketFactory

@Configuration
class ServiceConfig(
    private val persistentEntityStore: PersistentEntityStore,
    private val taskExecutor: TaskExecutor,
    private val applicationProperties: ApplicationProperties,
    private val clientRegistrationRepository: ClientRegistrationRepository,
    private val accessTokenRefresher: AccessTokenRefresher,
    private val clock: Clock
) {
    @Bean
    fun webPageService(): WebPageService =
        WebPageServiceImpl(persistentEntityStore, diffRunnerExecutor(), clock)

    @Bean
    fun activationService(): ActivationService =
        ActivationServiceImpl()

    @Bean
    fun diffRunnerExecutor(): DiffRunnerExecutor =
        DiffRunnerExecutorImpl(taskExecutor, persistentEntityStore, diffRunnerService(), clock)

    @Bean
    fun diffRunnerService(): DiffRunnerService =
        DiffRunnerServiceImpl(persistentEntityStore, emailSender(), htmlPageService(), clock, UUID::randomUUID)

    @Bean
    fun appInitialized(): AppInitialized =
        AppInitialized(diffRunnerExecutor())

    @Bean
    fun emailSender(): EmailSender =
        EmailSenderImpl(
            applicationProperties,
            clientRegistrationRepository,
            accessTokenRefresher,
            GoogleNetHttpTransport.newTrustedTransport()!!,
            JacksonFactory.getDefaultInstance()!!
        )

    @Bean
    fun htmlPageService(): HtmlPageService =
        HtmlPageServiceImpl(HtmlPageService.Config(
            5 * 1000,
            SSLSocketFactory.getDefault() as SSLSocketFactory
        ))
}

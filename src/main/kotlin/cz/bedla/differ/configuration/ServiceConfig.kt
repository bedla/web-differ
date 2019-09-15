package cz.bedla.differ.configuration

import cz.bedla.differ.security.AccessTokenRefresher
import cz.bedla.differ.service.*
import jetbrains.exodus.entitystore.PersistentEntityStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import javax.net.ssl.SSLSocketFactory

@Configuration
class ServiceConfig(
    private val persistentEntityStore: PersistentEntityStore,
    private val taskExecutor: TaskExecutor,
    private val applicationProperties: ApplicationProperties,
    private val clientRegistrationRepository: ClientRegistrationRepository,
    private val accessTokenRefresher: AccessTokenRefresher
) {
    @Bean
    fun webPageService(): WebPageService =
        WebPageServiceImpl(persistentEntityStore, diffRunnerExecutor())

    @Bean
    fun activationService(): ActivationService =
        ActivationServiceImpl()

    @Bean
    fun diffRunnerExecutor(): DiffRunnerExecutor =
        DiffRunnerExecutorImpl(taskExecutor, persistentEntityStore, diffRunnerService())

    @Bean
    fun diffRunnerService(): DiffRunnerService =
        DiffRunnerServiceImpl(persistentEntityStore, emailSender(), htmlPageService())

    @Bean
    fun appInitialized(): AppInitialized =
        AppInitialized(diffRunnerExecutor())

    @Bean
    fun emailSender(): EmailSender =
        EmailSenderImpl(applicationProperties, clientRegistrationRepository, accessTokenRefresher)

    @Bean
    fun htmlPageService(): HtmlPageService =
        HtmlPageServiceImpl(HtmlPageService.Config(
            5 * 1000,
            SSLSocketFactory.getDefault() as SSLSocketFactory
        ))
}

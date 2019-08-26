package cz.bedla.differ.configuration

import cz.bedla.differ.service.*
import jetbrains.exodus.entitystore.PersistentEntityStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor

@Configuration
class ServiceConfig(
    private val persistentEntityStore: PersistentEntityStore,
    private val taskExecutor: TaskExecutor
) {
    @Bean
    fun webPageService(): WebPageService = WebPageServiceImpl(persistentEntityStore, diffRunnerExecutor())

    @Bean
    fun activationService(): ActivationService = ActivationServiceImpl()

    @Bean
    fun diffRunnerExecutor(): DiffRunnerExecutor = DiffRunnerExecutorImpl(taskExecutor, persistentEntityStore, diffRunnerService())

    @Bean
    fun diffRunnerService(): DiffRunnerService = DiffRunnerServiceImpl(persistentEntityStore, emailSender())

    @Bean
    fun appInitialized(): AppInitialized = AppInitialized(diffRunnerExecutor())

    @Bean
    fun emailSender(): EmailSender = EmailSenderImpl()
}

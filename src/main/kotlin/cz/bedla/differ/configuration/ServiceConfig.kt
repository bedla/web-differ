package cz.bedla.differ.configuration

import cz.bedla.differ.service.ActivationService
import cz.bedla.differ.service.ActivationServiceImpl
import cz.bedla.differ.service.WebPageService
import cz.bedla.differ.service.WebPageServiceImpl
import jetbrains.exodus.entitystore.PersistentEntityStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ServiceConfig(
    private val persistentEntityStore: PersistentEntityStore
) {
    @Bean
    fun webPageService(): WebPageService = WebPageServiceImpl(persistentEntityStore)

    @Bean
    fun activationService(): ActivationService = ActivationServiceImpl()
}

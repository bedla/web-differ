package cz.bedla.differ.service

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener

class AppInitialized(
    private val diffRunnerExecutor: DiffRunnerExecutor
) : ApplicationContextAware {
    private lateinit var thisContext: ApplicationContext

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        thisContext = applicationContext
    }

    @EventListener(ContextRefreshedEvent::class)
    fun contextRefreshedEvent(event: ContextRefreshedEvent) {
        val applicationContext = event.applicationContext
        if (applicationContext == thisContext) {
            diffRunnerExecutor.start()
        }
    }
}

package cz.bedla.differ.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/webjars/**")
            .addResourceLocations("/webjars/")
        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
    }
}

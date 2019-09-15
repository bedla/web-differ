package cz.bedla.differ

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.PropertySource

@SpringBootApplication
@PropertySource(value = [
    "classpath:oauth.properties"
])
class WebDifferApplication

fun main(args: Array<String>) {
    runApplication<WebDifferApplication>(*args)
}

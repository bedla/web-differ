package cz.bedla.differ

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WebDifferApplication

fun main(args: Array<String>) {
    runApplication<WebDifferApplication>(*args)
}

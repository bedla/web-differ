package cz.bedla.differ.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("differ")
data class ApplicationProperties(
    val actuator: Actuator,
    val oAuthClient: OAuthClient
)

data class Actuator(
    val user: String,
    val password: String
)

data class OAuthClient(
    val id: String,
    val secret: String
)

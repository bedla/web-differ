package cz.bedla.differ.configuration

import cz.bedla.differ.security.CustomMethodSecurityExpressionHandler
import cz.bedla.differ.service.UserService
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration

@Configuration
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true, jsr250Enabled = true)
class MethodSecurityConfig(
    private val userService: UserService
) : GlobalMethodSecurityConfiguration() {
    override fun createExpressionHandler(): MethodSecurityExpressionHandler {
        return CustomMethodSecurityExpressionHandler(userService)
    }
}

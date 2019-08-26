package cz.bedla.differ.configuration

import cz.bedla.differ.utils.actuatorMatcher
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy


@Configuration
@EnableWebSecurity
@Order(1)
class ActuatorSecurityConfig(
    private val applicationProperties: ApplicationProperties
) : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http.requestMatcher(actuatorMatcher())
            .csrf().disable()
            .authorizeRequests()
            .antMatchers("/**/health").access(healthAccess())
            .antMatchers("/**").access(actuatorRoleAccess())
            .and()
            .httpBasic()
            .realmName("WebDiffer actuator")
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .httpBasic()
    }

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.inMemoryAuthentication()
            .withUser(applicationProperties.actuator.user)
            .password("{noop}${applicationProperties.actuator.password}")
            .roles("ACTUATOR")
    }

    companion object {
        private fun healthAccess(): String {
            return "anonymous or " + actuatorRoleAccess()
        }

        private fun actuatorRoleAccess(): String {
            return "hasRole('ACTUATOR')"
        }
    }
}

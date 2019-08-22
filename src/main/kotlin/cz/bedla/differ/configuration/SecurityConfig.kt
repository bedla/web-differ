package cz.bedla.differ.configuration

import cz.bedla.differ.security.AuthenticatedUserUpdater
import cz.bedla.differ.service.UserService
import cz.bedla.differ.service.UserServiceImpl
import jetbrains.exodus.entitystore.PersistentEntityStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.web.context.annotation.ApplicationScope
import org.thymeleaf.extras.springsecurity5.dialect.SpringSecurityDialect

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val persistentEntityStore: PersistentEntityStore
) : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http.authorizeRequests()
            .antMatchers("/login").permitAll()
            .anyRequest().authenticated()
        http.oauth2Login()
            .loginPage("/login")
        http.logout()
            .logoutRequestMatcher(OrRequestMatcher(
                AntPathRequestMatcher("/logout", "GET"),
                AntPathRequestMatcher("/logout", "POST"),
                AntPathRequestMatcher("/logout", "PUT"),
                AntPathRequestMatcher("/logout", "DELETE")
            ))
        http.csrf()
            .ignoringAntMatchers("/api/**")
    }

    override fun configure(web: WebSecurity) {
        web.ignoring()
            .antMatchers("/static/**", "/webjars/**")
    }

    @Bean
    fun springSecurityDialect(): SpringSecurityDialect = SpringSecurityDialect()

    @Bean
    fun authenticatedUserUpdater(): AuthenticatedUserUpdater =
        AuthenticatedUserUpdater(userService())

    @Bean
    @ApplicationScope
    fun userService(): UserService = UserServiceImpl(persistentEntityStore)
}

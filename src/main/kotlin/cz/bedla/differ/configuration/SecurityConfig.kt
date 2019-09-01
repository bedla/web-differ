package cz.bedla.differ.configuration

import cz.bedla.differ.security.AccessTokenRefresher
import cz.bedla.differ.security.AuthenticatedUserUpdater
import cz.bedla.differ.security.MyOAuth2AuthorizationRequestResolver
import cz.bedla.differ.service.UserService
import cz.bedla.differ.service.UserServiceImpl
import cz.bedla.differ.utils.notActuatorMatcher
import jetbrains.exodus.entitystore.PersistentEntityStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.web.context.annotation.ApplicationScope
import org.thymeleaf.extras.springsecurity5.dialect.SpringSecurityDialect

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val persistentEntityStore: PersistentEntityStore,
    private val clientRegistrationRepository: ClientRegistrationRepository
) : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http.requestMatcher(notActuatorMatcher())
            .authorizeRequests()
            .antMatchers("/").permitAll()
            .antMatchers("/login").permitAll()
            .anyRequest().authenticated()
            .and()
            .oauth2Login()
            .loginPage("/login")
            .authorizationEndpoint()
            .authorizationRequestResolver(authorizationRequestResolver())
            .and()
            .and()
            .csrf()
            .ignoringAntMatchers("/api/**")
            .and()
            .logout()
            .logoutRequestMatcher(OrRequestMatcher(
                AntPathRequestMatcher("/logout", "GET"),
                AntPathRequestMatcher("/logout", "POST"),
                AntPathRequestMatcher("/logout", "PUT"),
                AntPathRequestMatcher("/logout", "DELETE")
            ))
    }

    override fun configure(web: WebSecurity) {
        web.ignoring()
            .antMatchers("/static/**", "/webjars/**", "/privacy")
    }

    @Bean
    fun springSecurityDialect(): SpringSecurityDialect = SpringSecurityDialect()

    @Bean
    fun authenticatedUserUpdater(): AuthenticatedUserUpdater =
        AuthenticatedUserUpdater(userService())

    @Bean
    @ApplicationScope
    fun userService(): UserService = UserServiceImpl(persistentEntityStore)

    @Bean
    fun authorizationRequestResolver(): OAuth2AuthorizationRequestResolver {
        return MyOAuth2AuthorizationRequestResolver(clientRegistrationRepository)
    }

    @Bean
    fun accessTokenRefresher(): AccessTokenRefresher = AccessTokenRefresher(userService())
}

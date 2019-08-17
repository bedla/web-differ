package cz.bedla.differ

import jetbrains.exodus.entitystore.PersistentEntityStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.thymeleaf.extras.springsecurity5.dialect.SpringSecurityDialect

@Configuration
@EnableWebSecurity
class SecurityConfig : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http.authorizeRequests()
            .antMatchers("/login").permitAll()
            .anyRequest().authenticated()
            .and()
            .oauth2Login()
            .loginPage("/login")
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
            .antMatchers("/static/**", "/webjars/**")
    }

    @Bean
    fun springSecurityDialect(): SpringSecurityDialect = SpringSecurityDialect()

    @Bean
    fun authenticatedUserUpdater(persistentEntityStore: PersistentEntityStore): AuthenticatedUserUpdater =
        AuthenticatedUserUpdater(persistentEntityStore)
}

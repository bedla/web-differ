package cz.bedla.differ.security

import cz.bedla.differ.service.UserService
import cz.bedla.differ.utils.findAttribute
import cz.bedla.differ.utils.getAttribute
import org.springframework.context.ApplicationListener
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken

class AuthenticatedUserUpdater(
    private val userService: UserService
) : ApplicationListener<AuthenticationSuccessEvent> {
    override fun onApplicationEvent(event: AuthenticationSuccessEvent) {
        createOrUpdateUser(event.source as? OAuth2LoginAuthenticationToken ?: return)
    }

    private fun createOrUpdateUser(authentication: OAuth2LoginAuthenticationToken) {
        val principal = authentication.principal ?: return
        val subject: String = principal.findAttribute("sub") ?: error("Unable to find sub claim")
        val pictureUrl = principal.findAttribute("picture") ?: "/static/no-avatar.png"
        val firstName = principal.findAttribute("given_name") ?: error("Unable to find given_name claim")
        val lastName = principal.findAttribute("family_name") ?: error("Unable to find family_name claim")
        val email = principal.findAttribute("email") ?: error("Unable to find email claim")
        val accessToken = authentication.accessToken.tokenValue
            ?: error("Invalid accessToken ${authentication.accessToken}")
        val refreshToken = authentication.refreshToken?.tokenValue
            ?: error("Invalid refreshToken ${authentication.refreshToken}")

        userService.createOrUpdateUser(
            subject,
            pictureUrl,
            firstName,
            lastName,
            email,
            accessToken,
            refreshToken
        )
    }
}

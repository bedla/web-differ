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
        val subject = principal.getAttribute("sub")
        val pictureUrl = principal.findAttribute("picture") ?: "/static/no-avatar.png"
        val firstName = principal.getAttribute("given_name")
        val lastName = principal.getAttribute("family_name")
        val email = principal.getAttribute("email")

        userService.createOrUpdateUser(subject, pictureUrl, firstName, lastName, email)
    }
}

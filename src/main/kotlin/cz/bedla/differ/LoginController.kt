package cz.bedla.differ

import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter
import org.springframework.security.web.WebAttributes
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import java.security.Principal
import javax.servlet.http.HttpServletRequest

@Controller
class LoginController(private val clientRegistrationRepository: ClientRegistrationRepository) {
    @GetMapping("/login")
    fun getLoginPage(
            model: Model,
            principal: Principal?,
            request: HttpServletRequest
    ): String {
        return if (principal == null) {
            setupOAuthLoginUrl(model)
            setupLogout(model, request)
            setupError(model, request)
            "login"
        } else {
            "redirect:/"
        }
    }

    private fun setupOAuthLoginUrl(model: Model) {
        val registration = clientRegistrationRepository.findByRegistrationId("google")
                ?: error("Unable to find OAuth2 client")

        val baseUri = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
        model.addAttribute("oauthUrl", "$baseUri/${registration.registrationId}")
    }

    private fun setupLogout(model: Model, request: HttpServletRequest) {
        if (request.getParameter("logout") != null) {
            model.addAttribute("logout", true)
        }
    }

    private fun setupError(model: Model, request: HttpServletRequest) {
        if (request.getParameter("error") != null) {
            val lastException = request.getSession(false)
                    ?.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION) as? AuthenticationException
            model.addAttribute("errorMessage", lastException?.message ?: "Invalid credentials")
        }
    }
}

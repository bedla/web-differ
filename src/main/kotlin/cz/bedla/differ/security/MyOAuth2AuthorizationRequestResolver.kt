package cz.bedla.differ.security

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import javax.servlet.http.HttpServletRequest

class MyOAuth2AuthorizationRequestResolver(
    clientRegistrationRepository: ClientRegistrationRepository
) : OAuth2AuthorizationRequestResolver {
    private val defaultAuthorizationRequestResolver = DefaultOAuth2AuthorizationRequestResolver(
        clientRegistrationRepository,
        OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);

    override fun resolve(request: HttpServletRequest?): OAuth2AuthorizationRequest? {
        val authorizationRequest = this.defaultAuthorizationRequestResolver.resolve(request)
        return if (authorizationRequest != null) customAuthorizationRequest(authorizationRequest) else null
    }

    override fun resolve(request: HttpServletRequest?, clientRegistrationId: String?): OAuth2AuthorizationRequest? {
        val authorizationRequest = this.defaultAuthorizationRequestResolver.resolve(request, clientRegistrationId)
        return if (authorizationRequest != null) customAuthorizationRequest(authorizationRequest) else null
    }

    private fun customAuthorizationRequest(authorizationRequest: OAuth2AuthorizationRequest): OAuth2AuthorizationRequest {
        val additionalParameters = mutableMapOf<String, Any>()
            .also { it.putAll(authorizationRequest.additionalParameters) }
            .also { it["access_type"] = "offline" }
            .also { it["prompt"] = "select_account consent" }

        return OAuth2AuthorizationRequest.from(authorizationRequest)
            .additionalParameters(additionalParameters)
            .build()
    }
}


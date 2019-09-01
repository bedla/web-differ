package cz.bedla.differ.security

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.auth.oauth2.CredentialRefreshListener
import com.google.api.client.auth.oauth2.TokenErrorResponse
import com.google.api.client.auth.oauth2.TokenResponse
import cz.bedla.differ.service.UserService

class AccessTokenRefresher(
    private val userService: UserService
) : CredentialRefreshListener {
    override fun onTokenErrorResponse(credential: Credential, tokenErrorResponse: TokenErrorResponse) {
        error("Unable to refresh access token: $tokenErrorResponse")
    }

    override fun onTokenResponse(credential: Credential, tokenResponse: TokenResponse) {
        val oldAccessToken = credential.accessToken ?: error("No oldAccessToken found")
        val newAccessToken = tokenResponse.accessToken ?: error("No newAccessToken found")
        val newRefreshToken: String? = tokenResponse.refreshToken
        userService.updateOAuthTokens(oldAccessToken, newAccessToken, newRefreshToken)
    }
}

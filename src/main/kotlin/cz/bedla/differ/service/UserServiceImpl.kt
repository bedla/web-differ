package cz.bedla.differ.service

import cz.bedla.differ.utils.getAttribute
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.user.OAuth2User

class UserServiceImpl : UserService {
    override fun currentAuthenticatedUser(): String {
        return userFromAuthentication(SecurityContextHolder.getContext()?.authentication
            ?: error("Unable to find authentication in security-context ${SecurityContextHolder.getContext()}"))
    }

    override fun userFromAuthentication(authentication: Authentication): String {
        val user = (authentication.principal as? OAuth2User
            ?: error("Principal is not ${OAuth2User::class.simpleName} but ${authentication.principal}"))
        return user.getAttribute("sub")
    }
}

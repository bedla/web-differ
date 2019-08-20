package cz.bedla.differ.service

import org.springframework.security.core.Authentication

interface UserService {
    fun currentAuthenticatedUser(): String

    fun userFromAuthentication(authentication: Authentication): String
}

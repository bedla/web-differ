package cz.bedla.differ.service

import cz.bedla.differ.dto.User
import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.StoreTransaction
import org.springframework.security.core.Authentication

interface UserService {
    fun currentAuthenticatedUserId(): String

    fun currentAuthenticatedUser(): User

    fun userIdFromAuthentication(authentication: Authentication): String
}

fun StoreTransaction.getUserEntity(userId: String): Entity =
    findUserEntity(userId)
        ?: error("Unable to find user.id=$userId")

fun StoreTransaction.findUserEntity(userId: String): Entity? =
    find("User", "subject", userId).first

package cz.bedla.differ.service

import cz.bedla.differ.dto.User
import cz.bedla.differ.dto.createUser
import cz.bedla.differ.utils.findAttribute
import cz.bedla.differ.utils.getAttribute
import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.PersistentEntityStore
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.user.OAuth2User

class UserServiceImpl(
    private val persistentEntityStore: PersistentEntityStore
) : UserService {
    override fun currentAuthenticatedUser(): User = persistentEntityStore.computeInTransaction { tx ->
        val userId = currentAuthenticatedUserId()
        userFromDb(userId)
    }

    override fun userFromAuthentication(authentication: Authentication): User {
        return userFromDb(userIdFromAuthentication(authentication))
    }

    override fun currentAuthenticatedUserId(): String {
        return userIdFromAuthentication(SecurityContextHolder.getContext()?.authentication
            ?: error("Unable to find authentication in security-context ${SecurityContextHolder.getContext()}"))
    }

    override fun activateUser(userId: String) = persistentEntityStore.executeInExclusiveTransaction { tx ->
        val entity = tx.getUserEntity(userId)
        entity.setProperty("active", true)
    }

    override fun createOrUpdateUser(
        subject: String,
        pictureUrl: String,
        firstName: String,
        lastName: String,
        email: String,
        accessToken: String,
        refreshToken: String
    ) = persistentEntityStore.executeInTransaction { tx ->
        val entity = tx.findUserEntity(subject)
            .let {
                if (it == null) {
                    val newEntity = tx.newEntity("User")
                    newEntity.setProperty("subject", subject)
                    newEntity.setProperty("active", false)
                    newEntity
                } else {
                    it
                }
            }
        entity.setProperty("pictureUrl", pictureUrl)
        entity.setProperty("firstName", firstName)
        entity.setProperty("lastName", lastName)
        entity.setProperty("email", email)
        updateOAuthTokens(entity, accessToken, refreshToken)
    }

    override fun updateOAuthTokens(
        oldAccessToken: String,
        newAccessToken: String,
        newRefreshToken: String?
    ) = persistentEntityStore.executeInTransaction { tx ->
        val entity = tx.find("User", "oauth-accessToken", oldAccessToken)
            .first ?: return@executeInTransaction

        updateOAuthTokens(entity, newAccessToken, newRefreshToken)
    }

    private fun updateOAuthTokens(entity: Entity, accessToken: String, refreshToken: String?) {
        entity.setProperty("oauth-accessToken", accessToken)
        if (refreshToken != null) entity.setProperty("oauth-refreshToken", refreshToken)
    }

    override fun userFromDb(userId: String): User = persistentEntityStore.computeInTransaction { tx ->
        val entity = tx.getUserEntity(userId)
        entity.createUser()
    }

    private fun userIdFromAuthentication(authentication: Authentication): String {
        val user = (authentication.principal as? OAuth2User
            ?: error("Principal is not ${OAuth2User::class.simpleName} but ${authentication.principal}"))
        return user.findAttribute("sub") ?: error("Unable to find sub claim")
    }
}

package cz.bedla.differ.service

import cz.bedla.differ.dto.User
import cz.bedla.differ.dto.createUser
import cz.bedla.differ.utils.getAttribute
import jetbrains.exodus.entitystore.PersistentEntityStore
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.user.OAuth2User

class UserServiceImpl(
    private val persistentEntityStore: PersistentEntityStore
) : UserService {
    override fun currentAuthenticatedUser(): User = persistentEntityStore.computeInTransaction { tx ->
        val userId = currentAuthenticatedUserId()
        val entity = tx.getUserEntity(userId)
        entity.createUser()
    }

    override fun currentAuthenticatedUserId(): String {
        return userIdFromAuthentication(SecurityContextHolder.getContext()?.authentication
            ?: error("Unable to find authentication in security-context ${SecurityContextHolder.getContext()}"))
    }

    override fun userIdFromAuthentication(authentication: Authentication): String {
        val user = (authentication.principal as? OAuth2User
            ?: error("Principal is not ${OAuth2User::class.simpleName} but ${authentication.principal}"))
        return user.getAttribute("sub")
    }
}

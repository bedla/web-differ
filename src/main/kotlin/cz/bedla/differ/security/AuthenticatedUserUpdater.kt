package cz.bedla.differ.security

import cz.bedla.differ.utils.getAttribute
import jetbrains.exodus.entitystore.PersistentEntityStore
import org.springframework.context.ApplicationListener
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken

class AuthenticatedUserUpdater(
    private val persistentEntityStore: PersistentEntityStore
) : ApplicationListener<AuthenticationSuccessEvent> {
    override fun onApplicationEvent(event: AuthenticationSuccessEvent) {
        createOrUpdateUser(event.source as? OAuth2LoginAuthenticationToken ?: return)
    }

    private fun createOrUpdateUser(authentication: OAuth2LoginAuthenticationToken) {
        val principal = authentication.principal ?: return
        val subject = principal.getAttribute("sub")
        val pictureUrl = principal.getAttribute("picture")
        val firstName = principal.getAttribute("given_name")
        val lastName = principal.getAttribute("family_name")
        val email = principal.getAttribute("email")

        persistentEntityStore.executeInTransaction { tx ->
            val entity = tx.find("User", "subject", subject)
                .first
                .let {
                    if (it == null) {
                        val newEntity = tx.newEntity("User")
                        newEntity.setProperty("subject", subject)
                        newEntity
                    } else {
                        it
                    }
                }
            entity.setProperty("pictureUrl", pictureUrl)
            entity.setProperty("firstName", firstName)
            entity.setProperty("lastName", lastName)
            entity.setProperty("email", email)
        }
    }
}

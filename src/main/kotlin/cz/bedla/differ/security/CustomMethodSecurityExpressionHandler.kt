package cz.bedla.differ.security

import cz.bedla.differ.service.UserService
import org.aopalliance.intercept.MethodInvocation
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations
import org.springframework.security.core.Authentication

class CustomMethodSecurityExpressionHandler(
    private val userService: UserService
) : DefaultMethodSecurityExpressionHandler() {
    override fun createSecurityExpressionRoot(authentication: Authentication,
                                              invocation: MethodInvocation): MethodSecurityExpressionOperations {
        val root = CustomMethodSecurityExpressionRoot(userService, authentication)
        root.setThis(invocation.getThis())
        root.setPermissionEvaluator(permissionEvaluator)
        root.setTrustResolver(trustResolver)
        root.setRoleHierarchy(roleHierarchy)
        root.setDefaultRolePrefix(defaultRolePrefix)
        return root
    }
}

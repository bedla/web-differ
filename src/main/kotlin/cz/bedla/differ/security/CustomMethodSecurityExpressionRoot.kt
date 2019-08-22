package cz.bedla.differ.security

import cz.bedla.differ.service.UserService
import org.springframework.security.access.expression.SecurityExpressionRoot
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations
import org.springframework.security.core.Authentication

class CustomMethodSecurityExpressionRoot(
    private val userService: UserService,
    authentication: Authentication
) : SecurityExpressionRoot(authentication), MethodSecurityExpressionOperations {
    private var filterObject: Any? = null
    private var returnObject: Any? = null
    private var target: Any? = null

    override fun setFilterObject(filterObject: Any) {
        this.filterObject = filterObject
    }

    override fun getFilterObject(): Any? {
        return filterObject
    }

    override fun setReturnObject(returnObject: Any) {
        this.returnObject = returnObject
    }

    override fun getReturnObject(): Any? {
        return returnObject
    }

    internal fun setThis(target: Any) {
        this.target = target
    }

    override fun getThis(): Any? {
        return target
    }

    fun isActivated(): Boolean = userService.userFromAuthentication(authentication).active
}

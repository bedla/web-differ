package cz.bedla.differ.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import java.util.*

class ActivationServiceImpl : ActivationService, InitializingBean {
    private lateinit var activationCode: String

    override fun checkActivationCode(code: String): Boolean = activationCode == code

    override fun afterPropertiesSet() {
        activationCode = UUID.randomUUID().toString()
        log.info("\n\n****** activation code = '$activationCode' ******\n")
    }

    companion object {
        private val log = LoggerFactory.getLogger(ActivationServiceImpl::class.java)!!
    }
}

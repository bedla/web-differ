package cz.bedla.differ.service

interface ActivationService {
    fun checkActivationCode(code: String): Boolean
}

package cz.bedla.differ.service

import cz.bedla.differ.dto.User
import cz.bedla.differ.dto.WebPageDetail
import java.util.concurrent.CompletableFuture

interface EmailSender {
    fun sendEmail(user: User, webPage: WebPageDetail): CompletableFuture<Boolean>
}

package cz.bedla.differ.service

import cz.bedla.differ.dto.CreateWebPage
import cz.bedla.differ.dto.UpdateWebPage
import cz.bedla.differ.dto.WebPage
import cz.bedla.differ.dto.WebPageDetail

interface WebPageService {
    fun find(id: String, userId: String): WebPageDetail?
    fun findAll(userId: String): List<WebPage>
    fun create(userId: String, request: CreateWebPage): String
    fun update(userId: String, id: String, request: UpdateWebPage): Boolean
    fun delete(userId: String, id: String): Boolean
}

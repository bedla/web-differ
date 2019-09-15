package cz.bedla.differ.service

import javax.net.ssl.SSLSocketFactory

interface HtmlPageService {
    fun contentOfSelector(url: String, selector: String): String?

    data class Config(
        val timeoutMillis: Int,
        val sslSocketFactory: SSLSocketFactory
    )
}

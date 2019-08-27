package cz.bedla.differ.service

import org.jsoup.Jsoup
import org.springframework.beans.factory.InitializingBean
import javax.net.ssl.SSLSocketFactory

class HtmlPageServiceImpl : HtmlPageService, InitializingBean {
    private lateinit var sslSocketFactory: SSLSocketFactory

    override fun afterPropertiesSet() {
        sslSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
    }

    override fun contentOfSelector(url: String, selector: String): String? {
        val doc = Jsoup.connect(url)
            .sslSocketFactory(if (url.startsWith("https://")) sslSocketFactory else null)
            .userAgent(userAgent)
            .timeout(5 * 1000)
            .get()!!

        return doc.select(selector).first()?.text()
    }

    companion object {
        private const val userAgent = """Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36"""
    }
}

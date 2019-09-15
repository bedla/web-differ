package cz.bedla.differ.service

import org.jsoup.Jsoup

class HtmlPageServiceImpl(private val config: HtmlPageService.Config) : HtmlPageService {
    override fun contentOfSelector(url: String, selector: String): String? {
        val doc = Jsoup.connect(url)
            .sslSocketFactory(config.sslSocketFactory)
            .userAgent(userAgent)
            .timeout(config.timeoutMillis)
            .get()!!

        return doc.select(selector).first()?.text()
    }

    companion object {
        private const val userAgent = """Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36"""
    }
}

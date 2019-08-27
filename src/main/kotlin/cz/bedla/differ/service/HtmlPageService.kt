package cz.bedla.differ.service

interface HtmlPageService {
    fun contentOfSelector(url: String, selector: String): String?
}

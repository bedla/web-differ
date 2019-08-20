package cz.bedla.differ.mvc

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class WebPageController {
    @GetMapping("/web-page")
    fun index(model: Model): String {
        return "webPage"
    }
}

package cz.bedla.differ.mvc

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class DashboardController {
    @GetMapping("/dashboard")
    fun dashboard(model: Model): String {
        return "dashboard"
    }
}

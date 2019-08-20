package cz.bedla.differ.mvc

import cz.bedla.differ.service.UserService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class ProfileController(
    private val userService: UserService
) {
    @GetMapping("/profile")
    fun index(model: Model): String {
        val user = userService.currentAuthenticatedUser()
        model.addAttribute("pictureUrl", user.pictureUrl)
        model.addAttribute("firstName", user.firstName)
        model.addAttribute("lastName", user.lastName)
        model.addAttribute("additionalInfo", user.email)
        return "profile"
    }
}

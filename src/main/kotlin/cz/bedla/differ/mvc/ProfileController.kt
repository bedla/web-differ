package cz.bedla.differ.mvc

import cz.bedla.differ.service.ActivationService
import cz.bedla.differ.service.UserService
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/profile")
class ProfileController(
    private val userService: UserService,
    private val activationService: ActivationService
) {
    @GetMapping
    fun index(model: Model): String {
        val user = userService.currentAuthenticatedUser()
        model.addAttribute("pictureUrl", user.pictureUrl)
        model.addAttribute("firstName", user.firstName)
        model.addAttribute("lastName", user.lastName)
        model.addAttribute("additionalInfo", user.email)
        model.addAttribute("active", user.active)
        return "profile"
    }

    @PostMapping(consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun submitCode(model: Model,
                   @RequestBody formData: MultiValueMap<String, String>
    ): String {
        val result = if (activationService.checkActivationCode(formData.getFirst("activationCode") ?: "invalid")) {
            userService.activateUser(userService.currentAuthenticatedUserId())
            "activated"
        } else {
            "invalid-code"
        }
        return "redirect:/profile?result=$result"
    }
}

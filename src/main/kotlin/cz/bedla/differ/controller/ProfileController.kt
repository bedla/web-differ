package cz.bedla.differ.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class ProfileController {
    @GetMapping("/profile")
    fun index(model: Model,
              @AuthenticationPrincipal oauth2User: OAuth2User
    ): String {
        model.addAttribute("photoUrl", oauth2User.attributes["picture"] ?: "/static/no-avatar.png")
        model.addAttribute("firstName", oauth2User.attributes["given_name"])
        model.addAttribute("lastName", oauth2User.attributes["family_name"])
        model.addAttribute("additionalInfo", oauth2User.attributes["email"])
        return "profile"
    }
}

package cz.bedla.differ.rest

import cz.bedla.differ.dto.CreateWebPage
import cz.bedla.differ.dto.UpdateWebPage
import cz.bedla.differ.dto.WebPage
import cz.bedla.differ.dto.WebPageDetail
import cz.bedla.differ.service.UserService
import cz.bedla.differ.service.WebPageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

@RequestMapping("/api")
@RestController
class UserWebPageController(
    private val webPageService: WebPageService,
    private val userService: UserService
) {
    @GetMapping("/user/me/web-page/{id}")
    fun get(
        @PathVariable("id") id: String
    ): ResponseEntity<WebPageDetail?> {
        val result = webPageService.get(id, userService.currentAuthenticatedUser())
        return if (result == null)
            ResponseEntity.notFound().build()
        else
            ResponseEntity.ok(result)
    }

    @GetMapping("/user/me/web-page")
    fun list(): List<WebPage> {
        return webPageService.findAll(userService.currentAuthenticatedUser())
    }

    @PutMapping("/user/me/web-page")
    fun create(
        @RequestBody createRequest: CreateWebPage
    ): ResponseEntity<Any> {
        val id = webPageService.create(userService.currentAuthenticatedUser(), createRequest)
        return ResponseEntity.created(URI("/api/me/web-page/$id")).build()
    }

    @PostMapping("/user/me/web-page/{id}")
    fun update(
        @PathVariable("id") id: String,
        @RequestBody updateRequest: UpdateWebPage
    ) {
        webPageService.update(userService.currentAuthenticatedUser(), id, updateRequest)
    }
}

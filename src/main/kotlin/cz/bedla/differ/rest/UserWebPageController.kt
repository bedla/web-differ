package cz.bedla.differ.rest

import cz.bedla.differ.dto.*
import cz.bedla.differ.service.HtmlPageService
import cz.bedla.differ.service.UserService
import cz.bedla.differ.service.WebPageService
import org.apache.commons.lang3.StringUtils.left
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RequestMapping("/api")
@RestController
class UserWebPageController(
    private val webPageService: WebPageService,
    private val userService: UserService,
    private val htmlPageService: HtmlPageService
) {
    @PreAuthorize("isActivated()")
    @GetMapping("/user/me/web-page/{id}")
    fun get(
        @PathVariable("id") id: String
    ): ResponseEntity<WebPageDetail?> {
        val result = webPageService.find(id, userService.currentAuthenticatedUserId())
        return if (result == null)
            ResponseEntity.notFound().build()
        else
            ResponseEntity.ok(result)
    }

    @PreAuthorize("isActivated()")
    @GetMapping("/user/me/web-page")
    fun list(): List<WebPageSimple> {
        return webPageService.findAll(userService.currentAuthenticatedUserId())
    }

    @PreAuthorize("isActivated()")
    @PutMapping("/user/me/web-page")
    fun create(
        @RequestBody createRequest: CreateWebPage
    ): ResponseEntity<Any> {
        val id = webPageService.create(userService.currentAuthenticatedUserId(), createRequest)
        return ResponseEntity.created(URI("/api/me/web-page/$id")).build()
    }

    @PreAuthorize("isActivated()")
    @PostMapping("/user/me/web-page/{id}")
    fun update(
        @PathVariable("id") id: String,
        @RequestBody updateRequest: UpdateWebPage
    ): ResponseEntity<Any> {
        val updated = webPageService.update(userService.currentAuthenticatedUserId(), id, updateRequest)
        return if (updated) {
            ResponseEntity.noContent()
        } else {
            ResponseEntity.notFound()
        }.build()
    }

    @PreAuthorize("isActivated()")
    @DeleteMapping("/user/me/web-page/{id}")
    fun delete(
        @PathVariable("id") id: String
    ): ResponseEntity<Any> {
        val deleted = webPageService.delete(userService.currentAuthenticatedUserId(), id)
        return if (deleted) {
            ResponseEntity.noContent()
        } else {
            ResponseEntity.notFound()
        }.build()
    }

    @PreAuthorize("isActivated()")
    @PostMapping("/user/me/web-page/{id}/execute")
    fun execute(
        @PathVariable("id") id: String
    ): ResponseEntity<Any> {
        val executed = webPageService.execute(userService.currentAuthenticatedUserId(), id)
        return if (executed) {
            ResponseEntity.noContent()
        } else {
            ResponseEntity.notFound()
        }.build()
    }

    @PreAuthorize("isActivated()")
    @PostMapping("/user/me/web-page/test")
    fun test(
        @RequestBody testRequest: TestWebPage
    ): ResponseEntity<TestWebPageResponse> {
        return try {
            val content = htmlPageService.contentOfSelector(testRequest.url, testRequest.selector) ?: ""
            ResponseEntity.ok(TestWebPageResponseContent(left(content, 100), content.length))
        } catch (e: Exception) {
            val uuid = UUID.randomUUID()!!
            log.error("Error id=$uuid while testing $testRequest", e)
            ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(TestWebPageResponseError(uuid, e.javaClass.name, e.message ?: ""))
        }
    }

    companion object {
        val log = LoggerFactory.getLogger(UserWebPageController::class.java)!!
    }
}

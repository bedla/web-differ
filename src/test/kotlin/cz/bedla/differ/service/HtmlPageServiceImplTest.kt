package cz.bedla.differ.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.common.Slf4jNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.apache.http.ssl.SSLContexts
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jsoup.HttpStatusException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.SocketTimeoutException
import javax.net.ssl.SSLSocketFactory

class HtmlPageServiceImplTest {
    private lateinit var fixture: HtmlPageServiceImpl
    private lateinit var wireMockServer: WireMockServer;

    @BeforeEach
    fun setUp() {
        fixture = HtmlPageServiceImpl(HtmlPageService.Config(3 * 1000, acceptAllSslSocketFactory()))

        // TODO investigate why BeforeAll with fixedDelay was not working
        wireMockServer = WireMockServer(options()
            .dynamicPort()
            .dynamicHttpsPort()
            .keystorePath("src/test/resources/test-keystore")
            .keystorePassword("password")
            .notifier(Slf4jNotifier(true)))
        wireMockServer.start()
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.stop()
    }

    @Test
    fun pageNotFound() {
        wireMockServer.stubFor(get(urlEqualTo("/not-found"))
            .willReturn(aResponse()
                .withStatus(404)))

        assertThatThrownBy {
            fixture.contentOfSelector("http://localhost:${wireMockServer.port()}/not-found", ".bar");
        }.isInstanceOfSatisfying(HttpStatusException::class.java) {
            assertThat(it.statusCode)
                .isEqualTo(404)
            assertThat(it.url)
                .contains("/not-found")
            assertThat(it.message)
                .contains("HTTP error fetching URL. Status=404, URL=[")
        }
    }

    @Test
    fun selectorNotFound() {
        wireMockServer.stubFor(get(urlEqualTo("/selector-not-found"))
            .willReturn(aResponse()
                .withBody(body)))

        val content = fixture.contentOfSelector("http://localhost:${wireMockServer.port()}/selector-not-found", ".bar");
        assertThat(content)
            .isNull()
    }

    @Test
    fun selectorContent() {
        wireMockServer.stubFor(get(urlEqualTo("/content"))
            .willReturn(aResponse()
                .withBody(body)))

        val content = fixture.contentOfSelector("http://localhost:${wireMockServer.port()}/content", ".foo");
        assertThat(content)
            .isEqualTo("i am text")
    }

    @Test
    fun timeout() {
        wireMockServer.stubFor(get(urlEqualTo("/timeout"))
            .willReturn(aResponse()
                .withFixedDelay(5 * 1000)
                .withBody(body)))

        assertThatThrownBy {
            fixture.contentOfSelector("http://localhost:${wireMockServer.port()}/timeout", ".foo");
        }.isInstanceOfSatisfying(SocketTimeoutException::class.java) {
            assertThat(it.message)
                .isEqualTo("Read timed out")
        }
    }

    @Test
    fun https() {
        wireMockServer.stubFor(get(urlEqualTo("/https"))
            .willReturn(aResponse()
                .withBody(body)))

        val content = fixture.contentOfSelector("https://localhost:${wireMockServer.httpsPort()}/https", ".foo");
        assertThat(content)
            .isEqualTo("i am text")
    }

    @Test
    fun redirectToHttps() {
        wireMockServer.stubFor(get(urlEqualTo("/http"))
            .willReturn(aResponse()
                .withStatus(302)
                .withHeader("Location", "https://localhost:${wireMockServer.httpsPort()}/https")))
        wireMockServer.stubFor(get(urlEqualTo("/https"))
            .willReturn(aResponse()
                .withBody(body)))

        val content = fixture.contentOfSelector("https://localhost:${wireMockServer.httpsPort()}/http", ".foo");
        assertThat(content)
            .isEqualTo("i am text")
    }

    companion object {
        const val body = """
            <html>
                <body>
                    <div class="foo">i am text</div>
                </body>
            </html>
        """

        private fun acceptAllSslSocketFactory(): SSLSocketFactory {
            val sslContext = SSLContexts.custom()
                .loadTrustMaterial(null) { _, _ -> true }
                .build()
            return sslContext.socketFactory as SSLSocketFactory
        }
    }
}

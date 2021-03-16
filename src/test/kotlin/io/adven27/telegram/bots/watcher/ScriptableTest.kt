package io.adven27.telegram.bots.watcher

import org.junit.Assert.assertNotNull
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

class ScriptableTest {
    private val webClient = WebClient.builder().clientConnector(
        ReactorClientHttpConnector(HttpClient.create().followRedirect(true))
    )
    private val sut = Scriptable(webClient) { "" }
    private val url = ""

    //@Test
    operator fun invoke() {
        val (url, name, price) = sut(url)

        assertNotNull(url)
        assertNotNull(name)
        assertNotNull(price)
    }
}

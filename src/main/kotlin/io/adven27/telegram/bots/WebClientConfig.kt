package io.adven27.telegram.bots

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class WebClientConfig {
    @Bean
    fun webClientBuilder(): WebClient.Builder = WebClient.builder().clientConnector(
        ReactorClientHttpConnector(HttpClient.create().followRedirect(true))
    ).exchangeStrategies(
        ExchangeStrategies.builder().codecs { it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }.build()
    )

    @Bean
    fun webClient(builder: WebClient.Builder): WebClient = builder.build()
}

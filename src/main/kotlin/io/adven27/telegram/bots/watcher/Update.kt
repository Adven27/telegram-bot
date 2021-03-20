package io.adven27.telegram.bots.watcher

import mu.KLogging
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import javax.script.ScriptEngineManager

@Component
class Scriptable(private val webClientBuilder: WebClient.Builder, private val provider: (String) -> String) :
        (String) -> Item {
    companion object : KLogging()

    override fun invoke(url: String): Item =
        eval(provider(url), mapOf("url" to url, "logger" to logger, "webClient" to webClientBuilder.build()))
}

@Component
class ScriptProvider(val repository: ScriptsRepository) : (String) -> String {
    override fun invoke(url: String): String = repository.findByPatternNotNull()
        .find { it.pattern!!.toRegex().matches(url) }?.script ?: throw ScriptNotFound(url)
}

class ScriptNotFound(url: String) : RuntimeException("Script not found for url [$url]")

@Suppress("UNCHECKED_CAST")
fun <T> eval(script: String, context: Map<String, Any> = emptyMap()): T =
    (scriptEngine.apply { context.forEach { (k, v) -> put(k, v) } }.eval(script) as T).also {
        scriptEngine.state.history.reset()
    }

val scriptEngine =
    ScriptEngineManager().getEngineByExtension("kts").factory.scriptEngine as KotlinJsr223JvmLocalScriptEngine
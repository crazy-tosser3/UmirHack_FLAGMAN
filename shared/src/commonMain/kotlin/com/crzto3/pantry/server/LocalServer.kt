package com.crzto3.pantry.server

import com.crzto3.pantry.resources.Res
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.defaultForFilePath
import io.ktor.server.application.ApplicationCall
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receive
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.ExperimentalResourceApi

class LocalServer(val backendUrl: String) {
    private val client = HttpClient {
        expectSuccess = false
        followRedirects = false
        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            connectTimeoutMillis = 10_000
        }
    }

    private var server: EmbeddedServer<*, *>? = null

    var url: String? = null
        private set

    suspend fun start(): String {
        url?.let { return it }

        val port = try {
            launch(PREFERRED_PORT)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            launch(0)
        }
        return "http://$HOST:$port".also { url = it }
    }

    suspend fun restart(): String {
        stopServer()
        return start()
    }

    suspend fun isAlive(): Boolean {
        val base = url ?: return false
        return try {
            client.get("$base$HEALTH_PATH") { timeout { requestTimeoutMillis = 2_000 } }.status == HttpStatusCode.OK
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    suspend fun checkBackend(): Result<Unit> = try {
        client.get("$backendUrl/api/v1/auth/me") { timeout { requestTimeoutMillis = 7_000 } }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun stop() {
        stopServer()
        client.close()
    }

    private fun stopServer() {
        server?.stop(gracePeriodMillis = 0, timeoutMillis = 500)
        server = null
        url = null
    }

    private suspend fun launch(port: Int): Int {
        val instance = embeddedServer(CIO, port = port, host = HOST) {
            routing {
                get(HEALTH_PATH) { call.respondText("ok") }
                route("/api/{...}") { handle { proxy(call) } }
                get("/{path...}") { serveStatic(call) }
            }
        }
        try {
            instance.startSuspend(wait = false)
            val actualPort = instance.engine.resolvedConnectors().first().port
            server = instance
            return actualPort
        } catch (e: Throwable) {
            instance.stop(0, 0)
            throw e
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun readWebFile(path: String): ByteArray? = try {
        Res.readBytes("$WEB_ROOT/$path")
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private suspend fun serveStatic(call: ApplicationCall) {
        val segments = call.parameters.getAll("path").orEmpty().filter { it.isNotEmpty() }
        if (segments.any { it == ".." || it == "." }) {
            call.respond(HttpStatusCode.NotFound)
            return
        }
        val path = segments.joinToString("/")
        val file = path.takeIf { it.isNotEmpty() }?.let { readWebFile(it) }

        if (file != null) {
            if (path.startsWith("assets/")) {
                call.response.header(HttpHeaders.CacheControl, "public, max-age=31536000, immutable")
            }
            call.respondBytes(file, ContentType.defaultForFilePath(path))
            return
        }

        val looksLikeFile = segments.lastOrNull()?.contains('.') == true
        if (looksLikeFile) {
            call.respond(HttpStatusCode.NotFound)
            return
        }

        val index = readWebFile("index.html")
        if (index == null) {
            call.respondText(
                "Веб-клиент не найден в сборке приложения. Пересоберите проект (задача :shared:buildWebClient).",
                status = HttpStatusCode.InternalServerError,
            )
            return
        }
        call.response.header(HttpHeaders.CacheControl, "no-cache")
        call.respondBytes(index, ContentType.Text.Html)
    }

    private suspend fun proxy(call: ApplicationCall) {
        val request = call.request
        val method = request.httpMethod
        val body = if (method == HttpMethod.Get || method == HttpMethod.Head) null else call.receive<ByteArray>()
        val requestContentType = request.headers[HttpHeaders.ContentType]
            ?.let { runCatching { ContentType.parse(it) }.getOrNull() }
            ?: ContentType.Application.OctetStream

        try {
            client.prepareRequest(backendUrl + request.uri) {
                this.method = method
                request.headers.forEach { name, values ->
                    if (name.lowercase() !in SKIP_REQUEST_HEADERS) {
                        values.forEach { headers.append(name, it) }
                    }
                }
                if (body != null) {
                    setBody(ByteArrayContent(body, requestContentType))
                }
            }.execute { response ->
                call.respond(ProxiedContent(response, proxiedHeaders(response), method == HttpMethod.Head))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!call.response.isCommitted) {
                call.respondText(
                    "Бэкенд недоступен: ${e.message ?: e::class.simpleName}",
                    status = HttpStatusCode.BadGateway,
                )
            }
        }
    }

    private fun proxiedHeaders(response: HttpResponse): Headers = HeadersBuilder().apply {
        response.headers.forEach { name, values ->
            when {
                name.lowercase() in SKIP_RESPONSE_HEADERS -> Unit
                name.equals(HttpHeaders.SetCookie, ignoreCase = true) ->
                    values.forEach { append(name, rewriteSetCookie(it)) }
                name.equals(HttpHeaders.Location, ignoreCase = true) ->
                    values.forEach { append(name, it.removePrefix(backendUrl).ifEmpty { "/" }) }
                else -> appendAll(name, values)
            }
        }
    }.build()

    private class ProxiedContent(
        private val response: HttpResponse,
        override val headers: Headers,
        private val headOnly: Boolean,
    ) : OutgoingContent.WriteChannelContent() {
        override val status: HttpStatusCode = response.status
        override val contentType: ContentType? = response.contentType()

        override suspend fun writeTo(channel: ByteWriteChannel) {
            if (headOnly || status == HttpStatusCode.NoContent || status == HttpStatusCode.NotModified) return
            pump(response.bodyAsChannel(), channel)
        }

        private suspend fun pump(input: ByteReadChannel, output: ByteWriteChannel) {
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val read = input.readAvailable(buffer, 0, buffer.size)
                if (read == -1) break
                if (read > 0) {
                    output.writeFully(buffer, 0, read)
                    output.flush()
                }
            }
        }
    }

    companion object {
        const val HOST = "127.0.0.1"
        const val PREFERRED_PORT = 41_731
        private const val HEALTH_PATH = "/__pantry/health"
        private const val WEB_ROOT = "files/web"

        private val HOP_BY_HOP = setOf(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailer", "transfer-encoding", "upgrade",
        )

        private val SKIP_REQUEST_HEADERS = HOP_BY_HOP + setOf(
            "host", "content-length", "content-type",

            "origin", "referer",

            "accept-encoding",
        )

        private val SKIP_RESPONSE_HEADERS = HOP_BY_HOP + setOf(
            "content-length", "content-type", "content-encoding",
        )

        internal fun rewriteSetCookie(value: String): String =
            value.split(';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .filterNot { attr ->
                    attr.equals("secure", ignoreCase = true) ||
                        attr.equals("partitioned", ignoreCase = true) ||
                        attr.startsWith("domain=", ignoreCase = true)
                }
                .map { attr -> if (attr.equals("samesite=none", ignoreCase = true)) "SameSite=Lax" else attr }
                .joinToString("; ")
    }
}

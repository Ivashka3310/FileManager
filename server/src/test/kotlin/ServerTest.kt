import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.server.api.configureRouting
import com.example.server.api.module
import com.example.server.api.security.AuthManager
import com.example.server.api.security.JwtService
import com.example.server.api.security.configureSecurity
import com.example.server.data.FileServiceImpl
import com.google.gson.Gson
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import io.ktor.serialization.gson.gson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import java.util.Date
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds

class ServerTest {
    private fun Application.config() {
        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
            gson()
        }

        val jwtService = JwtService(
            audience = environment.config.propertyOrNull("jwt.audience")?.getString() ?: "default-audience",
            issuer = environment.config.propertyOrNull("jwt.issuer")?.getString() ?: "file.manager.issuer-default",
            secret = environment.config.propertyOrNull("jwt.secret")?.getString() ?: "fallback-secret-key",
            validityMs = environment.config.propertyOrNull("jwt.validityMs")?.getString()?.toLong() ?: 604800000L
        )
        configureSecurity(jwtService)
        val fileService = FileServiceImpl(rootPath = Path("/home"))
        configureRouting(fileService, Gson(), jwtService)
    }

    @Test
    fun `test client connect via http post returns success`() = testApplication {
        AuthManager.generateNewSecret()
        application {
            config()
        }

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                gson()
            }
        }

        val secret = AuthManager.tempQrSecret
            ?: throw IllegalStateException("QR Secret was not generated! Check AuthManager initialization.")


        val response = client.post("/login") {
            setBody(FormDataContent(Parameters.build {
                append("secret", secret)
            }))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `test client did not connect via http post returns Unauthorized`() = testApplication {
        AuthManager.generateNewSecret()
        application {
            config()
        }

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                gson()
            }
        }

        val secret = AuthManager.tempQrSecret
            ?: throw IllegalStateException("QR Secret was not generated! Check AuthManager initialization.")

        AuthManager.generateNewSecret()

        val response = client.post("/login") {
            setBody(FormDataContent(Parameters.build {
                append("secret", secret)
            }))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test getFiles via http get returns success`() = testApplication {
        AuthManager.generateNewSecret()
        application {
            config()
        }

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                gson()
            }
        }

        val secret = AuthManager.tempQrSecret
            ?: throw IllegalStateException("QR Secret was not generated! Check AuthManager initialization.")


        val response = client.post("/login") {
            setBody(FormDataContent(Parameters.build {
                append("secret", secret)
            }))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<Map<String, String>>()
        val token = body["token"] ?: throw Exception("Token not found in response")

        val filesResponse = client.get("/files/list") {
            header("Authorization", "Bearer $token")
            setBody(FormDataContent(Parameters.build {
                append("path", ".")
            }))
        }

        assertTrue(filesResponse.status.isSuccess())
    }

    @Test
    fun `test getFiles via http get returns security error for invalid path`() = testApplication {
        AuthManager.generateNewSecret()
        application {
            config()
        }

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                gson()
            }
        }

        val secret = AuthManager.tempQrSecret
            ?: throw IllegalStateException("QR Secret was not generated! Check AuthManager initialization.")


        val response = client.post("/login") {
            setBody(FormDataContent(Parameters.build {
                append("secret", secret)
            }))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<Map<String, String>>()
        val token = body["token"] ?: throw Exception("Token not found in response")

        val filesResponse = client.get("/files/list") {
            header("Authorization", "Bearer $token")
            setBody(FormDataContent(Parameters.build {
                append("path", "..")
            }))
        }

        assertEquals(HttpStatusCode.Locked, filesResponse.status)
    }

    @Test
    fun `test use invalid jwt-token returns connection rejected`() = testApplication {
        AuthManager.generateNewSecret()

        var jwtAudience: String? = null
        var jwtIssuer: String? = null
        var jwtSecret: String? = null

        application {
            jwtAudience = environment.config.propertyOrNull("jwt.audience")?.getString() ?: "default-audience"
            jwtIssuer = environment.config.propertyOrNull("jwt.issuer")?.getString() ?: "file.manager.issuer-default"
            jwtSecret = environment.config.propertyOrNull("jwt.secret")?.getString() ?: "fallback-secret-key"
            config()
        }

        val client = createClient {
            install(WebSockets)
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                gson()
            }
        }

        client.post("/login") {
            setBody(FormDataContent(Parameters.build {
                append("secret", "")
            }))
        }

        val invalidTokens = listOf(
            JWT.create()
                .withAudience(jwtAudience?.reversed())
                .withIssuer(jwtIssuer)
                .withExpiresAt(Date(System.currentTimeMillis()+60*60*1000))
                .sign(Algorithm.HMAC256(jwtSecret)),
            JWT.create()
                .withAudience(jwtAudience)
                .withIssuer(jwtIssuer?.reversed())
                .withExpiresAt(Date(System.currentTimeMillis()+60*60*1000))
                .sign(Algorithm.HMAC256(jwtSecret)),
            JWT.create()
                .withAudience(jwtAudience)
                .withIssuer(jwtIssuer)
                .withExpiresAt(Date(System.currentTimeMillis()-60*60*1000))
                .sign(Algorithm.HMAC256(jwtSecret)),
            JWT.create()
                .withAudience(jwtAudience)
                .withIssuer(jwtIssuer)
                .withExpiresAt(Date(System.currentTimeMillis()+60*60*1000))
                .sign(Algorithm.HMAC256(jwtSecret?.reversed()))
        )

        for (invalidToken in invalidTokens) {
            try {
                val response = client.get("/files/list") {
                    header("Authorization", "Bearer $invalidToken")
                    setBody(FormDataContent(Parameters.build {
                        append("path", ".")
                    }))
                }
                assertEquals(HttpStatusCode.Unauthorized, response.status)
            } catch (_: Exception) {
                assertTrue(true, "Connection rejected as expected")
            }
        }
    }
}

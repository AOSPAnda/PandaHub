package co.aospa.hub.network

import co.aospa.hub.data.model.UpdateResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

class UpdateService {
    private val client = HttpClient(OkHttp)

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val baseUrl = "https://raw.githubusercontent.com/AOSPAnda/ota/master/updates"

    suspend fun getUpdates(device: String): Result<UpdateResponse> = runCatching {
        val responseText: String = client.get("$baseUrl/$device").bodyAsText()
        json.decodeFromString<UpdateResponse>(responseText)
    }
}
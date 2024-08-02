package co.aospa.hub.network

import co.aospa.hub.data.UpdateResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import kotlinx.serialization.json.Json

class UpdateService {
    private val client = HttpClient(OkHttp)

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getUpdates(device: String): Result<UpdateResponse> {
        return try {
            val responseString: String =
                client.get("https://raw.githubusercontent.com/AOSPAnda/ota/master/updates/$device")
                    .body()
            val response = json.decodeFromString<UpdateResponse>(responseString)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
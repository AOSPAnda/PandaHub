package co.aospa.hub.download

import android.app.Service
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var downloadJob: Job? = null
    private var currentFile: File? = null
    private var currentDownloadId: String? = null

    private val client = HttpClient(OkHttp)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            DownloadManager.ACTION_START -> {
                intent.getStringExtra(DownloadManager.EXTRA_URL)?.let { url ->
                    intent.getStringExtra(DownloadManager.EXTRA_FILE_NAME)?.let { fileName ->
                        intent.getStringExtra(DownloadManager.EXTRA_DOWNLOAD_ID)
                            ?.let { downloadId ->
                                startDownload(url, fileName, downloadId)
                            }
                    }
                }
            }

            DownloadManager.ACTION_PAUSE -> pauseDownload()
            DownloadManager.ACTION_RESUME -> resumeDownload()
            DownloadManager.ACTION_CANCEL -> cancelDownload()
        }
        return START_STICKY
    }

    private fun startDownload(url: String, fileName: String, downloadId: String) {
        if (downloadJob?.isActive == true) return

        currentDownloadId = downloadId
        currentFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        val downloadedBytes = currentFile?.length() ?: 0

        downloadJob = serviceScope.launch {
            try {
                updateDownloadStatus(DownloadStatus.Preparing)

                val response = client.get(url) {
                    if (downloadedBytes > 0) {
                        header(HttpHeaders.Range, "bytes=$downloadedBytes-")
                    }
                }

                if (!response.status.isSuccess()) throw Exception("Unexpected code ${response.status}")

                val channel = response.bodyAsChannel()
                val contentLength = response.contentLength()?.toLong() ?: -1L

                withContext(Dispatchers.IO) {
                    currentFile?.outputStream()?.use { outputStream ->
                        var totalBytes = downloadedBytes
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int

                        while (channel.readAvailable(buffer, 0, buffer.size)
                                .also { bytesRead = it } != -1
                        ) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead

                            val progress = if (contentLength > 0) {
                                ((totalBytes.toFloat() / contentLength) * 100).roundToInt()
                            } else {
                                -1 // indeterminate progress
                            }
                            updateDownloadStatus(DownloadStatus.Downloading(progress))

                            yield() // Cooperate with cancellation
                        }
                    }
                }

                currentFile?.let { file ->
                    updateDownloadStatus(DownloadStatus.Completed(file))
                }

                stopSelf()
            } catch (e: Exception) {
                updateDownloadStatus(DownloadStatus.Failed(e.message ?: "Unknown error occurred"))
                deletePartialFile()
                stopSelf()
            }
        }
    }

    private fun pauseDownload() {
        downloadJob?.cancel()
        updateDownloadStatus(
            DownloadStatus.Paused(
                currentFile?.absolutePath.orEmpty(),
                currentFile?.name.orEmpty()
            )
        )
    }

    private fun resumeDownload() {
        _downloadStatus.value.let { status ->
            if (status is DownloadStatus.Paused) {
                startDownload(status.url, status.fileName, currentDownloadId.orEmpty())
            }
        }
    }

    private fun cancelDownload() {
        downloadJob?.cancel()
        deletePartialFile()
        updateDownloadStatus(DownloadStatus.Cancelled)
        stopSelf()
    }

    private fun deletePartialFile() {
        currentFile?.takeIf { it.exists() }?.delete()
        currentFile = null
    }

    private fun updateDownloadStatus(status: DownloadStatus) {
        serviceScope.launch {
            _downloadStatus.emit(status)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        client.close()
    }

    companion object {
        private const val BUFFER_SIZE = 8192  // 8 KB buffer

        private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
        val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus
    }
}
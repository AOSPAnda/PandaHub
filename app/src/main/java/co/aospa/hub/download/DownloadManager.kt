package co.aospa.hub.download

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.StateFlow

class DownloadManager(private val context: Context) {

    private val downloadService = Intent(context, DownloadService::class.java)
    private var currentDownloadId: String? = null

    fun startDownload(url: String, fileName: String) {
        currentDownloadId = System.currentTimeMillis().toString()
        downloadService.apply {
            action = ACTION_START
            putExtra(EXTRA_DOWNLOAD_ID, currentDownloadId)
            putExtra(EXTRA_URL, url)
            putExtra(EXTRA_FILE_NAME, fileName)
        }
        context.startService(downloadService)
    }

    fun pauseDownload() {
        currentDownloadId?.let {
            downloadService.apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_DOWNLOAD_ID, it)
            }
            context.startService(downloadService)
        }
    }

    fun resumeDownload() {
        currentDownloadId?.let {
            downloadService.apply {
                action = ACTION_RESUME
                putExtra(EXTRA_DOWNLOAD_ID, it)
            }
            context.startService(downloadService)
        }
    }

    fun cancelDownload() {
        currentDownloadId?.let {
            downloadService.apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_DOWNLOAD_ID, it)
            }
            context.startService(downloadService)
            currentDownloadId = null
        }
    }

    fun getDownloadStatus(): StateFlow<DownloadStatus?> = DownloadService.downloadStatus

    companion object {
        const val ACTION_START = "co.aospa.hub.download.ACTION_START"
        const val ACTION_PAUSE = "co.aospa.hub.download.ACTION_PAUSE"
        const val ACTION_RESUME = "co.aospa.hub.download.ACTION_RESUME"
        const val ACTION_CANCEL = "co.aospa.hub.download.ACTION_CANCEL"
        const val EXTRA_DOWNLOAD_ID = "co.aospa.hub.download.EXTRA_DOWNLOAD_ID"
        const val EXTRA_URL = "co.aospa.hub.download.EXTRA_URL"
        const val EXTRA_FILE_NAME = "co.aospa.hub.download.EXTRA_FILE_NAME"
    }
}
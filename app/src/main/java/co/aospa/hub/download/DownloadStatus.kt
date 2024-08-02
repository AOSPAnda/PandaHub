package co.aospa.hub.download

import java.io.File

sealed class DownloadStatus {
    data object Idle : DownloadStatus()
    data object Preparing : DownloadStatus()
    data class Downloading(val progress: Int) : DownloadStatus()
    data class Paused(val url: String, val fileName: String) : DownloadStatus()
    data class Completed(val file: File) : DownloadStatus()
    data class Failed(val reason: String) : DownloadStatus()
    data object Cancelled : DownloadStatus()
}
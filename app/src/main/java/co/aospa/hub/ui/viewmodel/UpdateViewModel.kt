package co.aospa.hub.ui.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import co.aospa.hub.data.local.PreferencesManager
import co.aospa.hub.data.model.Update
import co.aospa.hub.download.DownloadManager
import co.aospa.hub.download.DownloadStatus
import co.aospa.hub.network.UpdateService
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val updateService = UpdateService()
    private val preferencesManager = PreferencesManager(application)
    private val downloadManager = DownloadManager(application)

    private val _uiState = MutableStateFlow<UiState>(UiState.NoUpdate)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _currentAndroidVersion = MutableStateFlow(Build.VERSION.RELEASE)
    val currentAndroidVersion: StateFlow<String> = _currentAndroidVersion.asStateFlow()

    private val _currentSecurityPatch =
        MutableStateFlow(formatSecurityPatch(Build.VERSION.SECURITY_PATCH))
    val currentSecurityPatch: StateFlow<String> = _currentSecurityPatch.asStateFlow()

    private val _lastSuccessfulCheck = MutableStateFlow<Instant?>(null)
    val lastSuccessfulCheck: StateFlow<String> = MutableStateFlow("Never")

    val downloadStatus: StateFlow<DownloadStatus?> = downloadManager.getDownloadStatus()

    private var currentUpdate: Update? = null


    init {
        loadLastSuccessfulCheck()
        observeDownloadStatus()
    }

    private fun formatSecurityPatch(securityPatch: String): String {
        return try {
            val date = LocalDateTime.parse(securityPatch)
            date.toJavaLocalDateTime()
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.US))
        } catch (e: Exception) {
            securityPatch
        }
    }

    private fun formatLastSuccessfulCheck(instant: Instant?): String {
        if (instant == null) return "Never"

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val lastCheck = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        return if (now.date == lastCheck.date) {
            lastCheck.toJavaLocalDateTime()
                .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
        } else {
            lastCheck.toJavaLocalDateTime()
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.getDefault()))
        }
    }

    private fun loadLastSuccessfulCheck() {
        viewModelScope.launch {
            preferencesManager.lastSuccessfulCheck.first()?.let { timestamp ->
                _lastSuccessfulCheck.value = Instant.fromEpochMilliseconds(timestamp)
                (lastSuccessfulCheck as MutableStateFlow).value =
                    formatLastSuccessfulCheck(_lastSuccessfulCheck.value)
            }
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                updateService.getUpdates(Build.DEVICE).fold(
                    onSuccess = { response ->
                        val latestUpdate = response.updates
                            .filter { it.dateTime > Instant.fromEpochMilliseconds(Build.TIME) }
                            .maxByOrNull { it.dateTime }
                        _uiState.value =
                            latestUpdate?.let { UiState.UpdateAvailable(it) } ?: UiState.NoUpdate
                        if (_uiState.value !is UiState.Error) {
                            _lastSuccessfulCheck.value = Clock.System.now()
                            (lastSuccessfulCheck as MutableStateFlow).value =
                                formatLastSuccessfulCheck(_lastSuccessfulCheck.value)
                            preferencesManager.saveLastSuccessfulCheck(_lastSuccessfulCheck.value!!.toEpochMilliseconds())
                        }
                    },
                    onFailure = { e ->
                        _uiState.value =
                            UiState.Error("Failed to fetch update: ${e.localizedMessage}")
                    }
                )
            } catch (e: Exception) {
                _uiState.value =
                    UiState.Error("An unexpected error occurred: ${e.localizedMessage}")
            }
        }
    }

    fun startDownload(update: Update) {
        currentUpdate = update
        downloadManager.startDownload(update.url, update.filename)
    }

    fun pauseDownload() {
        downloadManager.pauseDownload()
    }

    fun resumeDownload() {
        downloadManager.resumeDownload()
    }

    fun cancelDownload() {
        downloadManager.cancelDownload()
    }

    private fun observeDownloadStatus() {
        viewModelScope.launch {
            downloadManager.getDownloadStatus().collect { status ->
                when (status) {
                    is DownloadStatus.Idle -> {
                        _uiState.value = UiState.NoUpdate
                    }

                    is DownloadStatus.Preparing -> {
                        _uiState.value = UiState.Preparing(currentUpdate)
                    }

                    is DownloadStatus.Downloading -> {
                        _uiState.value = UiState.Downloading(currentUpdate, status.progress)
                    }

                    is DownloadStatus.Completed -> {
                        _uiState.value = UiState.Downloaded(currentUpdate, status.file)
                    }

                    is DownloadStatus.Failed -> {
                        _uiState.value = UiState.Error(status.reason)
                    }

                    is DownloadStatus.Paused -> {
                        _uiState.value = UiState.Paused(currentUpdate)
                    }

                    is DownloadStatus.Cancelled -> {
                        _uiState.value = UiState.Cancelled
                    }

                    null -> TODO()
                }
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        object NoUpdate : UiState()
        data class Preparing(val update: Update?) : UiState()
        data class UpdateAvailable(val update: Update) : UiState()
        data class Downloading(val update: Update?, val progress: Int) : UiState()
        data class Downloaded(val update: Update?, val file: File) : UiState()
        data class Paused(val update: Update?) : UiState()
        object Cancelled : UiState()
        data class Error(val message: String) : UiState()
    }
}
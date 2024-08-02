package co.aospa.hub.viewmodel

import android.app.Application
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import co.aospa.hub.data.PreferencesManager
import co.aospa.hub.data.Update
import co.aospa.hub.network.UpdateService
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val updateService = UpdateService()
    private val preferencesManager = PreferencesManager(application)

    var uiState by mutableStateOf<UiState>(UiState.NoUpdate)
        private set

    var currentAndroidVersion by mutableStateOf("Unknown")
        private set

    var currentSecurityPatch by mutableStateOf("Unknown")
        private set

    private var _lastSuccessfulCheck: Date? = null
    var lastSuccessfulCheck: String by mutableStateOf("Never")
        private set

    init {
        currentAndroidVersion = Build.VERSION.RELEASE
        currentSecurityPatch = formatSecurityPatch(Build.VERSION.SECURITY_PATCH)
        loadLastSuccessfulCheck()
    }

    private fun formatSecurityPatch(securityPatch: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
            val date = inputFormat.parse(securityPatch)
            outputFormat.format(date ?: return securityPatch)
        } catch (e: ParseException) {
            securityPatch
        }
    }

    private fun formatLastSuccessfulCheck(): String {
        if (_lastSuccessfulCheck == null) return "Never"

        val now = Calendar.getInstance()
        val lastCheck = Calendar.getInstance().apply { time = _lastSuccessfulCheck!! }

        return if (now.get(Calendar.YEAR) == lastCheck.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == lastCheck.get(Calendar.DAY_OF_YEAR)
        ) {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(_lastSuccessfulCheck!!)
        } else {
            SimpleDateFormat(
                "MMM d, yyyy h:mm a",
                Locale.getDefault()
            ).format(_lastSuccessfulCheck!!)
        }
    }

    private fun loadLastSuccessfulCheck() {
        viewModelScope.launch {
            val lastCheckTimestamp = preferencesManager.lastSuccessfulCheck.first()
            if (lastCheckTimestamp != null) {
                _lastSuccessfulCheck = Date(lastCheckTimestamp)
                lastSuccessfulCheck = formatLastSuccessfulCheck()
            }
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            uiState = UiState.Loading
            try {
                val result = updateService.getUpdates(Build.DEVICE)
                result.fold(
                    onSuccess = { response ->
                        val latestUpdate = response.updates
                            .filter { it.datetime.toLong() > Build.TIME / 1000 }
                            .maxByOrNull { it.datetime.toLong() }
                        uiState = if (latestUpdate != null) {
                            UiState.UpdateAvailable(latestUpdate)
                        } else {
                            UiState.NoUpdate
                        }
                        if (uiState is UiState.UpdateAvailable ||
                            uiState is UiState.NoUpdate
                        ) {
                            _lastSuccessfulCheck = Date()
                            lastSuccessfulCheck = formatLastSuccessfulCheck()
                            preferencesManager.saveLastSuccessfulCheck(_lastSuccessfulCheck!!)
                        }
                    },
                    onFailure = { e ->
                        uiState = UiState.Error("Failed to fetch update: ${e.localizedMessage}")
                    }
                )
            } catch (e: Exception) {
                uiState = UiState.Error("An unexpected error occurred: ${e.localizedMessage}")
            }
        }
    }

    sealed class UiState {
        data object Loading : UiState()
        data object NoUpdate : UiState()
        data class UpdateAvailable(val update: Update) : UiState()
        data class Error(val message: String) : UiState()
    }
}
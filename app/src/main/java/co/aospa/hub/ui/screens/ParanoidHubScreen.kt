package co.aospa.hub.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.aospa.hub.ui.components.UpdateAvailableScreen
import co.aospa.hub.ui.components.UpdateErrorScreen
import co.aospa.hub.ui.components.UpdateLoadingScreen
import co.aospa.hub.ui.components.UpdateStatusScreen
import co.aospa.hub.ui.viewmodel.UpdateViewModel

@Composable
fun ParanoidHubScreen(
    modifier: Modifier = Modifier,
    viewModel: UpdateViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentAndroidVersion by viewModel.currentAndroidVersion.collectAsState()
    val currentSecurityPatch by viewModel.currentSecurityPatch.collectAsState()
    val lastSuccessfulCheck by viewModel.lastSuccessfulCheck.collectAsState()
    val downloadStatus by viewModel.downloadStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        when (val state = uiState) {
            is UpdateViewModel.UiState.Loading -> UpdateLoadingScreen(modifier)
            is UpdateViewModel.UiState.NoUpdate,
            is UpdateViewModel.UiState.Cancelled -> UpdateStatusScreen(
                modifier = modifier,
                title = if (state is UpdateViewModel.UiState.Cancelled) "Download cancelled" else "Your system is up to date",
                currentAndroidVersion = currentAndroidVersion,
                currentSecurityPatch = currentSecurityPatch,
                lastSuccessfulCheck = lastSuccessfulCheck,
                onCheckForUpdate = viewModel::checkForUpdate
            )

            is UpdateViewModel.UiState.Preparing,
            is UpdateViewModel.UiState.UpdateAvailable,
            is UpdateViewModel.UiState.Downloading,
            is UpdateViewModel.UiState.Downloaded,
            is UpdateViewModel.UiState.Paused -> {
                val update = when (state) {
                    is UpdateViewModel.UiState.Preparing -> state.update
                    is UpdateViewModel.UiState.UpdateAvailable -> state.update
                    is UpdateViewModel.UiState.Downloading -> state.update
                    is UpdateViewModel.UiState.Downloaded -> state.update
                    is UpdateViewModel.UiState.Paused -> state.update
                    else -> null
                }
                update?.let {
                    UpdateAvailableScreen(
                        modifier = modifier,
                        update = it,
                        downloadStatus = downloadStatus,
                        onDownload = { viewModel.startDownload(it) },
                        onPause = viewModel::pauseDownload,
                        onResume = viewModel::resumeDownload,
                        onCancel = viewModel::cancelDownload,
                        onInstall = {}
                    )
                }
            }

            is UpdateViewModel.UiState.Error -> UpdateErrorScreen(
                modifier = modifier,
                errorMessage = state.message,
                onRetry = viewModel::checkForUpdate
            )
        }
    }
}
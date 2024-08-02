package co.aospa.hub.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import co.aospa.hub.data.model.Update
import co.aospa.hub.download.DownloadStatus
import co.aospa.hub.utils.formatFileSize

@Composable
fun UpdateAvailableScreen(
    modifier: Modifier = Modifier,
    update: Update,
    downloadStatus: DownloadStatus?,
    onDownload: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier.padding(vertical = 16.dp)
    ) {
        AOSPALogo(
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = when (downloadStatus) {
                is DownloadStatus.Idle -> "Paranoid update available"
                is DownloadStatus.Preparing -> "Preparing download..."
                is DownloadStatus.Downloading -> "Downloading update..."
                is DownloadStatus.Paused -> "Download paused"
                is DownloadStatus.Completed -> "Ready to install"
                is DownloadStatus.Cancelled -> "Download cancelled"
                is DownloadStatus.Failed -> "Download failed"
                null -> "Paranoid update available"
            },
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        when (downloadStatus) {
            is DownloadStatus.Preparing -> {
                Spacer(modifier = Modifier.height(24.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            is DownloadStatus.Downloading -> {
                Spacer(modifier = Modifier.height(24.dp))
                LinearProgressIndicator(
                    progress = { downloadStatus.progress.toFloat() / 100 },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            else -> {
                // No progress bar for other states
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
        if (update.changelogDevice.isNotBlank()) {
            Text(
                text = update.changelogDevice,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
        }
        val annotatedString = buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                append("Learn more at ")
            }
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("https://paranoidandroid.co/changelog")
            }
        }
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                uriHandler.openUri("https://paranoidandroid.co/changelog")
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Downloading updates over a mobile network or while roaming may cause additional charges.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(48.dp))
        UpdateDetailItem("Update size", formatFileSize(update.size))
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            when (downloadStatus) {
                is DownloadStatus.Preparing, is DownloadStatus.Downloading, is DownloadStatus.Paused -> {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Cancel")
                    }
                    if (downloadStatus != DownloadStatus.Preparing) {
                        Button(
                            onClick = if (downloadStatus is DownloadStatus.Downloading) onPause else onResume,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(if (downloadStatus is DownloadStatus.Downloading) "Pause" else "Resume")
                        }
                    }
                }

                null, is DownloadStatus.Idle, is DownloadStatus.Cancelled, is DownloadStatus.Failed -> {
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Download")
                    }
                }

                is DownloadStatus.Completed -> {
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onInstall,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Install")
                    }
                }
            }
        }
    }
}
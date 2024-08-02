package co.aospa.hub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import co.aospa.hub.ui.screens.ParanoidHubScreen
import co.aospa.hub.ui.theme.ParanoidHubTheme
import co.aospa.hub.ui.viewmodel.UpdateViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParanoidHubTheme {
                ParanoidHubApp()
            }
        }
    }
}

@Composable
fun ParanoidHubApp(viewModel: UpdateViewModel = viewModel()) {
    Scaffold { innerPadding ->
        ParanoidHubScreen(
            modifier = Modifier.padding(innerPadding),
            viewModel = viewModel
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ParanoidHubScreenPreview() {
    ParanoidHubTheme {
        ParanoidHubApp()
    }
}
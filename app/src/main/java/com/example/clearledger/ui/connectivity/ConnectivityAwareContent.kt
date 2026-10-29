package com.example.clearledger.ui.connectivity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clearledger.core.network.NetworkStatus

@Composable
fun ConnectivityAwareContent(content: @Composable () -> Unit) {
    val viewModel: ConnectivityViewModel = hiltViewModel()
    val status = viewModel.status.collectAsStateWithLifecycle().value

    Column(modifier = Modifier.fillMaxSize()) {
        if (status == NetworkStatus.Offline) {
            OfflineBanner()
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            content()
        }
    }
}

@Composable
fun OfflineBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Text(
            text = "Offline — cached data is available. Changes will sync when connected.",
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

package com.lockedapp.vault.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.lockedapp.vault.VaultViewModel
import com.lockedapp.vault.biometric.BiometricHelper
import com.lockedapp.vault.data.VaultItemEntity
import com.lockedapp.vault.data.VaultItemType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(activity: FragmentActivity, viewModel: VaultViewModel) {
    val items by viewModel.items.collectAsState()
    val scope = rememberCoroutineScope()
    var biometricEnabled by remember { mutableStateOf(viewModel.pinManager.isBiometricEnabled()) }
    val biometricAvailable = BiometricHelper.isAvailable(activity)

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importFile(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vault") },
                actions = {
                    if (biometricAvailable) {
                        IconButton(onClick = {
                            biometricEnabled = !biometricEnabled
                            viewModel.pinManager.setBiometricEnabled(biometricEnabled)
                        }) {
                            Icon(
                                Icons.Filled.Face,
                                contentDescription = "Toggle face/fingerprint unlock",
                                tint = if (biometricEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Import file")
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Vault is empty.\nTap + to import a file.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    VaultItemCard(
                        item = item,
                        onOpen = {
                            scope.launch {
                                val uri = viewModel.openForViewing(item)
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, item.mimeType)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                ContextCompat.startActivity(activity, intent, null)
                            }
                        },
                        onDelete = { viewModel.deleteItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultItemCard(
    item: VaultItemEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForType(item.type),
                    contentDescription = item.type.name,
                    modifier = Modifier.fillMaxWidth(0.5f).aspectRatio(1f),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                item.displayName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onOpen) {
                    Text("Open", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.aspectRatio(1f))
                }
            }
        }
    }
}

private fun iconForType(type: VaultItemType) = when (type) {
    VaultItemType.IMAGE -> Icons.Filled.Image
    VaultItemType.VIDEO -> Icons.Filled.VideoFile
    VaultItemType.AUDIO -> Icons.Filled.AudioFile
    VaultItemType.DOCUMENT -> Icons.Filled.Description
    VaultItemType.OTHER -> Icons.Filled.InsertDriveFile
}

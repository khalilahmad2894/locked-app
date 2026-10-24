package com.lockedapp.vault

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lockedapp.vault.ui.screens.CameraCaptureScreen
import com.lockedapp.vault.ui.screens.PhotoViewerScreen
import com.lockedapp.vault.ui.screens.SetupPinScreen
import com.lockedapp.vault.ui.screens.UnlockScreen
import com.lockedapp.vault.ui.screens.VaultScreen
import com.lockedapp.vault.ui.screens.VideoPlayerScreen
import com.lockedapp.vault.ui.theme.LockedVaultTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LockedVaultTheme {
                VaultApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun VaultApp(viewModel: VaultViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val startDestination = if (viewModel.pinManager.isPinSet()) "unlock" else "setup"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("setup") {
            SetupPinScreen(onPinCreated = { pin ->
                viewModel.pinManager.setPin(pin)
                navController.navigate("vault") {
                    popUpTo("setup") { inclusive = true }
                }
            })
        }
        composable("unlock") {
            UnlockScreen(
                pinManager = viewModel.pinManager,
                onUnlocked = {
                    navController.navigate("vault") {
                        popUpTo("unlock") { inclusive = true }
                    }
                }
            )
        }
        composable("vault") {
            VaultScreen(
                viewModel = viewModel,
                onOpenPhoto = { index ->
                    viewModel.photoViewerStartIndex = index
                    navController.navigate("photoViewer")
                },
                onOpenVideo = { item ->
                    viewModel.activeVideoItem = item
                    navController.navigate("videoPlayer")
                },
                onOpenDocument = { item ->
                    scope.launch {
                        val uri = viewModel.openForViewing(item)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, item.mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        ContextCompat.startActivity(context, intent, null)
                    }
                },
                onOpenCamera = { navController.navigate("camera") }
            )
        }
        composable("camera") {
            CameraCaptureScreen(viewModel = viewModel, onDone = { navController.popBackStack() })
        }
        composable("photoViewer") {
            PhotoViewerScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("videoPlayer") {
            VideoPlayerScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}

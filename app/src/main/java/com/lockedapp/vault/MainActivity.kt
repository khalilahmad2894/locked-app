package com.lockedapp.vault

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lockedapp.vault.ui.screens.SetupPinScreen
import com.lockedapp.vault.ui.screens.UnlockScreen
import com.lockedapp.vault.ui.screens.VaultScreen
import com.lockedapp.vault.ui.theme.LockedVaultTheme

class MainActivity : FragmentActivity() {

    private val viewModel: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LockedVaultTheme {
                VaultApp(activity = this, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun VaultApp(activity: FragmentActivity, viewModel: VaultViewModel) {
    val navController = rememberNavController()
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
                activity = activity,
                pinManager = viewModel.pinManager,
                onUnlocked = {
                    navController.navigate("vault") {
                        popUpTo("unlock") { inclusive = true }
                    }
                }
            )
        }
        composable("vault") {
            VaultScreen(activity = activity, viewModel = viewModel)
        }
    }
}

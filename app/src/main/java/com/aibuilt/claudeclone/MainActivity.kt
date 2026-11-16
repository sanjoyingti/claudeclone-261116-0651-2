package com.aibuilt.claudeclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.claudeclone.ui.AppViewModel
import com.aibuilt.claudeclone.ui.VmFactory
import com.aibuilt.claudeclone.ui.navigation.AppNavHost
import com.aibuilt.claudeclone.ui.theme.ClaudeCloneTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as ClaudeCloneApp).container
        setContent {
            val appViewModel: AppViewModel = viewModel(factory = VmFactory.app(container))
            val settings by appViewModel.settings.collectAsStateWithLifecycle()
            ClaudeCloneTheme(themeMode = settings.themeMode) {
                AppNavHost(container = container)
            }
        }
    }
}

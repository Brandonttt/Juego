package com.example.juego

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels // <-- IMPORTANTE
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.juego.ui.screens.GameScreen
import com.example.juego.ui.theme.JuegoTheme
import com.example.juego.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

    // Instancia el ViewModel
    private val gameViewModel by viewModels<GameViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JuegoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Llama a tu pantalla principal del juego
                    GameScreen(viewModel = gameViewModel)
                }
            }
        }
    }
}
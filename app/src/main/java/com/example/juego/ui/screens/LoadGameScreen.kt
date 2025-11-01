// En: com/example/juego/ui/screens/LoadGameScreen.kt
package com.example.juego.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.juego.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadGameScreen(
    viewModel: GameViewModel,
    onGameLoaded: () -> Unit, // Para navegar al GameScreen
    onNavigateBack: () -> Unit
) {
    // Obtenemos la lista de archivos CADA VEZ que se muestra la pantalla
    val savedGames by remember { mutableStateOf(viewModel.getSavedGames()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cargar Partida") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF003300),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF003300)
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (savedGames.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("No hay partidas guardadas.", color = Color.White)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(savedGames) { filename ->
                        Text(
                            text = filename,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.loadGame(filename)
                                    onGameLoaded()
                                }
                                .background(Color(0xFF004400))
                                .padding(16.dp)
                        )
                        Divider(color = Color(0xFF003300))
                    }
                }
            }
            Button(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Volver")
            }
        }
    }
}
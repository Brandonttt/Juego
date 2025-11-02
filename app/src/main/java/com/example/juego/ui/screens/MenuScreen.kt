// En: com/example/juego/ui/screens/MenuScreen.kt
package com.example.juego.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuScreen(
    onOnePlayerClick: () -> Unit,
    onTwoPlayerClick: () -> Unit,
    onLoadGameClick: () -> Unit, // <-- NUEVO
    onOptionsClick: () -> Unit,
    onImportGameClick: () -> Unit// <-- NUEVO
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF003300))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Blackjack 21",
            fontSize = 40.sp,
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(60.dp)) // Menos espacio

        // --- Botones de Jugar ---
        Button(onClick = onOnePlayerClick, modifier = Modifier.fillMaxWidth().height(60.dp)) {
            Text(text = "1 Jugador", fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onTwoPlayerClick, modifier = Modifier.fillMaxWidth().height(60.dp)) {
            Text(text = "2 Jugadores", fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))

        // --- Botones de Gestión ---
        Button(onClick = onLoadGameClick, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text(text = "Cargar Partida", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onImportGameClick, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text(text = "Importar Partida", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onOptionsClick, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text(text = "Opciones", fontSize = 18.sp)
        }
    }
}
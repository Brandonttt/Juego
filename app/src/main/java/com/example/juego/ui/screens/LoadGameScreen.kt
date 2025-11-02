// En: com/example/juego/ui/screens/LoadGameScreen.kt
package com.example.juego.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.juego.viewmodel.GameViewModel
import com.example.juego.viewmodel.SaveGameMetadata
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadGameScreen(
    viewModel: GameViewModel,
    onGameLoaded: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val savedGames by remember { mutableStateOf(viewModel.getSavedGamesMetadata()) }
    val context = LocalContext.current

    // --- LÓGICA DE EXPORTACIÓN (NUEVA) ---
    var fileToExport by remember { mutableStateOf<SaveGameMetadata?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"), // Permite cualquier tipo
        onResult = { uri ->
            if (uri != null && fileToExport != null) {
                viewModel.exportGame(fileToExport!!.filename, uri) { success ->
                    val message = if (success) "¡Exportado con éxito!" else "Error al exportar"
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

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
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No hay partidas guardadas.", color = Color.White)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(savedGames) { metadata ->
                        SaveGameItem(
                            metadata = metadata,
                            onLoad = {
                                viewModel.loadGame(metadata.filename)
                                onGameLoaded()
                            },
                            onExport = {
                                fileToExport = metadata
                                // Sugiere el nombre de archivo al usuario
                                exportLauncher.launch(metadata.filename)
                            }
                        )
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

// --- NUEVO COMPOSABLE PARA MOSTRAR LOS METADATOS ---
@Composable
fun SaveGameItem(
    metadata: SaveGameMetadata,
    onLoad: () -> Unit,
    onExport: () -> Unit
) {
    val formattedDate = remember {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(metadata.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onLoad() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF004400))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metadata.filename,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (metadata.tag.isNotBlank()) {
                    Text(
                        text = "Etiqueta: ${metadata.tag}",
                        color = Color.Yellow,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "Modo: ${metadata.gameMode}",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
                Text(
                    text = "P1: ${metadata.player1Score} | Dealer: ${metadata.dealerScore}",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
                Text(
                    text = "Guardado: $formattedDate",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }

            // Botón de Exportar
            IconButton(onClick = onExport) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Exportar Partida",
                    tint = Color.White
                )
            }
        }
    }
}
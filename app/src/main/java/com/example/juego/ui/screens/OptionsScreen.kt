// En: com/example/juego/ui/screens/OptionsScreen.kt
package com.example.juego.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.juego.ui.utils.SaveFormat
import com.example.juego.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit
) {
    val preferredFormat by viewModel.preferredFormatFlow.collectAsState(initial = SaveFormat.JSON)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Opciones") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF003300),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF003300)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Formato de Guardado Preferido",
                fontSize = 20.sp,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Crea un RadioButton por cada formato
            SaveFormat.values().forEach { format ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (preferredFormat == format),
                            onClick = { viewModel.setPreferredSaveFormat(format) }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (preferredFormat == format),
                        onClick = { viewModel.setPreferredSaveFormat(format) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color.White,
                            unselectedColor = Color.Gray
                        )
                    )
                    Text(
                        text = format.name,
                        color = Color.White,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                Text("Volver")
            }
        }
    }
}
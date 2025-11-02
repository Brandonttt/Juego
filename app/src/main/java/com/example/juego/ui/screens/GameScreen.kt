package com.example.juego.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.juego.R
import com.example.juego.model.Card
import com.example.juego.model.Suit
import com.example.juego.ui.utils.SoundManager
import com.example.juego.viewmodel.GameResult
import com.example.juego.viewmodel.GameState
import com.example.juego.viewmodel.GameStatus
import com.example.juego.viewmodel.GameViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton


val tableColor = Color(0xFF006400) // Verde oscuro

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    isTwoPlayer: Boolean, // ¡NUEVO!
    isLoadedGame: Boolean,
    onNavigateBack: () -> Unit // ¡NUEVO!
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    // 1. Inicia el juego con el modo correcto
    LaunchedEffect(key1 = isTwoPlayer, key2 = isLoadedGame) {
        if (!isLoadedGame) {
            // Si NO es una partida cargada, inicia una nueva
            Log.d("GameScreen", "Iniciando NUEVO juego, es 2 jugadores: $isTwoPlayer")
            viewModel.initGame(isTwoPlayer)
        } else {
            // Si ES una partida cargada, solo informa.
            // El ViewModel ya tiene el estado.
            Log.d("GameScreen", "Restaurando partida cargada.")
        }
    }

    val state by viewModel.gameState.collectAsState()
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }

    // El oyente de sonido sigue igual
    LaunchedEffect(key1 = soundManager) {
        soundManager.loadSound(R.raw.card_deal)
        soundManager.loadSound(R.raw.win)
        soundManager.loadSound(R.raw.bust)
        viewModel.soundEffect.collect { soundResourceId ->
            soundManager.playSound(soundResourceId)
        }
    }
    DisposableEffect(key1 = soundManager) { onDispose { soundManager.release() } }
    if (showSaveDialog) {
        SaveGameDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { filename, tag ->
                viewModel.saveGame(filename, tag)
                showSaveDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tableColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- MANO DEL DEALER ---
        HandView(
            title = "Dealer",
            cards = state.dealerHand,
            score = state.dealerScore,
            isDealerHand = true,
            gameStatus = state.gameStatus
        )

        // --- MENSAJE DE ESTADO DEL JUEGO ---
        GameStatusMessage(state = state)

        // --- MANOS DE JUGADORES ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- JUGADOR 2 (si existe) ---
            if (state.isTwoPlayerMode) {
                HandView(
                    title = "Jugador 2",
                    cards = state.player2Hand,
                    score = state.player2Score,
                    isActive = state.gameStatus == GameStatus.PLAYER_2_TURN,
                    result = state.player2Result
                )
            }

            // --- JUGADOR 1 (siempre) ---
            HandView(
                title = "Jugador 1",
                cards = state.player1Hand,
                score = state.player1Score,
                isActive = state.gameStatus == GameStatus.PLAYER_1_TURN,
                result = state.player1Result
            )
        }

        // --- BOTONES DE CONTROL ---
        ControlButtons(
            status = state.gameStatus,
            onHit = { viewModel.onPlayerHit() },
            onStand = { viewModel.onPlayerStand() },
            onNewGame = { viewModel.startNewGame() },
            onGoToMenu = onNavigateBack, // ¡NUEVO!
            onSaveGame = { showSaveDialog = true }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveGameDialog(
    onDismiss: () -> Unit,
    onSave: (filename: String, tag: String) -> Unit
) {
    var filename by remember { mutableStateOf("partida1") }
    var tag by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.padding(16.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Guardar Partida", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    label = { Text("Nombre del archivo") }
                )
                Spacer(modifier = Modifier.height(8.dp)) // <-- Espacio

                // --- NUEVO CAMPO PARA ETIQUETA ---
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Etiqueta (ej. 'Victoria rápida')") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    TextButton(
                        onClick = { onSave(filename, tag) },
                        enabled = filename.isNotBlank()
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Composable
fun HandView(
    title: String,
    cards: List<Card>,
    score: Int,
    isDealerHand: Boolean = false,
    gameStatus: GameStatus? = null, // Solo para el dealer
    isActive: Boolean = false, // Para P1 y P2
    result: GameResult? = null // Para P1 y P2
) {
    // Borde brillante si es el turno del jugador
    val activeBorder = if (isActive) Modifier.border(2.dp, Color.Yellow, MaterialTheme.shapes.medium) else Modifier

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = activeBorder.padding(4.dp)
    ) {

        var titleText = "$title: $score"
        var titleColor = Color.White

        // Muestra el resultado final del jugador
        result?.let {
            when(it) {
                GameResult.WIN -> { titleText = "$title: ¡GANA!"; titleColor = Color.Green }
                GameResult.LOSS -> { titleText = "$title: PIERDE"; titleColor = Color.Red }
                GameResult.BUST -> { titleText = "$title: BUST ($score)"; titleColor = Color.Red }
                GameResult.PUSH -> { titleText = "$title: EMPATE"; titleColor = Color.Yellow }
                GameResult.PENDING -> {} // No hacer nada
            }
        }

        Text(
            text = titleText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(100.dp) // Altura fija para la mano
        ) {
            val isDealerTurn = gameStatus == GameStatus.DEALER_TURN || gameStatus == GameStatus.GAME_OVER

            if (isDealerHand && !isDealerTurn && cards.isNotEmpty()) {
                // Dealer con carta oculta
                CardView(card = cards[0], isHidden = true)
                cards.drop(1).forEach { card -> CardView(card = card) }
            } else {
                // Muestra todas las cartas
                cards.forEach { card ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
                        exit = fadeOut() + slideOutHorizontally()
                    ) {
                        CardView(card = card)
                    }
                }
            }
        }
    }
}

@Composable
fun GameStatusMessage(state: GameState) {
    val message = when (state.gameStatus) {
        GameStatus.PLAYER_1_TURN -> "Turno del Jugador 1"
        GameStatus.PLAYER_2_TURN -> "Turno del Jugador 2"
        GameStatus.DEALER_TURN -> "Turno del Dealer..."
        GameStatus.GAME_OVER -> "¡Juego Terminado!"
    }

    Text(
        text = message,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Yellow,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun ControlButtons(
    status: GameStatus,
    onHit: () -> Unit,
    onStand: () -> Unit,
    onNewGame: () -> Unit,
    onGoToMenu: () -> Unit,
    onSaveGame: () -> Unit
) {
    val isGameInProgress = status == GameStatus.PLAYER_1_TURN || status == GameStatus.PLAYER_2_TURN

    if (isGameInProgress) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onHit, enabled = isGameInProgress) { Text("Pedir") }
            Button(onClick = onStand, enabled = isGameInProgress) { Text("Plantarse") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // --- NUEVO BOTÓN DE GUARDAR ---
        Button(onClick = onSaveGame, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
            Text("Guardar Partida")
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = onNewGame) { Text("Jugar de Nuevo") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onGoToMenu) { Text("Volver al Menú") }
        }
    }
}

// CardView no necesita cambios
@Composable
fun CardView(card: Card, isHidden: Boolean = false) {
    val cardColor = if (card.suit == Suit.DIAMONDS || card.suit == Suit.HEARTS) Color.Red else Color.Black
    val text = if (isHidden) "???" else "${card.rank.symbol}${card.suit.symbol}"

    Box(
        modifier = Modifier
            .size(width = 70.dp, height = 100.dp)
            .background(if (isHidden) Color.Blue else Color.White, shape = MaterialTheme.shapes.medium)
            .border(1.dp, Color.Black, shape = MaterialTheme.shapes.medium)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isHidden) Color.White else cardColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
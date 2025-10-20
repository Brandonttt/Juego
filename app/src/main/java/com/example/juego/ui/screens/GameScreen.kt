package com.example.juego.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.juego.model.Card
import com.example.juego.model.Suit
import com.example.juego.viewmodel.GameStatus
import com.example.juego.viewmodel.GameViewModel

// Color de la mesa
val tableColor = Color(0xFF006400) // Verde oscuro

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state by viewModel.gameState.collectAsState()
    val isPlayerTurn = state.gameStatus == GameStatus.PLAYER_TURN

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tableColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        // --- MANO DEL DEALER ---
        HandView(
            title = "Dealer",
            cards = state.dealerHand,
            score = state.dealerScore,
            isDealerHand = true,
            isPlayerTurn = isPlayerTurn
        )

        // --- MENSAJE DE ESTADO ---
        GameStatusMessage(status = state.gameStatus)

        // --- MANO DEL JUGADOR ---
        HandView(
            title = "Jugador",
            cards = state.playerHand,
            score = state.playerScore
        )

        // --- BOTONES DE CONTROL ---
        ControlButtons(
            status = state.gameStatus,
            onHit = { viewModel.onPlayerHit() },
            onStand = { viewModel.onPlayerStand() },
            onNewGame = { viewModel.startNewGame() }
        )
    }
}

@Composable
fun HandView(
    title: String,
    cards: List<Card>,
    score: Int,
    isDealerHand: Boolean = false,
    isPlayerTurn: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$title: $score",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // La primera carta del dealer está oculta durante el turno del jugador
            if (isDealerHand && isPlayerTurn && cards.isNotEmpty()) {
                CardView(card = cards[0], isHidden = true)
                // Muestra el resto de las cartas (solo la segunda al inicio)
                cards.drop(1).forEach { card ->
                    CardView(card = card)
                }
            } else {
                // Muestra todas las cartas normalmente
                cards.forEach { card ->
                    CardView(card = card)
                }
            }
        }
    }
}

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

@Composable
fun GameStatusMessage(status: GameStatus) {
    val message = when (status) {
        GameStatus.PLAYER_TURN -> "Tu turno"
        GameStatus.DEALER_TURN -> "Turno del Dealer..."
        GameStatus.PLAYER_WINS -> "¡Ganaste!"
        GameStatus.DEALER_WINS -> "Gana el Dealer"
        GameStatus.PLAYER_BUSTS -> "¡Te pasaste! Gana el Dealer"
        GameStatus.DEALER_BUSTS -> "¡Dealer se pasó! ¡Ganaste!"
        GameStatus.PUSH -> "Empate"
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
    onNewGame: () -> Unit
) {
    val isGameInProgress = status == GameStatus.PLAYER_TURN

    if (isGameInProgress) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onHit, enabled = isGameInProgress) {
                Text("Pedir")
            }
            Button(onClick = onStand, enabled = isGameInProgress) {
                Text("Plantarse")
            }
        }
    } else {
        Button(onClick = onNewGame) {
            Text("Jugar de Nuevo")
        }
    }
}
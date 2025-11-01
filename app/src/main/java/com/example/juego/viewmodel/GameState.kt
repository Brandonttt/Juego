// En: com/example/juego/viewmodel/GameState.kt
package com.example.juego.viewmodel

import com.example.juego.model.Card
import kotlinx.serialization.Serializable // <-- ¡IMPORTANTE!

@Serializable // <-- ¡IMPORTANTE!
data class GameState(
    // Banderas de modo
    val isTwoPlayerMode: Boolean = false,

    // Manos
    val player1Hand: List<Card> = emptyList(),
    val player2Hand: List<Card> = emptyList(),
    val dealerHand: List<Card> = emptyList(),

    // Puntuaciones
    val player1Score: Int = 0,
    val player2Score: Int = 0,
    val dealerScore: Int = 0,

    // Lógica de turnos y resultados
    val gameStatus: GameStatus = GameStatus.PLAYER_1_TURN,
    val player1Result: GameResult = GameResult.PENDING,
    val player2Result: GameResult = GameResult.PENDING,

    // --- NUEVOS CAMPOS PARA GUARDAR ---
    val timeElapsed: Long = 0, // En segundos
    val moveHistory: List<String> = emptyList()
)
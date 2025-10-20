package com.example.juego.viewmodel

import com.example.juego.model.Card

data class GameState(
    val playerHand: List<Card> = emptyList(),
    val dealerHand: List<Card> = emptyList(),
    val playerScore: Int = 0,
    val dealerScore: Int = 0, // Durante el turno del jugador, esta será la puntuación de la carta visible
    val gameStatus: GameStatus = GameStatus.PLAYER_TURN
)
package com.example.juego.viewmodel

enum class GameStatus {
    PLAYER_TURN,
    DEALER_TURN,
    PLAYER_WINS,
    DEALER_WINS,
    PLAYER_BUSTS,
    DEALER_BUSTS,
    PUSH // Empate
}
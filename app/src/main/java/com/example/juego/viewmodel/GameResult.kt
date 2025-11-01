package com.example.juego.viewmodel

enum class GameResult {
    PENDING, // El jugador aún no ha jugado o busteado
    WIN,     // Gana al Dealer
    LOSS,    // Pierde contra el Dealer
    BUST,    // Se pasó de 21
    PUSH     // Empate con el Dealer
}
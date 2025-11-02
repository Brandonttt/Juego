// En: com/example/juego/viewmodel/SaveGameMetadata.kt
package com.example.juego.viewmodel

import kotlinx.serialization.Serializable

@Serializable
data class SaveGameMetadata(
    val filename: String,    // ej: "partida1.json"
    val timestamp: Long,     // La hora en que se guardó
    val gameMode: String,    // "1 Jugador" o "2 Jugadores"
    val tag: String,         // Etiqueta personalizada
    val player1Score: Int,   // Puntuación de P1 al guardar
    val dealerScore: Int     // Puntuación del Dealer al guardar
)
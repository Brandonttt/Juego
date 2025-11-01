package com.example.juego.model
import kotlinx.serialization.Serializable // <-- AÑADE ESTA LÍNEA

@Serializable
data class Card(
    val rank: Rank,
    val suit: Suit
)
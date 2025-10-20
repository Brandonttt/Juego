package com.example.juego.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juego.model.Card
import com.example.juego.model.Deck
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState = _gameState.asStateFlow()

    private val deck = Deck()
    private lateinit var internalDealerHand: MutableList<Card> // Mano real del dealer
    private lateinit var internalPlayerHand: MutableList<Card> // Mano real del jugador

    init {
        startNewGame()
    }

    fun startNewGame() {
        deck.shuffle()
        internalPlayerHand = deck.drawHand()
        internalDealerHand = deck.drawHand()

        _gameState.value = GameState(
            playerHand = internalPlayerHand,
            dealerHand = internalDealerHand, // Al inicio mostramos ambas
            playerScore = calculateHandValue(internalPlayerHand),
            dealerScore = internalDealerHand[1].rank.value, // Puntuación de la CARTA VISIBLE
            gameStatus = GameStatus.PLAYER_TURN
        )
    }

    fun onPlayerHit() {
        if (_gameState.value.gameStatus != GameStatus.PLAYER_TURN) return

        internalPlayerHand.add(deck.drawCard())
        val newScore = calculateHandValue(internalPlayerHand)

        _gameState.update {
            it.copy(
                playerHand = internalPlayerHand,
                playerScore = newScore
            )
        }

        if (newScore > 21) {
            _gameState.update { it.copy(gameStatus = GameStatus.PLAYER_BUSTS) }
        }
    }

    fun onPlayerStand() {
        if (_gameState.value.gameStatus != GameStatus.PLAYER_TURN) return

        // Es el turno del dealer
        _gameState.update { it.copy(
            gameStatus = GameStatus.DEALER_TURN,
            dealerScore = calculateHandValue(internalDealerHand) // Revela la puntuación real
        )}

        // Lanza una corrutina para manejar el turno del dealer
        viewModelScope.launch {
            runDealerTurn()
        }
    }

    private suspend fun runDealerTurn() {
        var dealerScore = calculateHandValue(internalDealerHand)

        // El dealer pide carta mientras tenga menos de 17
        while (dealerScore < 17) {
            delay(1000) // Pausa para simular que "piensa"
            internalDealerHand.add(deck.drawCard())
            dealerScore = calculateHandValue(internalDealerHand)

            _gameState.update {
                it.copy(
                    dealerHand = internalDealerHand,
                    dealerScore = dealerScore
                )
            }
        }

        // Espera un segundo más y determina el ganador
        delay(1000)
        determineWinner(dealerScore)
    }

    private fun determineWinner(dealerScore: Int) {
        val playerScore = _gameState.value.playerScore

        val finalStatus = when {
            dealerScore > 21 -> GameStatus.DEALER_BUSTS
            playerScore > dealerScore -> GameStatus.PLAYER_WINS
            dealerScore > playerScore -> GameStatus.DEALER_WINS
            else -> GameStatus.PUSH // Empate
        }

        _gameState.update { it.copy(gameStatus = finalStatus) }
    }

    // La lógica clave del Blackjack: calcular el valor de la mano
    private fun calculateHandValue(hand: List<Card>): Int {
        var total = 0
        var aces = 0
        for (card in hand) {
            total += card.rank.value
            if (card.rank.name == "ACE") {
                aces++
            }
        }
        // Maneja los Ases: si el total es > 21, un As pasa a valer 1
        while (total > 21 && aces > 0) {
            total -= 10
            aces--
        }
        return total
    }
}
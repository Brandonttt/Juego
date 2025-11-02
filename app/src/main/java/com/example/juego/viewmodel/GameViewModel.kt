package com.example.juego.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel // <-- CAMBIO IMPORTANTE
import androidx.lifecycle.viewModelScope
import com.example.juego.R
import com.example.juego.model.Card
import com.example.juego.model.Deck
import com.example.juego.ui.utils.SaveFormat
import com.example.juego.ui.utils.SaveLoadManager // <-- NUEVO
import com.example.juego.ui.utils.SettingsManager // <-- NUEVO
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.net.Uri // <-- NUEVO
import com.example.juego.viewmodel.SaveGameMetadata // <-- NUEVO
import kotlinx.coroutines.flow.StateFlow

class GameViewModel(application: Application) : AndroidViewModel(application) { // <-- CAMBIO

    private val _gameState = MutableStateFlow(GameState())
    val gameState = _gameState.asStateFlow()

    private val _soundEffect = Channel<Int>(Channel.BUFFERED)
    val soundEffect = _soundEffect.receiveAsFlow()

    private val deck = Deck()
    private lateinit var internalPlayer1Hand: MutableList<Card>
    private lateinit var internalPlayer2Hand: MutableList<Card>
    private lateinit var internalDealerHand: MutableList<Card>

    // --- NUEVA LÓGICA DE GUARDADO Y TIEMPO ---
    private val settingsManager = SettingsManager(application)
    private val saveLoadManager = SaveLoadManager(application)
    private var timerJob: Job? = null

    // Expone el flujo de preferencias a la UI (para la pantalla de Opciones)
    val preferredFormatFlow = settingsManager.preferredFormatFlow

    // --- FIN DE NUEVA LÓGICA ---

    private var isTwoPlayerMode = false

    init {
        // Al iniciar, no iniciamos el juego, esperamos a la UI
    }

    fun initGame(isTwoPlayer: Boolean) {
        isTwoPlayerMode = isTwoPlayer
        startNewGame()
    }

    private fun playSound(soundId: Int) {
        viewModelScope.launch {
            _soundEffect.send(soundId)
        }
    }

    fun startNewGame() {
        // Cancela el timer anterior si existe
        timerJob?.cancel()

        deck.shuffle()
        internalPlayer1Hand = deck.drawHand()
        internalDealerHand = deck.drawHand()

        var initialState = GameState(
            isTwoPlayerMode = isTwoPlayerMode,
            player1Hand = internalPlayer1Hand,
            player1Score = calculateHandValue(internalPlayer1Hand),
            dealerHand = internalDealerHand,
            dealerScore = internalDealerHand[1].rank.value,
            gameStatus = GameStatus.PLAYER_1_TURN,
            player1Result = GameResult.PENDING,
            timeElapsed = 0, // Resetea el tiempo
            moveHistory = listOf("NUEVA_PARTIDA") // Resetea el historial
        )

        if (isTwoPlayerMode) {
            internalPlayer2Hand = deck.drawHand()
            initialState = initialState.copy(
                player2Hand = internalPlayer2Hand,
                player2Score = calculateHandValue(internalPlayer2Hand),
                player2Result = GameResult.PENDING
            )
        }

        _gameState.value = initialState
        startTimer() // Inicia el nuevo timer
    }

    private fun startTimer() {
        timerJob?.cancel() // Asegura que solo haya un timer
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _gameState.update { it.copy(timeElapsed = it.timeElapsed + 1) }
            }
        }
    }
    fun importGame(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val loadedState = saveLoadManager.loadGameFromUri(uri)
            if (loadedState != null) {
                Log.d("GameViewModel", "Partida importada exitosamente desde URI")
                _gameState.value = loadedState

                // Restaura las manos internas (copiado de loadGame)
                isTwoPlayerMode = loadedState.isTwoPlayerMode
                internalPlayer1Hand = loadedState.player1Hand.toMutableList()
                internalDealerHand = loadedState.dealerHand.toMutableList()
                if(isTwoPlayerMode) {
                    internalPlayer2Hand = loadedState.player2Hand.toMutableList()
                }

                startTimer() // Reinicia el timer
                onResult(true) // Éxito
            } else {
                Log.e("GameViewModel", "Error al importar desde URI")
                onResult(false) // Fracaso
            }
        }
    }

    // --- NUEVAS FUNCIONES DE GUARDAR/CARGAR ---

    fun saveGame(filename: String, tag: String) { // <-- AHORA RECIBE EL TAG
        viewModelScope.launch {
            val format = settingsManager.preferredFormatFlow.first()
            // Añade la etiqueta al estado antes de guardar
            val stateToSave = _gameState.value.copy(tag = tag)

            val success = saveLoadManager.saveGame(stateToSave, filename, format)
            if (success) {
                Log.d("GameViewModel", "Partida guardada $filename.${format.extension}")
            } else {
                Log.e("GameViewModel", "Error al guardar la partida")
            }
        }
    }

    fun loadGame(filename: String) {
        viewModelScope.launch {
            val loadedState = saveLoadManager.loadGame(filename)
            if (loadedState != null) {
                Log.d("GameViewModel", "Partida cargada desde $filename")
                _gameState.value = loadedState

                // Reinicia las manos internas (importante)
                isTwoPlayerMode = loadedState.isTwoPlayerMode
                internalPlayer1Hand = loadedState.player1Hand.toMutableList()
                internalDealerHand = loadedState.dealerHand.toMutableList()
                if(isTwoPlayerMode) {
                    internalPlayer2Hand = loadedState.player2Hand.toMutableList()
                }

                // Reinicia el timer
                startTimer()
            } else {
                Log.e("GameViewModel", "Error al cargar la partida $filename")
            }
        }
    }

    // Expone la lista de partidas guardadas a la UI
    fun getSavedGamesMetadata(): List<SaveGameMetadata> {
        return saveLoadManager.getSavedGamesMetadata()
    }
    fun exportGame(filename: String, targetUri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val content = saveLoadManager.getSavedGameContent(filename)
            if (content == null) {
                Log.e("GameViewModel", "No se pudo leer el contenido de $filename para exportar")
                onResult(false)
                return@launch
            }
            val success = saveLoadManager.exportGame(content, targetUri)
            onResult(success)
        }
    }

    // Expone la función de guardar preferencias
    fun setPreferredSaveFormat(format: SaveFormat) {
        viewModelScope.launch {
            settingsManager.setPreferredFormat(format)
        }
    }

    // --- FIN DE NUEVAS FUNCIONES ---

    fun onPlayerHit() {
        val currentState = _gameState.value

        when (currentState.gameStatus) {
            GameStatus.PLAYER_1_TURN -> {
                Log.d("GameViewModel", "¡onPlayerHit() P1!")
                playSound(R.raw.card_deal)
                internalPlayer1Hand.add(deck.drawCard())
                val newScore = calculateHandValue(internalPlayer1Hand)

                _gameState.update { it.copy(
                    player1Hand = internalPlayer1Hand,
                    player1Score = newScore,
                    moveHistory = it.moveHistory + "P1_HIT" // Añade al historial
                )}

                if (newScore > 21) {
                    viewModelScope.launch {
                        Log.d("GameViewModel", "¡P1 SE PASÓ (BUST)!")
                        playSound(R.raw.bust)
                        delay(100)
                        _gameState.update { it.copy(
                            player1Result = GameResult.BUST,
                            moveHistory = it.moveHistory + "P1_BUST"
                        )}
                        onPlayerStand()
                    }
                }
            }
            GameStatus.PLAYER_2_TURN -> {
                Log.d("GameViewModel", "¡onPlayerHit() P2!")
                playSound(R.raw.card_deal)
                internalPlayer2Hand.add(deck.drawCard())
                val newScore = calculateHandValue(internalPlayer2Hand)

                _gameState.update { it.copy(
                    player2Hand = internalPlayer2Hand,
                    player2Score = newScore,
                    moveHistory = it.moveHistory + "P2_HIT"
                )}

                if (newScore > 21) {
                    viewModelScope.launch {
                        Log.d("GameViewModel", "¡P2 SE PASÓ (BUST)!")
                        playSound(R.raw.bust)
                        delay(100)
                        _gameState.update { it.copy(
                            player2Result = GameResult.BUST,
                            moveHistory = it.moveHistory + "P2_BUST"
                        )}
                        onPlayerStand()
                    }
                }
            }
            else -> {}
        }
    }

    fun onPlayerStand() {
        val currentState = _gameState.value

        when (currentState.gameStatus) {
            GameStatus.PLAYER_1_TURN -> {
                _gameState.update { it.copy(moveHistory = it.moveHistory + "P1_STAND") }
                if (isTwoPlayerMode && currentState.player2Result == GameResult.PENDING) {
                    _gameState.update { it.copy(gameStatus = GameStatus.PLAYER_2_TURN) }
                } else {
                    startDealerTurn()
                }
            }
            GameStatus.PLAYER_2_TURN -> {
                _gameState.update { it.copy(moveHistory = it.moveHistory + "P2_STAND") }
                startDealerTurn()
            }
            else -> { }
        }
    }

    // ... el resto de funciones (startDealerTurn, runDealerTurn, determineWinner, calculateResult, calculateHandValue)
    // no necesitan cambios, ya que están completas desde la vez anterior.
    // Solo me aseguraré de que estén aquí:

    private fun startDealerTurn() {
        _gameState.update { it.copy(
            gameStatus = GameStatus.DEALER_TURN,
            dealerScore = calculateHandValue(internalDealerHand),
            moveHistory = it.moveHistory + "DEALER_TURN"
        )}
        viewModelScope.launch {
            runDealerTurn()
        }
    }

    private suspend fun runDealerTurn() {
        var dealerScore = calculateHandValue(internalDealerHand)

        while (dealerScore < 17) {
            delay(1000)
            _soundEffect.send(R.raw.card_deal)
            internalDealerHand.add(deck.drawCard())
            dealerScore = calculateHandValue(internalDealerHand)
            _gameState.update { it.copy(
                dealerHand = internalDealerHand,
                dealerScore = dealerScore,
                moveHistory = it.moveHistory + "DEALER_HIT"
            )}
        }

        _gameState.update { it.copy(moveHistory = it.moveHistory + "DEALER_STAND") }
        delay(1000)
        determineWinner(dealerScore)
    }

    private fun determineWinner(dealerScore: Int) {
        val currentState = _gameState.value
        val dealerBusted = dealerScore > 21

        val p1Result = calculateResult(
            currentState.player1Score,
            currentState.player1Result,
            dealerScore,
            dealerBusted
        )

        var p2Result = currentState.player2Result
        if (isTwoPlayerMode) {
            p2Result = calculateResult(
                currentState.player2Score,
                currentState.player2Result,
                dealerScore,
                dealerBusted
            )
        }

        if (p1Result == GameResult.WIN || p2Result == GameResult.WIN) {
            playSound(R.raw.win)
        }
        else if (p1Result == GameResult.LOSS || p2Result == GameResult.LOSS) {
            playSound(R.raw.bust)
        }

        _gameState.update { it.copy(
            player1Result = p1Result,
            player2Result = p2Result,
            gameStatus = GameStatus.GAME_OVER,
            moveHistory = it.moveHistory + "GAME_OVER"
        )}
        // Detiene el timer al final del juego
        timerJob?.cancel()
    }

    private fun calculateResult(
        playerScore: Int,
        currentResult: GameResult,
        dealerScore: Int,
        dealerBusted: Boolean
    ): GameResult {
        if (currentResult == GameResult.BUST) return GameResult.BUST
        if (dealerBusted) return GameResult.WIN
        return when {
            playerScore > dealerScore -> GameResult.WIN
            playerScore < dealerScore -> GameResult.LOSS
            else -> GameResult.PUSH
        }
    }

    private fun calculateHandValue(hand: List<Card>): Int {
        var total = 0
        var aces = 0
        for (card in hand) {
            total += card.rank.value
            if (card.rank.name == "ACE") {
                aces++
            }
        }
        while (total > 21 && aces > 0) {
            total -= 10
            aces--
        }
        return total
    }
}
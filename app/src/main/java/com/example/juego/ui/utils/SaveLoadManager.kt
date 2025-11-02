// En: com/example/juego/ui/utils/SaveLoadManager.kt
package com.example.juego.ui.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Xml
import com.example.juego.model.Card
import com.example.juego.model.Rank
import com.example.juego.model.Suit
import com.example.juego.viewmodel.GameResult
import com.example.juego.viewmodel.GameStatus
import com.example.juego.viewmodel.GameState
import com.example.juego.viewmodel.SaveGameMetadata // <-- NUEVO IMPORT
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.File
import java.io.StringReader
import java.io.StringWriter

import java.io.BufferedReader
import java.io.InputStreamReader


class SaveLoadManager(private val context: Context) {

    private val TAG = "SaveLoadManager"
    private val FOLDER_NAME = "saved_games"
    private val METADATA_FILENAME = "metadata.json" // <-- NUEVO
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true } // <-- ACTUALIZADO

    // --- MANEJO DE METADATOS (NUEVO) ---

    private fun getMetadataFile(): File {
        val directory = File(context.filesDir, FOLDER_NAME)
        if (!directory.exists()) directory.mkdirs()
        return File(directory, METADATA_FILENAME)
    }

    private fun readMetadata(): MutableList<SaveGameMetadata> {
        val file = getMetadataFile()
        if (!file.exists()) return mutableListOf()
        return try {
            val content = file.readText()
            json.decodeFromString(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error al leer metadata.json", e)
            mutableListOf()
        }
    }

    private fun writeMetadata(metadataList: List<SaveGameMetadata>) {
        try {
            val file = getMetadataFile()
            val content = json.encodeToString(metadataList)
            file.writeText(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error al escribir metadata.json", e)
        }
    }

    /**
     * Esta es la lista que verá la UI de "Cargar Partida"
     */
    fun getSavedGamesMetadata(): List<SaveGameMetadata> {
        return readMetadata().sortedByDescending { it.timestamp }
    }

    // --- LÓGICA DE GUARDADO (ACTUALIZADA) ---

    fun saveGame(state: GameState, filename: String, format: SaveFormat): Boolean {
        val fullFilename = "$filename${format.extension}"
        val fileContent = when (format) {
            SaveFormat.JSON -> serializeToJson(state)
            SaveFormat.XML -> serializeToXml(state)
            SaveFormat.TXT -> serializeToTxt(state)
        }

        val success = try {
            val directory = File(context.filesDir, FOLDER_NAME)
            if (!directory.exists()) directory.mkdirs()
            File(directory, fullFilename).writeText(fileContent)
            Log.d(TAG, "Guardado exitoso en $fullFilename")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar ${format.name}", e)
            false
        }

        // Si se guardó bien, actualiza los metadatos
        if (success) {
            updateMetadata(state, fullFilename)
        }
        return success
    }

    private fun updateMetadata(state: GameState, filename: String) {
        val metadataList = readMetadata()
        // Borra la entrada vieja si existe
        metadataList.removeAll { it.filename == filename }

        // Crea la nueva entrada
        val newMetadata = SaveGameMetadata(
            filename = filename,
            timestamp = System.currentTimeMillis(),
            gameMode = if (state.isTwoPlayerMode) "2 Jugadores" else "1 Jugador",
            tag = state.tag,
            player1Score = state.player1Score,
            dealerScore = state.dealerScore
        )
        metadataList.add(newMetadata)
        writeMetadata(metadataList)
    }

    // --- LÓGICA DE EXPORTACIÓN (NUEVA) ---

    fun getSavedGameContent(filename: String): String? {
        val directory = File(context.filesDir, FOLDER_NAME)
        val file = File(directory, filename)
        if (!file.exists()) return null
        return file.readText()
    }

    fun exportGame(content: String, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al exportar archivo", e)
            false
        }
    }

    // --- LÓGICA DE CARGA (Sin cambios) ---
    fun loadGame(filename: String): GameState? {
        // ... (esta función no cambia)
        val directory = File(context.filesDir, FOLDER_NAME)
        val file = File(directory, filename)

        if (!file.exists()) {
            Log.e(TAG, "No se encontró el archivo: $filename")
            return null
        }

        val content = file.readText()

        return try {
            when (file.extension) {
                "json" -> deserializeFromJson(content)
                "xml" -> deserializeFromXml(content)
                "txt" -> deserializeFromTxt(content)
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar ${file.extension}", e)
            null
        }
    }
    fun loadGameFromUri(uri: Uri): GameState? {
        // 1. Lee el contenido del archivo desde el URI
        val content = try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al leer el URI", e)
            return null
        }

        if (content.isNullOrBlank()) return null

        // 2. Intenta adivinar el formato y parsearlo
        // (El orden de los 'try-catch' es importante)

        // Intenta JSON (el más estricto, empieza con {)
        if (content.trim().startsWith("{")) {
            try {
                val state = deserializeFromJson(content)
                Log.d(TAG, "Partida importada como JSON")
                return state
            } catch (e: Exception) { /* No era JSON, sigue intentando */ }
        }

        // Intenta XML (empieza con <)
        if (content.trim().startsWith("<")) {
            try {
                val state = deserializeFromXml(content)
                Log.d(TAG, "Partida importada como XML")
                return state
            } catch (e: Exception) { /* No era XML, sigue intentando */ }
        }

        // Intenta TXT (formato clave=valor)
        try {
            val state = deserializeFromTxt(content)
            Log.d(TAG, "Partida importada como TXT")
            return state
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo parsear el archivo importado como TXT", e)
        }

        // Si todos fallan
        Log.e(TAG, "El archivo importado no tiene un formato reconocido.")
        return null
    }

    // --- LÓGICA DE SERIALIZACIÓN (Sin cambios) ---
    private fun serializeToJson(state: GameState): String {
        return json.encodeToString(state)
    }
    private fun deserializeFromJson(data: String): GameState {
        return json.decodeFromString(data)
    }

    // ... (Tus funciones serializeToTxt, deserializeFromTxt, serializeToXml, etc.
    //      NO necesitan cambios)
    // ... (Pegar el resto de tu SaveLoadManager.kt aquí)
    private fun serializeToTxt(state: GameState): String {
        return buildString {
            append("isTwoPlayerMode=${state.isTwoPlayerMode}\n")
            append("gameStatus=${state.gameStatus.name}\n")
            append("timeElapsed=${state.timeElapsed}\n")
            append("player1Score=${state.player1Score}\n")
            append("player2Score=${state.player2Score}\n")
            append("dealerScore=${state.dealerScore}\n")
            append("player1Hand=${serializeHand(state.player1Hand)}\n")
            append("player2Hand=${serializeHand(state.player2Hand)}\n")
            append("dealerHand=${serializeHand(state.dealerHand)}\n")
            append("player1Result=${state.player1Result.name}\n")
            append("player2Result=${state.player2Result.name}\n")
            append("moveHistory=${state.moveHistory.joinToString(",")}\n")
            append("tag=${state.tag}\n") // <-- AÑADIDO
        }
    }
    private fun deserializeFromTxt(data: String): GameState {
        val map = data.lines().filter { it.contains("=") }.associate {
            val (key, value) = it.split("=", limit = 2)
            key to value
        }
        return GameState(
            isTwoPlayerMode = map["isTwoPlayerMode"]?.toBoolean() ?: false,
            gameStatus = GameStatus.valueOf(map["gameStatus"] ?: "PLAYER_1_TURN"),
            timeElapsed = map["timeElapsed"]?.toLong() ?: 0L,
            player1Score = map["player1Score"]?.toInt() ?: 0,
            player2Score = map["player2Score"]?.toInt() ?: 0,
            dealerScore = map["dealerScore"]?.toInt() ?: 0,
            player1Hand = deserializeHand(map["player1Hand"] ?: ""),
            player2Hand = deserializeHand(map["player2Hand"] ?: ""),
            dealerHand = deserializeHand(map["dealerHand"] ?: ""),
            player1Result = GameResult.valueOf(map["player1Result"] ?: "PENDING"),
            player2Result = GameResult.valueOf(map["player2Result"] ?: "PENDING"),
            moveHistory = map["moveHistory"]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
            tag = map["tag"] ?: "" // <-- AÑADIDO
        )
    }
    private fun serializeHand(hand: List<Card>): String = hand.joinToString(",") { "${it.rank.name}_${it.suit.name}" }
    private fun deserializeHand(data: String): List<Card> {
        if (data.isEmpty()) return emptyList()
        return data.split(",").map {
            val (rank, suit) = it.split("_")
            Card(Rank.valueOf(rank), Suit.valueOf(suit))
        }
    }

    private fun serializeToXml(state: GameState): String {
        val serializer: XmlSerializer = Xml.newSerializer()
        val writer = StringWriter()
        serializer.setOutput(writer)
        serializer.startDocument("UTF-8", true)
        serializer.startTag(null, "gameState")

        serializer.tag("isTwoPlayerMode", state.isTwoPlayerMode.toString())
        serializer.tag("gameStatus", state.gameStatus.name)
        serializer.tag("timeElapsed", state.timeElapsed.toString())
        serializer.tag("player1Score", state.player1Score.toString())
        serializer.tag("player2Score", state.player2Score.toString())
        serializer.tag("dealerScore", state.dealerScore.toString())
        serializer.tag("player1Result", state.player1Result.name)
        serializer.tag("player2Result", state.player2Result.name)
        serializer.tag("tag", state.tag) // <-- AÑADIDO

        serializer.handToXml("player1Hand", state.player1Hand)
        serializer.handToXml("player2Hand", state.player2Hand)
        serializer.handToXml("dealerHand", state.dealerHand)

        serializer.startTag(null, "moveHistory")
        state.moveHistory.forEach { serializer.tag("move", it) }
        serializer.endTag(null, "moveHistory")

        serializer.endTag(null, "gameState")
        serializer.endDocument()
        return writer.toString()
    }
    // Helper para XML
    private fun XmlSerializer.tag(name: String, text: String) {
        startTag(null, name)
        text(text)
        endTag(null, name)
    }
    // Helper para XML
    private fun XmlSerializer.handToXml(name: String, hand: List<Card>) {
        startTag(null, name)
        hand.forEach { card ->
            startTag(null, "card")
            tag("rank", card.rank.name)
            tag("suit", card.suit.name)
            endTag(null, "card")
        }
        endTag(null, name)
    }

    private fun deserializeFromXml(data: String): GameState {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(data))

        var eventType = parser.eventType
        var currentTag = ""

        var isTwoPlayerMode = false
        var gameStatus = GameStatus.PLAYER_1_TURN
        var timeElapsed = 0L
        var player1Score = 0
        var player2Score = 0
        var dealerScore = 0
        var player1Result = GameResult.PENDING
        var player2Result = GameResult.PENDING
        val player1Hand = mutableListOf<Card>()
        val player2Hand = mutableListOf<Card>()
        val dealerHand = mutableListOf<Card>()
        val moveHistory = mutableListOf<String>()
        var tag = "" // <-- AÑADIDO

        var currentHand: MutableList<Card>? = null
        var currentCardRank: Rank? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    when (currentTag) {
                        "player1Hand" -> currentHand = player1Hand
                        "player2Hand" -> currentHand = player2Hand
                        "dealerHand" -> currentHand = dealerHand
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text
                    when (currentTag) {
                        "isTwoPlayerMode" -> isTwoPlayerMode = text.toBoolean()
                        "gameStatus" -> gameStatus = GameStatus.valueOf(text)
                        "timeElapsed" -> timeElapsed = text.toLong()
                        "player1Score" -> player1Score = text.toIntOrNull() ?: 0
                        "player2Score" -> player2Score = text.toIntOrNull() ?: 0
                        "dealerScore" -> dealerScore = text.toIntOrNull() ?: 0
                        "player1Result" -> player1Result = GameResult.valueOf(text)
                        "player2Result" -> player2Result = GameResult.valueOf(text)
                        "move" -> moveHistory.add(text)
                        "tag" -> tag = text // <-- AÑADIDO
                        "rank" -> currentCardRank = Rank.valueOf(text)
                        "suit" -> {
                            if (currentCardRank != null) {
                                currentHand?.add(Card(currentCardRank, Suit.valueOf(text)))
                                currentCardRank = null
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when(parser.name) {
                        "player1Hand", "player2Hand", "dealerHand" -> currentHand = null
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        return GameState(
            isTwoPlayerMode, player1Hand, player2Hand, dealerHand,
            player1Score, player2Score, dealerScore,
            gameStatus, player1Result, player2Result, timeElapsed, moveHistory, tag // <-- AÑADIDO
        )
    }
}
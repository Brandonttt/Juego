package com.example.juego.ui.utils

import android.content.Context
import android.media.SoundPool
import androidx.annotation.RawRes
import android.util.Log
private const val TAG = "SoundManager"
class SoundManager(private val context: Context) {

    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<Int, Int>() // Mapa de <ResourceID, SoundID>

    // --- NUEVO: Un set para rastrear los sonidos que SÍ están cargados ---
    private val loadedSounds = mutableSetOf<Int>() // Almacena los ResourceID (ej. R.raw.win)

    init {
        // --- NUEVO: El "oyente" que nos avisa cuando un sonido está listo ---
        val listener = SoundPool.OnLoadCompleteListener { pool, sampleId, status ->
            if (status == 0) {
                // El status 0 significa que se cargó correctamente
                // Ahora, buscamos a qué ResourceID le pertenece este 'sampleId'
                val resId = soundMap.entries.find { it.value == sampleId }?.key
                resId?.let {
                    // ¡Lo marcamos como cargado!
                    loadedSounds.add(it)
                    Log.d(TAG, "¡CARGA COMPLETA! El sonido $resId está listo.")
                }
            }
            else {
                // --- AÑADE ESTE LOG ---
                Log.e(TAG, "Error al cargar el sonido, sampleId: $sampleId, status: $status")
            }
        }

        // Construimos el SoundPool y le asignamos el "oyente"
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .build()
        soundPool.setOnLoadCompleteListener(listener)
    }

    /**
     * Carga un sonido.
     */
    fun loadSound(@RawRes soundResId: Int) {
        // Evita cargarlo si ya está en el mapa
        if (soundMap.containsKey(soundResId)) return

        // El context se toma del constructor de la clase
        Log.d(TAG, "Cargando sonido $soundResId...")
        val soundId = soundPool.load(context, soundResId, 1)
        soundMap[soundResId] = soundId
    }

    /**
     * Reproduce un sonido (si está cargado).
     */
    fun playSound(@RawRes soundResId: Int) {
        // --- LA MAGIA ESTÁ AQUÍ ---
        // Solo reproducimos si nuestro 'loadedSounds' set contiene el ID
        if (loadedSounds.contains(soundResId)) {
            soundMap[soundResId]?.let { soundId ->
                Log.d(TAG, "¡SONANDO! El sonido $soundResId está cargado.")
                soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
            }
        }
        else {
            // --- AÑADE ESTE LOG ---
            Log.w(TAG, "ADVERTENCIA: El sonido $soundResId AÚN NO ESTÁ CARGADO.")
        }
        // Si no, no hacemos nada (porque aún no se carga)
    }

    /**
     * Libera los recursos.
     */
    fun release() {
        soundPool.release()
    }
}
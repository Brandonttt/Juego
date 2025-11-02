package com.example.juego

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.juego.ui.screens.GameScreen
import com.example.juego.ui.screens.MenuScreen
import com.example.juego.ui.theme.JuegoTheme
import com.example.juego.viewmodel.GameViewModel
import com.example.juego.ui.screens.LoadGameScreen // <-- NUEVO
import com.example.juego.ui.screens.OptionsScreen

class MainActivity : ComponentActivity() {

    // El ViewModel se comparte entre pantallas
    private val gameViewModel by viewModels<GameViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JuegoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Configura el controlador de navegación
                    AppNavigation(viewModel = gameViewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: GameViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "menu") {

        // Ruta 1: Pantalla de Menú
        composable("menu") {
            MenuScreen(
                onOnePlayerClick = { navController.navigate("game/false") },
                onTwoPlayerClick = { navController.navigate("game/true") },
                // --- NUEVAS NAVEGACIONES ---
                onLoadGameClick = { navController.navigate("load") },
                onOptionsClick = { navController.navigate("options") }
            )
        }

        // Ruta 2: Pantalla de Juego
        composable(
            route = "game/{isTwoPlayer}", // Recibe (false, true, o "loaded")
            arguments = listOf(navArgument("isTwoPlayer") { type = NavType.StringType })
        ) { backStackEntry ->

            val isTwoPlayerArg = backStackEntry.arguments?.getString("isTwoPlayer")

            // 1. Calcula si es una partida cargada
            val isLoadedGame = (isTwoPlayerArg == "loaded")

            // 2. Determina el modo de juego
            val isTwoPlayerBool = if (isLoadedGame) {
                // Si se carga, toma el valor que ya está en el ViewModel
                viewModel.gameState.value.isTwoPlayerMode
            } else {
                // Si es nueva, toma el valor de la navegación
                isTwoPlayerArg.toBoolean()
            }

            // 3. Llama a GameScreen UNA SOLA VEZ, pasando el nuevo parámetro
            GameScreen(
                viewModel = viewModel,
                isTwoPlayer = isTwoPlayerBool,
                isLoadedGame = isLoadedGame, // <-- ¡Aquí está el parámetro que faltaba!
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- NUEVA RUTA 3: Opciones ---
        composable("options") {
            OptionsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- NUEVA RUTA 4: Cargar Partida ---
        composable("load") {
            LoadGameScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onGameLoaded = {
                    // Navega a la pantalla de juego, pero sin argumentos
                    // El ViewModel ya tiene el estado cargado
                    // Necesitamos una ruta que no fuerce el "isTwoPlayer"
                    // Vamos a modificar la ruta "game"
                    navController.navigate("game/loaded") {
                        // Limpia el stack para que no puedas "volver" a la pantalla de carga
                        popUpTo("menu")
                    }
                }
            )
        }

        // --- Ruta 2 (Modificada) para aceptar partidas cargadas ---
        // (Borra la "Ruta 2" anterior y reemplázala con esta)
        composable(
            route = "game/{isTwoPlayer}", // Recibe (false, true, o "loaded")
            arguments = listOf(navArgument("isTwoPlayer") { type = NavType.StringType })
        ) { backStackEntry ->

            val isTwoPlayerArg = backStackEntry.arguments?.getString("isTwoPlayer")
            val isLoadedGame = (isTwoPlayerArg == "loaded")

            // Determina el modo de juego
            val isTwoPlayerBool = if (isLoadedGame) {
                // Si se carga, toma el valor del ViewModel
                viewModel.gameState.value.isTwoPlayerMode
            } else {
                // Si es nueva, toma el valor de la navegación
                isTwoPlayerArg.toBoolean()
            }

            GameScreen(
                viewModel = viewModel,
                isTwoPlayer = isTwoPlayerBool,
                isLoadedGame = isLoadedGame, // <-- ¡NUEVO PARÁMETRO AÑADIDO!
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
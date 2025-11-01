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
            route = "game/{isTwoPlayer}",
            arguments = listOf(navArgument("isTwoPlayer") { type = NavType.BoolType })
        ) { backStackEntry ->
            val isTwoPlayer = backStackEntry.arguments?.getBoolean("isTwoPlayer") ?: false
            GameScreen(
                viewModel = viewModel,
                isTwoPlayer = isTwoPlayer,
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
            route = "game/{isTwoPlayer}", // Recibe el argumento (false, true, o "loaded")
            arguments = listOf(navArgument("isTwoPlayer") { type = NavType.StringType })
        ) { backStackEntry ->

            val isTwoPlayerArg = backStackEntry.arguments?.getString("isTwoPlayer")

            // Si no es "loaded", es una partida nueva
            if (isTwoPlayerArg != "loaded") {
                val isTwoPlayer = isTwoPlayerArg.toBoolean()
                GameScreen(
                    viewModel = viewModel,
                    isTwoPlayer = isTwoPlayer,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                // Es una partida cargada, el ViewModel ya tiene el estado
                // El 'isTwoPlayer' es solo para satisfacer el Composable
                GameScreen(
                    viewModel = viewModel,
                    isTwoPlayer = viewModel.gameState.value.isTwoPlayerMode, // Usa el estado cargado
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
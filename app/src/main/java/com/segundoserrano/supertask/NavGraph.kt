package com.segundoserrano.supertask

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.segundoserrano.supertask.ui.*
import com.segundoserrano.supertask.ui.components.BottomNavBar
import com.segundoserrano.supertask.viewmodel.NoteViewModel
import com.segundoserrano.supertask.viewmodel.SettingsViewModel
import com.segundoserrano.supertask.viewmodel.TaskViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Calendar : Screen("calendar")
    object Groups : Screen("groups")
    object Notes : Screen("notes")
    object Settings : Screen("settings")
    object NewTask : Screen("new_task?taskId={taskId}") {
        fun createRoute(taskId: Long? = null): String {
            return if (taskId != null) "new_task?taskId=$taskId" else "new_task"
        }
    }
    object NoteDetail : Screen("note_detail?noteId={noteId}") {
        fun createRoute(noteId: Long? = null): String {
            return if (noteId != null) "note_detail?noteId=$noteId" else "note_detail"
        }
    }
}

@Composable
fun SuperTaskNavigation(
    taskViewModel: TaskViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    noteViewModel: NoteViewModel = viewModel()
) {
    val navController = rememberNavController()
    val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()

    com.segundoserrano.supertask.ui.theme.SuperTaskTheme(darkTheme = isDarkTheme) {
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route
        ) {
            // Splash Screen
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // Main Screens con Bottom Navigation
            composable(Screen.Home.route) {
                MainScaffold(
                    navController = navController,
                    currentRoute = Screen.Home.route,
                    taskViewModel = taskViewModel
                ) {
                    HomeScreen(
                        viewModel = taskViewModel,
                        onNavigateToNewTask = { taskId ->
                            navController.navigate(Screen.NewTask.createRoute(taskId))
                        }
                    )
                }
            }

            composable(Screen.Calendar.route) {
                MainScaffold(
                    navController = navController,
                    currentRoute = Screen.Calendar.route,
                    taskViewModel = taskViewModel
                ) {
                    CalendarScreen(
                        viewModel = taskViewModel,
                        onNavigateToNewTask = { taskId ->
                            navController.navigate(Screen.NewTask.createRoute(taskId))
                        }
                    )
                }
            }

            composable(Screen.Groups.route) {
                MainScaffold(
                    navController = navController,
                    currentRoute = Screen.Groups.route,
                    taskViewModel = taskViewModel
                ) {
                    ManageGroupsScreen(viewModel = taskViewModel)
                }
            }

            composable(Screen.Notes.route) {
                MainScaffold(
                    navController = navController,
                    currentRoute = Screen.Notes.route,
                    taskViewModel = taskViewModel
                ) {
                    NotesScreen(
                        viewModel = noteViewModel,
                        taskViewModel = taskViewModel,
                        onNavigateToNote = { noteId ->
                            navController.navigate(Screen.NoteDetail.createRoute(noteId))
                        }
                    )
                }
            }

            composable(Screen.Settings.route) {
                MainScaffold(
                    navController = navController,
                    currentRoute = Screen.Settings.route,
                    taskViewModel = taskViewModel
                ) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        taskViewModel = taskViewModel
                    )
                }
            }

            composable(
                route = Screen.NoteDetail.route,
                arguments = listOf(
                    navArgument("noteId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val noteIdString = backStackEntry.arguments?.getString("noteId")
                val noteId = noteIdString?.toLongOrNull()
                NoteDetailScreen(
                    viewModel = noteViewModel,
                    taskViewModel = taskViewModel,
                    noteId = noteId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.NewTask.route,
                arguments = listOf(
                    navArgument("taskId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val taskIdString = backStackEntry.arguments?.getString("taskId")
                val taskId = taskIdString?.toLongOrNull()

                NewTaskScreen(
                    viewModel = taskViewModel,
                    taskId = taskId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun MainScaffold(
    navController: NavHostController,
    currentRoute: String,
    taskViewModel: TaskViewModel,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        // Pop hasta home para evitar acumulación
                        popUpTo(Screen.Home.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}
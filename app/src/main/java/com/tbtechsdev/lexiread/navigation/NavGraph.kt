package com.tbtechsdev.lexiread.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tbtechsdev.lexiread.ui.classroom.ClassroomRemoteScreen
import com.tbtechsdev.lexiread.ui.reader.ReaderScreen
import com.tbtechsdev.lexiread.ui.reader.ReaderViewModel
import com.tbtechsdev.lexiread.ui.settings.SettingsScreen
import com.tbtechsdev.lexiread.ui.vocabulary.VocabularyScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    readerViewModel: ReaderViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Reader.route,
        modifier = modifier
    ) {
        composable(Screen.Reader.route) {
            ReaderScreen(
                viewModel = readerViewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToClassroomRemote = {
                    navController.navigate(Screen.ClassroomRemote.route)
                }
            )
        }
        composable(Screen.ClassroomRemote.route) {
            ClassroomRemoteScreen(
                pdfWords = readerViewModel.getCurrentPageWords(),
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Vocabulary.route) {
            VocabularyScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}


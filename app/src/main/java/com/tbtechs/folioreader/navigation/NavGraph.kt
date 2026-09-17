package com.tbtechs.folioreader.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tbtechs.folioreader.ui.classroom.ClassroomRemoteScreen
import com.tbtechs.folioreader.ui.reader.ReaderScreen
import com.tbtechs.folioreader.ui.reader.ReaderViewModel
import com.tbtechs.folioreader.ui.settings.SettingsScreen
import com.tbtechs.folioreader.ui.vocabulary.VocabularyScreen

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


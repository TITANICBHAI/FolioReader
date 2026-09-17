package com.tbtechs.folioreader.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tbtechs.folioreader.R
import com.tbtechs.folioreader.navigation.NavGraph
import com.tbtechs.folioreader.navigation.Screen
import com.tbtechs.folioreader.ui.onboarding.OnboardingScreen
import com.tbtechs.folioreader.ui.reader.ReaderViewModel

@Composable
fun FolioReaderMainScreen(
    pendingPdfUri: Uri? = null,
    onPdfHandled: () -> Unit = {},
    readerViewModel: ReaderViewModel = hiltViewModel()
) {
    val isOnboardingCompleted by readerViewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    val showPostOnboardingHindiPrompt by readerViewModel.showPostOnboardingHindiPrompt.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val readerUiState by readerViewModel.uiState.collectAsStateWithLifecycle()

    // Automatically handle external incoming PDF (from WhatsApp, Chrome, My Files, Drive, etc.)
    LaunchedEffect(pendingPdfUri) {
        pendingPdfUri?.let { uri ->
            if (!isOnboardingCompleted) {
                readerViewModel.completeOnboarding()
            }
            readerViewModel.openPdf(uri)
            if (currentDestination?.hierarchy?.any { it.route == Screen.Reader.route } != true) {
                navController.navigate(Screen.Reader.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            onPdfHandled()
        }
    }

    if (!isOnboardingCompleted && pendingPdfUri == null) {
        OnboardingScreen(
            onFinished = {
                readerViewModel.completeOnboarding()
            }
        )
        return
    }

    val isReaderTab = currentDestination?.hierarchy?.any { it.route == Screen.Reader.route } != false
    val isClassroomRemote = currentDestination?.route == Screen.ClassroomRemote.route
    val showBottomBar = (!isReaderTab || !readerUiState.isPdfLoaded) && !isClassroomRemote

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Screen.bottomNavItems.forEach { screen ->
                            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            NavigationBarItem(
                                modifier = Modifier.testTag(screen.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = {
                                    Text(text = screen.title)
                                },
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavGraph(
                navController = navController,
                readerViewModel = readerViewModel,
                modifier = Modifier.padding(if (showBottomBar) innerPadding else PaddingValues(0.dp))
            )
        }

        // Post-onboarding Hindi model prompt dialog
        if (showPostOnboardingHindiPrompt) {
            AlertDialog(
                onDismissRequest = { readerViewModel.dismissPostOnboardingHindiPrompt() },
                title = {
                    Text(text = stringResource(R.string.hindi_prompt_title))
                },
                text = {
                    Text(text = stringResource(R.string.hindi_prompt_desc))
                },
                confirmButton = {
                    Button(
                        onClick = { readerViewModel.downloadHindiModelFromPrompt() },
                        modifier = Modifier.testTag("onboarding_confirm_download_hindi")
                    ) {
                        Text(text = stringResource(R.string.hindi_prompt_download))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { readerViewModel.dismissPostOnboardingHindiPrompt() },
                        modifier = Modifier.testTag("onboarding_dismiss_download_hindi")
                    ) {
                        Text(text = stringResource(R.string.hindi_prompt_later))
                    }
                }
            )
        }
    }
}


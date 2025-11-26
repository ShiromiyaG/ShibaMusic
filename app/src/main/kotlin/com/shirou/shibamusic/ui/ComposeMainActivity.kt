package com.shirou.shibamusic.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shirou.shibamusic.github.utils.UpdateUtil
import com.shirou.shibamusic.helper.LanguageHelper
import com.shirou.shibamusic.ui.component.UpdateAvailableDialog
import com.shirou.shibamusic.ui.navigation.*
import com.shirou.shibamusic.ui.player.MiniPlayer
import com.shirou.shibamusic.ui.theme.ShibaMusicTheme
import com.shirou.shibamusic.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/**
 * Main Activity using Jetpack Compose
 */
@AndroidEntryPoint
class ComposeMainActivity : ComponentActivity() {

	override fun attachBaseContext(newBase: Context?) {
		val wrappedContext = newBase?.let { LanguageHelper.wrapContext(it) }
		super.attachBaseContext(wrappedContext ?: newBase)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		actionBar?.hide()
		enableEdgeToEdge()

		setContent {
			val mainViewModel: MainViewModel = viewModel()
			val theme by mainViewModel.theme.collectAsState()
			
			ShibaMusicTheme(themeMode = theme) {
				ShibaMusicApp(mainViewModel = mainViewModel)
			}
		}
	}
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ShibaMusicApp(mainViewModel: MainViewModel) {
	val latestReleaseLiveData = remember(mainViewModel) { mainViewModel.checkShibaMusicUpdate() }
	val latestRelease by latestReleaseLiveData.observeAsState()
	val updateAvailable = remember(latestRelease) {
		latestRelease?.let { UpdateUtil.showUpdateDialog(it) } == true
	}
	var showUpdateDialog by remember { mutableStateOf(false) }

	LaunchedEffect(updateAvailable, latestRelease?.id) {
		if (updateAvailable) {
			showUpdateDialog = true
		}
	}

	val navController = rememberNavController()
	val navBackStackEntry by navController.currentBackStackEntryAsState()
	val currentDestination = navBackStackEntry?.destination
	val density = LocalDensity.current
	var miniPlayerHeightPx by remember { mutableStateOf(0) }
    
	// Check if user is logged in
	val isLoggedIn by remember {
		derivedStateOf {
			val server = com.shirou.shibamusic.util.Preferences.getServer()
			val user = com.shirou.shibamusic.util.Preferences.getUser()
			!server.isNullOrBlank() && !user.isNullOrBlank()
		}
	}
    
	// Determine start destination based on login status
	val startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route
    
	val showBottomBar = remember(currentDestination?.route) {
		currentDestination?.route in listOf(
			Screen.Home.route,
			Screen.Search.route,
			Screen.Library.route
		)
	}
    
	val showMiniPlayer = remember(currentDestination?.route) {
		currentDestination?.route != Screen.Player.route &&
		currentDestination?.route != Screen.Login.route &&
		currentDestination?.route != Screen.Settings.route
	}

	val miniPlayerBottomPadding = if (showMiniPlayer && miniPlayerHeightPx > 0) {
		with(density) { miniPlayerHeightPx.toDp() }
	} else {
		0.dp
	}

	SharedTransitionLayout {
		Scaffold(
			bottomBar = {
				if (showBottomBar) {
					Column(
						modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
					) {
						NavigationBar(
							modifier = Modifier.renderInSharedTransitionScopeOverlay(
								renderInOverlay = { showMiniPlayer || currentDestination?.route == Screen.Player.route },
								zIndexInOverlay = 2f
							)
						) {
							bottomNavItems.forEach { item ->
								val selected = currentDestination?.hierarchy?.any {
									it.route == item.screen.route
								} == true
								NavigationBarItem(
									selected = selected,
									onClick = {
										navController.navigate(item.screen.route) {
											popUpTo(Screen.Home.route) {
												saveState = true
											}
											launchSingleTop = true
											restoreState = true
										}
									},
									icon = {
										Icon(
											imageVector = item.icon,
											contentDescription = stringResource(item.titleResId)
										)
									},
									label = { Text(stringResource(item.titleResId)) }
								)
							}
						}
					}
				}
			}
		) { paddingValues ->
			CompositionLocalProvider(
				LocalSharedTransitionScope provides this@SharedTransitionLayout
			) {
				Box(modifier = Modifier.fillMaxSize()) {
					Column(
						modifier = Modifier
							.fillMaxSize()
							.padding(paddingValues)
					) {
						Box(
							modifier = Modifier
								.weight(1f)
								.fillMaxWidth()
						) {
							ShibaMusicNavGraph(
								navController = navController,
								startDestination = startDestination,
								modifier = Modifier.fillMaxSize(),
								contentBottomPadding = miniPlayerBottomPadding
							)
						}
					}
					
					AnimatedVisibility(
						visible = showMiniPlayer,
						modifier = Modifier
							.align(Alignment.BottomCenter)
							.padding(paddingValues),
						enter = slideInVertically(
							initialOffsetY = { it },
							animationSpec = tween(250)
						) + fadeIn(tween(250)),
						exit = fadeOut(tween(150))
					) {
						val animatedScope = this
						Box(
							modifier = Modifier.onSizeChanged { size ->
								if (miniPlayerHeightPx != size.height) {
									miniPlayerHeightPx = size.height
								}
							}
						) {
							MiniPlayer(
								onClick = {
									navController.navigate(Screen.Player.route)
								},
								animatedVisibilityScope = animatedScope,
								sharedTransitionScope = this@SharedTransitionLayout
							)
						}
					}

					if (updateAvailable && showUpdateDialog && latestRelease != null) {
						UpdateAvailableDialog(
							release = latestRelease!!,
							onDismiss = { showUpdateDialog = false }
						)
					}
				}
			}
		}
	}
}



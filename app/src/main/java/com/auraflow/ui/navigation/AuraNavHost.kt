package com.auraflow.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.auraflow.ui.components.CosmicBackground
import com.auraflow.ui.screens.home.HomeScreen
import com.auraflow.ui.screens.onboarding.LoginScreen
import com.auraflow.ui.screens.onboarding.OnboardingScreen
import com.auraflow.ui.screens.practice.PracticeScreen
import com.auraflow.ui.screens.profile.ProfileScreen
import com.auraflow.ui.screens.settings.SettingsScreen
import com.auraflow.ui.screens.splash.SplashScreen
import com.auraflow.ui.theme.NeonCyan
import com.auraflow.ui.theme.Night
import com.auraflow.ui.theme.SurfaceGlass
import com.auraflow.ui.theme.TextMuted

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding?rebuild={rebuild}"
    const val LOGIN = "login"
    const val HOME = "home"
    const val PRACTICE = "practice/{day}"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"

    fun onboarding(rebuild: Boolean) = "onboarding?rebuild=$rebuild"
    fun practice(day: Int) = "practice/$day"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, "Главная", Icons.Rounded.SelfImprovement),
    Tab(Routes.PROFILE, "Кабинет", Icons.Rounded.AutoAwesome),
    Tab(Routes.SETTINGS, "Настройки", Icons.Rounded.Tune)
)

@Composable
fun AuraNavHost() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showTabs = TABS.any { it.route == route }

    CosmicBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                AnimatedVisibility(
                    visible = showTabs,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    BottomTabs(route) { target -> nav.navigateToTab(target) }
                }
            }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = Routes.SPLASH,
                modifier = Modifier.padding(padding),
                enterTransition = { fadeIn(tween(500)) + scaleIn(tween(500), initialScale = 0.96f) },
                exitTransition = { fadeOut(tween(300)) },
                popEnterTransition = { fadeIn(tween(500)) },
                popExitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.96f) }
            ) {
                composable(Routes.SPLASH) {
                    SplashScreen(onFinished = { destination ->
                        nav.navigate(destination) { popUpTo(Routes.SPLASH) { inclusive = true } }
                    })
                }
                composable(
                    Routes.ONBOARDING,
                    arguments = listOf(navArgument("rebuild") {
                        type = NavType.BoolType
                        defaultValue = false
                    })
                ) {
                    OnboardingScreen(
                        onFinished = { nav.navigate(Routes.HOME) { popUpTo(nav.graph.id) { inclusive = true } } },
                        onLogin = { nav.navigate(Routes.LOGIN) }
                    )
                }
                composable(Routes.LOGIN) {
                    LoginScreen(
                        onLoggedIn = { nav.navigate(Routes.HOME) { popUpTo(nav.graph.id) { inclusive = true } } },
                        onCreateProfile = { nav.navigate(Routes.onboarding(rebuild = false)) }
                    )
                }
                composable(Routes.HOME) {
                    HomeScreen(onStartPractice = { day -> nav.navigate(Routes.practice(day)) })
                }
                composable(
                    Routes.PRACTICE,
                    arguments = listOf(navArgument("day") { type = NavType.IntType })
                ) {
                    PracticeScreen(onExit = { nav.popBackStack() })
                }
                composable(Routes.PROFILE) {
                    ProfileScreen()
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onRebuildPlan = { nav.navigate(Routes.onboarding(rebuild = true)) },
                        onLoggedOut = { nav.navigate(Routes.LOGIN) { popUpTo(nav.graph.id) { inclusive = true } } }
                    )
                }
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomTabs(currentRoute: String?, onSelect: (String) -> Unit) {
    NavigationBar(containerColor = SurfaceGlass.copy(alpha = 0.88f), tonalElevation = 0.dp) {
        for (tab in TABS) {
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onSelect(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Night,
                    selectedTextColor = NeonCyan,
                    indicatorColor = NeonCyan,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )
            )
        }
    }
}

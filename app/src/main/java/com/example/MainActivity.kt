package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ThemePreferences
import com.example.ui.navigation.ArkAppRoot
import com.example.ui.navigation.ArkRoutes
import com.example.ui.theme.ArkSocialTheme

data class MainBottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

val mainBottomTabs = listOf(
    MainBottomTab(ArkRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    MainBottomTab(ArkRoutes.EXPLORE, "Explore", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_explore"),
    MainBottomTab(ArkRoutes.CREATE, "Create", Icons.Filled.AddBox, Icons.Outlined.AddBox, "nav_create"),
    MainBottomTab(ArkRoutes.REELS, "Reels", Icons.Filled.SmartDisplay, Icons.Outlined.SmartDisplay, "nav_reels"),
    MainBottomTab(ArkRoutes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePrefs = remember { ThemePreferences(applicationContext) }
            val storedDarkMode by themePrefs.darkModeFlow.collectAsStateWithLifecycle(initialValue = null)
            val useDarkTheme = storedDarkMode ?: isSystemInDarkTheme()

            ArkSocialTheme(darkTheme = useDarkTheme) {
                ArkAppRoot(
                    themePreferences = themePrefs,
                    isDarkMode = useDarkTheme
                )
            }
        }
    }
}

@Composable
fun MainBottomNavScaffold(
    currentRoute: String?,
    showBottomBar: Boolean,
    snackbarHostState: SnackbarHostState,
    onTabSelected: (String) -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                MainBottomNavigationBar(
                    currentRoute = currentRoute,
                    onTabSelected = onTabSelected
                )
            }
        }
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }
}

@Composable
fun MainBottomNavigationBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        windowInsets = WindowInsets.navigationBars
    ) {
        mainBottomTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label) },
                alwaysShowLabel = true,
                modifier = Modifier.testTag(tab.testTag)
            )
        }
    }
}

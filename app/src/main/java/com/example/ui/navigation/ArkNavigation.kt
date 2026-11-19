package com.example.ui.navigation

import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.ArkSocialRepository
import com.example.data.ThemePreferences
import com.example.data.authStateFlow
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.signOutUser
import com.example.ui.screens.AccountRecoveryScreen
import com.example.ui.screens.CommentsScreen
import com.example.ui.screens.CreatePostScreen
import com.example.ui.screens.DirectMessagesScreen
import com.example.ui.screens.EditProfileScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FollowersFollowingScreen
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PrivacySettingsScreen
import com.example.ui.screens.ReelsScreen
import com.example.ui.screens.SavedPostsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignupScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

object ArkRoutes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val RECOVERY = "recovery"

    const val HOME = "home"
    const val EXPLORE = "explore"
    const val CREATE = "create"
    const val REELS = "reels"
    const val PROFILE = "profile"

    const val EDIT_PROFILE = "edit_profile"
    const val FOLLOWERS = "followers/{tab}"
    const val COMMENTS = "comments/{postId}"
    const val SAVED_POSTS = "saved_posts"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val PRIVACY_SETTINGS = "privacy_settings"
    const val DIRECT_MESSAGES = "direct_messages"
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val bottomNavItems = listOf(
    BottomNavItem(ArkRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    BottomNavItem(ArkRoutes.EXPLORE, "Explore", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_explore"),
    BottomNavItem(ArkRoutes.CREATE, "Create", Icons.Filled.AddBox, Icons.Outlined.AddBox, "nav_create"),
    BottomNavItem(ArkRoutes.REELS, "Reels", Icons.Filled.SmartDisplay, Icons.Outlined.SmartDisplay, "nav_reels"),
    BottomNavItem(ArkRoutes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
)

@Composable
fun ArkAppRoot(
    themePreferences: ThemePreferences,
    isDarkMode: Boolean
) {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var splashCompleted by remember { mutableStateOf(false) }

    // Pending signup handle customization captured before Google auth finishes
    var pendingUsername by remember { mutableStateOf<String?>(null) }
    var pendingDisplayName by remember { mutableStateOf<String?>(null) }
    var pendingBio by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val user = currentUser
    if (!splashCompleted) {
        SplashScreen(
            onAuthenticated = { splashCompleted = true },
            onRequireAuth = { splashCompleted = true }
        )
    } else if (user == null) {
        val authNav = rememberNavController()
        NavHost(
            navController = authNav,
            startDestination = ArkRoutes.WELCOME
        ) {
            composable(ArkRoutes.WELCOME) {
                WelcomeScreen(
                    onNavigateToLogin = { authNav.navigate(ArkRoutes.LOGIN) },
                    onNavigateToSignup = { authNav.navigate(ArkRoutes.SIGNUP) },
                    onAuthSuccess = {}
                )
            }
            composable(ArkRoutes.LOGIN) {
                LoginScreen(
                    onBack = { authNav.popBackStack() },
                    onNavigateToSignup = { authNav.navigate(ArkRoutes.SIGNUP) },
                    onNavigateToForgotPassword = { authNav.navigate(ArkRoutes.RECOVERY) },
                    onAuthSuccess = {}
                )
            }
            composable(ArkRoutes.SIGNUP) {
                SignupScreen(
                    onBack = { authNav.popBackStack() },
                    onNavigateToLogin = { authNav.navigate(ArkRoutes.LOGIN) },
                    onAuthSuccessWithHandle = { uname, dname, bio ->
                        pendingUsername = uname.ifBlank { null }
                        pendingDisplayName = dname.ifBlank { null }
                        pendingBio = bio.ifBlank { null }
                        val signedUser = Firebase.auth.currentUser
                        if (signedUser != null && (pendingUsername != null || pendingDisplayName != null)) {
                            scope.launch {
                                val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
                                ArkSocialRepository(db).ensureUserProfileInitialized(
                                    signedUser,
                                    preferredUsername = pendingUsername,
                                    preferredDisplayName = pendingDisplayName,
                                    preferredBio = pendingBio
                                )
                            }
                        }
                    }
                )
            }
            composable(ArkRoutes.RECOVERY) {
                AccountRecoveryScreen(onBack = { authNav.popBackStack() })
            }
        }
    } else {
        key(user.uid) {
            val viewModel: MainViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val dbId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(dbId)
                        MainViewModel(ArkSocialRepository(db), user.uid)
                    }
                }
            )
            AuthenticatedAppShell(
                viewModel = viewModel,
                themePreferences = themePreferences,
                isDarkMode = isDarkMode
            )
        }
    }
}

@Composable
private fun AuthenticatedAppShell(
    viewModel: MainViewModel,
    themePreferences: ThemePreferences,
    isDarkMode: Boolean
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val topLevelRoutes = remember { bottomNavItems.map { it.route }.toSet() }
    val showBottomNav = currentRoute in topLevelRoutes

    var createInitialMode by remember { mutableStateOf("photo") }

    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val privateSettings by viewModel.privateSettings.collectAsStateWithLifecycle()
    val feedState by viewModel.feedPosts.collectAsStateWithLifecycle()
    val userPosts by viewModel.userPosts.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val discoverableUsers by viewModel.discoverableUsers.collectAsStateWithLifecycle()
    val likedPostIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val savedPosts by viewModel.savedPosts.collectAsStateWithLifecycle()
    val savedPostIds by viewModel.savedPostIds.collectAsStateWithLifecycle()
    val following by viewModel.following.collectAsStateWithLifecycle()
    val followingUserIds by viewModel.followingUserIds.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadNotifCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeComments by viewModel.activeComments.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.statusMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val currentProfile = (profileState as? UiState.Success)?.data ?: UserProfile(
        id = viewModel.currentUserId,
        userId = viewModel.currentUserId,
        username = "ark_creator",
        displayName = "ARK Creator"
    )

    val sharePost: (Post) -> Unit = { post ->
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out @${post.authorUsername} on ARK SOCIAL: ${post.caption}")
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share post via"))
    }

    val followFromPost: (Post) -> Unit = { post ->
        val target = discoverableUsers.firstOrNull { (it.userId.ifBlank { it.id }) == post.authorId }
            ?: UserProfile(
                id = post.authorId,
                userId = post.authorId,
                username = post.authorUsername,
                displayName = post.authorDisplayName,
                avatarKey = post.authorAvatarKey
            )
        viewModel.toggleFollow(target)
    }

    val navigateTab: (String) -> Unit = { route ->
        if (route == ArkRoutes.CREATE) {
            createInitialMode = "photo"
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showBottomNav && !useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                alwaysShowLabel = true,
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showBottomNav && useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ArkRoutes.HOME
                    ) {
                        composable(ArkRoutes.HOME) {
                            HomeFeedScreen(
                                currentUserProfile = currentProfile,
                                feedState = feedState,
                                stories = stories,
                                likedPostIds = likedPostIds,
                                savedPostIds = savedPostIds,
                                followingUserIds = followingUserIds,
                                unreadNotificationsCount = unreadNotifCount,
                                onOpenNotifications = { navController.navigate(ArkRoutes.NOTIFICATIONS) },
                                onOpenMessages = { navController.navigate(ArkRoutes.DIRECT_MESSAGES) },
                                onAddStoryClick = {
                                    createInitialMode = "story"
                                    navController.navigate(ArkRoutes.CREATE)
                                },
                                onLikeToggle = { viewModel.toggleLike(it) },
                                onDoubleTapLike = { viewModel.toggleLike(it) },
                                onCommentClick = { post ->
                                    viewModel.selectPostForComments(post.id)
                                    navController.navigate("comments/${post.id}")
                                },
                                onShareClick = sharePost,
                                onSaveToggle = { viewModel.toggleSave(it) },
                                onFollowAuthor = followFromPost,
                                onAuthorClick = { authorId ->
                                    if (authorId == viewModel.currentUserId) {
                                        navigateTab(ArkRoutes.PROFILE)
                                    } else {
                                        navController.navigate("followers/0")
                                    }
                                },
                                onCreateFirstPost = { navigateTab(ArkRoutes.CREATE) }
                            )
                        }

                        composable(ArkRoutes.EXPLORE) {
                            ExploreScreen(
                                feedState = feedState,
                                creators = discoverableUsers,
                                followingUserIds = followingUserIds,
                                onFollowCreator = { viewModel.toggleFollow(it) },
                                onPostClick = { post ->
                                    viewModel.selectPostForComments(post.id)
                                    navController.navigate("comments/${post.id}")
                                },
                                onCreatorClick = { navController.navigate("followers/0") }
                            )
                        }

                        composable(ArkRoutes.CREATE) {
                            CreatePostScreen(
                                isBusy = isBusy,
                                initialMode = createInitialMode,
                                onPublish = { caption, mediaType, mediaKey, location ->
                                    viewModel.publishPost(caption, mediaType, mediaKey, location) {
                                        navigateTab(ArkRoutes.HOME)
                                    }
                                }
                            )
                        }

                        composable(ArkRoutes.REELS) {
                            val allPosts = (feedState as? UiState.Success)?.data.orEmpty()
                            ReelsScreen(
                                posts = allPosts,
                                likedPostIds = likedPostIds,
                                savedPostIds = savedPostIds,
                                followingUserIds = followingUserIds,
                                currentUserId = viewModel.currentUserId,
                                onLikeToggle = { viewModel.toggleLike(it) },
                                onDoubleTapLike = { viewModel.toggleLike(it) },
                                onCommentClick = { post ->
                                    viewModel.selectPostForComments(post.id)
                                    navController.navigate("comments/${post.id}")
                                },
                                onShareClick = sharePost,
                                onSaveToggle = { viewModel.toggleSave(it) },
                                onFollowAuthor = followFromPost
                            )
                        }

                        composable(ArkRoutes.PROFILE) {
                            UserProfileScreen(
                                profileState = profileState,
                                userPosts = userPosts,
                                savedPosts = savedPosts,
                                followingCount = following.size,
                                onEditProfile = { navController.navigate(ArkRoutes.EDIT_PROFILE) },
                                onOpenSavedPosts = { navController.navigate(ArkRoutes.SAVED_POSTS) },
                                onOpenSettings = { navController.navigate(ArkRoutes.SETTINGS) },
                                onOpenFollowers = { tab -> navController.navigate("followers/$tab") },
                                onCreatePostClick = { navigateTab(ArkRoutes.CREATE) }
                            )
                        }

                        composable(ArkRoutes.EDIT_PROFILE) {
                            EditProfileScreen(
                                profile = currentProfile,
                                isBusy = isBusy,
                                onBack = { navController.popBackStack() },
                                onSave = { uname, dname, bio, site, avatar ->
                                    viewModel.updateProfile(uname, dname, bio, site, avatar) {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }

                        composable(
                            route = ArkRoutes.FOLLOWERS,
                            arguments = listOf(navArgument("tab") { type = NavType.IntType })
                        ) { entry ->
                            val tab = entry.arguments?.getInt("tab") ?: 0
                            FollowersFollowingScreen(
                                initialTab = tab,
                                following = following,
                                discoverableUsers = discoverableUsers,
                                followingUserIds = followingUserIds,
                                onToggleFollow = { viewModel.toggleFollow(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = ArkRoutes.COMMENTS,
                            arguments = listOf(navArgument("postId") { type = NavType.StringType })
                        ) { entry ->
                            val postId = entry.arguments?.getString("postId") ?: ""
                            val post = (feedState as? UiState.Success)?.data?.firstOrNull { it.id == postId }
                            CommentsScreen(
                                post = post,
                                comments = activeComments,
                                onSendComment = { text ->
                                    if (post != null) viewModel.addComment(post, text)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ArkRoutes.SAVED_POSTS) {
                            SavedPostsScreen(
                                savedPosts = savedPosts,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ArkRoutes.NOTIFICATIONS) {
                            NotificationsScreen(
                                notifications = notifications,
                                onMarkRead = { viewModel.markNotificationRead(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ArkRoutes.SETTINGS) {
                            SettingsScreen(
                                isDarkMode = isDarkMode,
                                onToggleDarkMode = { enabled ->
                                    scope.launch { themePreferences.setDarkMode(enabled) }
                                },
                                onOpenPrivacySettings = { navController.navigate(ArkRoutes.PRIVACY_SETTINGS) },
                                onOpenSavedPosts = { navController.navigate(ArkRoutes.SAVED_POSTS) },
                                onLogout = {
                                    signOutUser(
                                        credentialManager = credentialManager,
                                        scope = scope
                                    )
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ArkRoutes.PRIVACY_SETTINGS) {
                            PrivacySettingsScreen(
                                settings = privateSettings,
                                onUpdateSettings = { priv, likes, comments, follows, status ->
                                    viewModel.updatePrivacySettings(priv, likes, comments, follows, status)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ArkRoutes.DIRECT_MESSAGES) {
                            DirectMessagesScreen(
                                currentUserId = viewModel.currentUserId,
                                creators = discoverableUsers,
                                conversations = conversations,
                                activeMessages = activeMessages,
                                onSelectConversation = { viewModel.selectConversation(it) },
                                onSendMessage = { target, text -> viewModel.sendDirectMessage(target, text) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

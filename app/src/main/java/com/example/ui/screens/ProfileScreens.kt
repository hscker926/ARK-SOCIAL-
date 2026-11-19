package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.FollowingRecord
import com.example.data.model.Post
import com.example.data.model.SavedPost
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.ArkGradientAvatar
import com.example.ui.components.ArkGradientButton
import com.example.ui.components.ArkMediaImage
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.ArkCoral

@Composable
fun UserProfileScreen(
    profileState: UiState<UserProfile>,
    userPosts: List<Post>,
    savedPosts: List<SavedPost>,
    followingCount: Int,
    onEditProfile: () -> Unit,
    onOpenSavedPosts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFollowers: (initialTab: Int) -> Unit,
    onCreatePostClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        when (profileState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateCard(
                        title = "Profile unavailable",
                        subtitle = profileState.message
                    )
                }
            }
            is UiState.Success -> {
                val profile = profileState.data

                // Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "@${profile.username.ifBlank { "ark_creator" }}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = onOpenSavedPosts,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("profile_saved_button")
                    ) {
                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Saved posts")
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("profile_settings_button")
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxSize()
                    ) {
                        // Profile Info Header
                        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ArkGradientAvatar(
                                    avatarKey = profile.avatarKey,
                                    displayName = profile.displayName,
                                    size = 80.dp,
                                    showStoryRing = true
                                )
                                Spacer(modifier = Modifier.width(20.dp))
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    ProfileStatColumn(
                                        count = maxOf(profile.postsCount, userPosts.size.toLong()),
                                        label = "Posts",
                                        onClick = { selectedTab = 0 }
                                    )
                                    ProfileStatColumn(
                                        count = profile.followersCount,
                                        label = "Followers",
                                        onClick = { onOpenFollowers(0) }
                                    )
                                    ProfileStatColumn(
                                        count = maxOf(profile.followingCount, followingCount.toLong()),
                                        label = "Following",
                                        onClick = { onOpenFollowers(1) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = profile.displayName.ifBlank { "ARK Creator" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (profile.bio.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = profile.bio,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            if (profile.website.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = profile.website,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onEditProfile,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("edit_profile_button")
                                ) {
                                    Text("Edit Profile")
                                }
                                OutlinedButton(
                                    onClick = onOpenSavedPosts,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("view_saved_button")
                                ) {
                                    Text("Saved (${savedPosts.size})")
                                }
                            }
                        }

                        // Tabs: Posts | Reels | Saved
                        TabRow(selectedTabIndex = selectedTab) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Outlined.GridOn, contentDescription = "Posts grid") }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Outlined.VideoLibrary, contentDescription = "Reels grid") }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Saved grid") }
                            )
                        }

                        when (selectedTab) {
                            0 -> ProfilePostGrid(
                                posts = userPosts,
                                emptyTitle = "Share your first post",
                                emptySubtitle = "Photos and visual stories you publish appear here.",
                                onAction = onCreatePostClick
                            )
                            1 -> ProfilePostGrid(
                                posts = userPosts.filter { it.mediaType == "reel" },
                                emptyTitle = "No Reels yet",
                                emptySubtitle = "Create a vertical Reel to showcase motion stories.",
                                onAction = onCreatePostClick
                            )
                            else -> SavedPostsGridContent(savedPosts = savedPosts)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatColumn(
    count: Long,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = formatCompactCount(count),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfilePostGrid(
    posts: List<Post>,
    emptyTitle: String,
    emptySubtitle: String,
    onAction: () -> Unit
) {
    if (posts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            EmptyStateCard(
                title = emptyTitle,
                subtitle = emptySubtitle,
                actionLabel = "Create Now",
                onAction = onAction
            )
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(posts, key = { it.id }) { post ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                ArkMediaImage(
                    mediaKey = post.mediaKey,
                    contentDescription = post.caption.ifBlank { "User post" },
                    modifier = Modifier.fillMaxSize()
                )
                if (post.mediaType == "reel") {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Reel",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedPostsGridContent(savedPosts: List<SavedPost>) {
    if (savedPosts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            EmptyStateCard(
                title = "Nothing saved yet",
                subtitle = "Tap the bookmark icon on any post or reel to keep it in your collection."
            )
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(savedPosts, key = { it.postId }) { saved ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                ArkMediaImage(
                    mediaKey = saved.mediaKey,
                    contentDescription = saved.caption.ifBlank { "Saved post" },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun EditProfileScreen(
    profile: UserProfile,
    isBusy: Boolean,
    onBack: () -> Unit,
    onSave: (username: String, displayName: String, bio: String, website: String, avatarKey: String) -> Unit
) {
    var username by remember(profile.username) { mutableStateOf(profile.username) }
    var displayName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var bio by remember(profile.bio) { mutableStateOf(profile.bio) }
    var website by remember(profile.website) { mutableStateOf(profile.website) }
    var avatarKey by remember(profile.avatarKey) { mutableStateOf(profile.avatarKey.ifBlank { "preset_hero" }) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val avatarOptions = listOf("preset_hero", "preset_arch", "preset_studio", "preset_coast", "preset_icon")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArkGradientAvatar(
                    avatarKey = avatarKey,
                    displayName = displayName.ifBlank { "ARK" },
                    size = 86.dp,
                    showStoryRing = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Choose Avatar Style",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(avatarOptions) { key ->
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .then(
                                    if (avatarKey == key) Modifier.border(2.5.dp, ArkCoral, RoundedCornerShape(14.dp))
                                    else Modifier
                                )
                                .clickable { avatarKey = key }
                        ) {
                            ArkMediaImage(
                                mediaKey = key,
                                contentDescription = "Avatar option",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { if (it.length <= 50) displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_display_name_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { if (it.length <= 30) username = it.lowercase() },
                    label = { Text("Username") },
                    prefix = { Text("@") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_username_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 200) bio = it },
                    label = { Text("Bio") },
                    minLines = 3,
                    maxLines = 4,
                    supportingText = { Text("${bio.length}/200") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_bio_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = website,
                    onValueChange = { if (it.length <= 120) website = it },
                    label = { Text("Website") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(22.dp))
                ArkGradientButton(
                    text = "Save Profile",
                    isLoading = isBusy,
                    onClick = {
                        val cleanUser = username.trim().lowercase()
                        val cleanName = displayName.trim()
                        if (cleanName.isBlank()) {
                            errorMessage = "Display name cannot be empty."
                            return@ArkGradientButton
                        }
                        if (cleanUser.length !in 3..30) {
                            errorMessage = "Username must be 3 to 30 characters."
                            return@ArkGradientButton
                        }
                        errorMessage = null
                        onSave(cleanUser, cleanName, bio, website, avatarKey)
                    },
                    testTag = "save_profile_button"
                )
            }
        }
    }
}

@Composable
fun FollowersFollowingScreen(
    initialTab: Int,
    following: List<FollowingRecord>,
    discoverableUsers: List<UserProfile>,
    followingUserIds: Set<String>,
    onToggleFollow: (UserProfile) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Connections",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Discover") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Following (${following.size})") }
                )
            }

            if (selectedTab == 1) {
                if (following.isEmpty()) {
                    EmptyStateCard(
                        title = "Not following anyone yet",
                        subtitle = "Discover creators to follow their posts and reels."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(following, key = { it.targetUserId }) { record ->
                            val target = UserProfile(
                                id = record.targetUserId,
                                userId = record.targetUserId,
                                username = record.targetUsername,
                                displayName = record.targetDisplayName,
                                avatarKey = record.targetAvatarKey
                            )
                            ConnectionRow(
                                user = target,
                                isFollowing = true,
                                onToggleFollow = { onToggleFollow(target) }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(discoverableUsers, key = { it.userId.ifBlank { it.id } }) { creator ->
                        val id = creator.userId.ifBlank { creator.id }
                        ConnectionRow(
                            user = creator,
                            isFollowing = id in followingUserIds,
                            onToggleFollow = { onToggleFollow(creator) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionRow(
    user: UserProfile,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArkGradientAvatar(
                avatarKey = user.avatarKey,
                displayName = user.displayName,
                size = 46.dp,
                showStoryRing = true
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onToggleFollow,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isFollowing) "Following" else "Follow")
            }
        }
    }
}

@Composable
fun SavedPostsScreen(
    savedPosts: List<SavedPost>,
    onBack: () -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Saved Posts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            SavedPostsGridContent(savedPosts = savedPosts)
        }
    }
}

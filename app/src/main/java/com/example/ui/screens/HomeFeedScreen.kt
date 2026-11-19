package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Post
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.ArkBrandWordmark
import com.example.ui.components.ArkGradientAvatar
import com.example.ui.components.ArkMediaImage
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PostCard
import com.example.ui.theme.ArkBrandGradient
import kotlinx.coroutines.delay

@Composable
fun HomeFeedScreen(
    currentUserProfile: UserProfile,
    feedState: UiState<List<Post>>,
    stories: List<Story>,
    likedPostIds: Set<String>,
    savedPostIds: Set<String>,
    followingUserIds: Set<String>,
    unreadNotificationsCount: Int,
    onOpenNotifications: () -> Unit,
    onOpenMessages: () -> Unit,
    onAddStoryClick: () -> Unit,
    onLikeToggle: (Post) -> Unit,
    onDoubleTapLike: (Post) -> Unit,
    onCommentClick: (Post) -> Unit,
    onShareClick: (Post) -> Unit,
    onSaveToggle: (Post) -> Unit,
    onFollowAuthor: (Post) -> Unit,
    onAuthorClick: (String) -> Unit,
    onCreateFirstPost: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeStory by remember { mutableStateOf<Story?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Editorial Bar
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArkBrandWordmark(compact = true)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("home_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationsCount > 0) {
                                Badge {
                                    Text(unreadNotificationsCount.coerceAtMost(99).toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = "Notifications"
                        )
                    }
                }
                IconButton(
                    onClick = onOpenMessages,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("home_messages_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MailOutline,
                        contentDescription = "Direct Messages"
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        when (feedState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateCard(
                        title = "Feed unavailable",
                        subtitle = feedState.message,
                        actionLabel = "Create a Post",
                        onAction = onCreateFirstPost
                    )
                }
            }
            is UiState.Success -> {
                val posts = feedState.data
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxSize()
                            .testTag("home_feed_list"),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item(key = "stories_row") {
                            StoriesStrip(
                                currentUserProfile = currentUserProfile,
                                stories = stories,
                                onAddStoryClick = onAddStoryClick,
                                onStoryClick = { activeStory = it }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        if (posts.isEmpty()) {
                            item(key = "empty_feed") {
                                EmptyStateCard(
                                    title = "Your feed is ready",
                                    subtitle = "Follow creators in Explore or share your first photo to start the conversation.",
                                    actionLabel = "Share First Post",
                                    onAction = onCreateFirstPost
                                )
                            }
                        } else {
                            items(posts, key = { it.id }) { post ->
                                PostCard(
                                    post = post,
                                    isLiked = post.id in likedPostIds,
                                    isSaved = post.id in savedPostIds,
                                    isFollowingAuthor = post.authorId in followingUserIds,
                                    isCurrentUserAuthor = post.authorId == currentUserProfile.userId,
                                    onLikeToggle = { onLikeToggle(post) },
                                    onDoubleTapLike = { onDoubleTapLike(post) },
                                    onCommentClick = { onCommentClick(post) },
                                    onShareClick = { onShareClick(post) },
                                    onSaveToggle = { onSaveToggle(post) },
                                    onFollowClick = { onFollowAuthor(post) },
                                    onAuthorClick = { onAuthorClick(post.authorId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    activeStory?.let { story ->
        StoryViewerDialog(
            story = story,
            onDismiss = { activeStory = null }
        )
    }
}

@Composable
fun StoriesStrip(
    currentUserProfile: UserProfile,
    stories: List<Story>,
    onAddStoryClick: () -> Unit,
    onStoryClick: (Story) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("stories_horizontal_list"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "your_story") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clickable(onClick = onAddStoryClick)
                    .testTag("add_story_button")
            ) {
                Box(modifier = Modifier.size(66.dp)) {
                    ArkGradientAvatar(
                        avatarKey = currentUserProfile.avatarKey,
                        displayName = currentUserProfile.displayName,
                        size = 62.dp,
                        showStoryRing = false
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(ArkBrandGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add story",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Your Story",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        items(stories, key = { it.id }) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clickable { onStoryClick(story) }
                    .testTag("story_item_${story.id}")
            ) {
                ArkGradientAvatar(
                    avatarKey = story.authorAvatarKey,
                    displayName = story.authorUsername,
                    size = 60.dp,
                    showStoryRing = true,
                    onClick = { onStoryClick(story) }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = story.authorUsername,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StoryViewerDialog(
    story: Story,
    onDismiss: () -> Unit
) {
    var progress by remember(story.id) { mutableStateOf(0f) }

    LaunchedEffect(story.id) {
        val steps = 60
        for (i in 1..steps) {
            progress = i / steps.toFloat()
            delay(80L)
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss)
        ) {
            ArkMediaImage(
                mediaKey = story.mediaKey,
                contentDescription = story.caption.ifBlank { "${story.authorUsername} story" },
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.65f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ArkGradientAvatar(
                        avatarKey = story.authorAvatarKey,
                        displayName = story.authorUsername,
                        size = 36.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "@${story.authorUsername}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close story",
                            tint = Color.White
                        )
                    }
                }
            }

            if (story.caption.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 48.dp)
                ) {
                    Text(
                        text = story.caption,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Post
import com.example.ui.components.ArkGradientAvatar
import com.example.ui.components.ArkMediaImage
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.ArkCoral
import com.example.ui.theme.ArkRose
import kotlinx.coroutines.delay

@Composable
fun ReelsScreen(
    posts: List<Post>,
    likedPostIds: Set<String>,
    savedPostIds: Set<String>,
    followingUserIds: Set<String>,
    currentUserId: String,
    onLikeToggle: (Post) -> Unit,
    onDoubleTapLike: (Post) -> Unit,
    onCommentClick: (Post) -> Unit,
    onShareClick: (Post) -> Unit,
    onSaveToggle: (Post) -> Unit,
    onFollowAuthor: (Post) -> Unit,
    modifier: Modifier = Modifier
) {
    val reels = remember(posts) {
        val explicitReels = posts.filter { it.mediaType == "reel" }
        if (explicitReels.isNotEmpty()) explicitReels else posts
    }

    if (reels.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateCard(
                title = "No Reels Yet",
                subtitle = "Publish a Reel in Create to see vertical motion stories here."
            )
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { reels.size })

    VerticalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("reels_vertical_pager")
    ) { page ->
        val reel = reels[page]
        val isLiked = reel.id in likedPostIds
        val isSaved = reel.id in savedPostIds
        val isFollowing = reel.authorId in followingUserIds

        ReelPageItem(
            reel = reel,
            isLiked = isLiked,
            isSaved = isSaved,
            isFollowing = isFollowing,
            isCurrentUserAuthor = reel.authorId == currentUserId,
            onLikeToggle = { onLikeToggle(reel) },
            onDoubleTapLike = { onDoubleTapLike(reel) },
            onCommentClick = { onCommentClick(reel) },
            onShareClick = { onShareClick(reel) },
            onSaveToggle = { onSaveToggle(reel) },
            onFollowClick = { onFollowAuthor(reel) }
        )
    }
}

@Composable
private fun ReelPageItem(
    reel: Post,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowing: Boolean,
    isCurrentUserAuthor: Boolean,
    onLikeToggle: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveToggle: () -> Unit,
    onFollowClick: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var showHeartBurst by remember { mutableStateOf(false) }

    val eqTransition = rememberInfiniteTransition(label = "reelEq")
    val barScale by eqTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar"
    )

    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            delay(750)
            showHeartBurst = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(reel.id, isLiked) {
                detectTapGestures(
                    onTap = { isPlaying = !isPlaying },
                    onDoubleTap = {
                        showHeartBurst = true
                        if (!isLiked) onDoubleTapLike()
                    }
                )
            }
    ) {
        ArkMediaImage(
            mediaKey = reel.mediaKey,
            contentDescription = reel.caption.ifBlank { "Reel by ${reel.authorUsername}" },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.72f)
                        )
                    )
                )
        )

        // Top Bar Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reels",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (isPlaying) "Playing" else "Paused",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isPlaying) "Playing" else "Paused",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showHeartBurst,
            enter = scaleIn(initialScale = 0.4f) + fadeIn(),
            exit = scaleOut(targetScale = 1.3f) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Liked",
                tint = ArkCoral,
                modifier = Modifier.size(96.dp)
            )
        }

        // Bottom Metadata & Right Action Rail
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ArkGradientAvatar(
                        avatarKey = reel.authorAvatarKey,
                        displayName = reel.authorDisplayName,
                        size = 38.dp,
                        showStoryRing = true
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "@${reel.authorUsername}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isCurrentUserAuthor) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = if (isFollowing) 0.22f else 0.92f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onFollowClick)
                        ) {
                            Text(
                                text = if (isFollowing) "Following" else "Follow",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isFollowing) Color.White else Color.Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = reel.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "Audio track",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Original Audio • ${reel.authorUsername}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height((if (isPlaying) 14f * barScale else 6f).dp)
                            .clip(CircleShape)
                            .background(ArkCoral)
                    )
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height((if (isPlaying) 14f * (1.2f - barScale * 0.5f) else 6f).dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            // Right Action Rail
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val displayedLikes = reel.likesCount + if (reel.isShowcase && isLiked) 1L else 0L
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onLikeToggle,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like reel",
                            tint = if (isLiked) ArkRose else Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Text(
                        text = formatCompactCount(displayedLikes),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onCommentClick,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = formatCompactCount(reel.commentsCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onSaveToggle,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save reel",
                        tint = if (isSaved) ArkCoral else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Send,
                        contentDescription = "Share reel",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

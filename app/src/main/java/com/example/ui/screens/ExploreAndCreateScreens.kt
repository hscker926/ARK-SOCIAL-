package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.ArkGradientAvatar
import com.example.ui.components.ArkGradientButton
import com.example.ui.components.ArkMediaImage
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.ArkCoral

@Composable
fun ExploreScreen(
    feedState: UiState<List<Post>>,
    creators: List<UserProfile>,
    followingUserIds: Set<String>,
    onFollowCreator: (UserProfile) -> Unit,
    onPostClick: (Post) -> Unit,
    onCreatorClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Photos", "Reels", "Architecture", "Travel")

    val allPosts = (feedState as? UiState.Success)?.data.orEmpty()
    val filteredCreators = remember(creators, searchQuery) {
        if (searchQuery.isBlank()) creators
        else creators.filter {
            it.username.contains(searchQuery, ignoreCase = true) ||
                it.displayName.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredPosts = remember(allPosts, searchQuery, selectedFilter) {
        allPosts.filter { post ->
            val matchesQuery = searchQuery.isBlank() ||
                post.caption.contains(searchQuery, ignoreCase = true) ||
                post.authorUsername.contains(searchQuery, ignoreCase = true) ||
                post.location.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Photos" -> post.mediaType == "photo"
                "Reels" -> post.mediaType == "reel"
                "Architecture" -> post.caption.contains("arch", ignoreCase = true) || post.mediaKey == "preset_arch"
                "Travel" -> post.location.isNotBlank() || post.mediaKey == "preset_coast"
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search creators, locations, or posts...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("explore_search_input")
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) }
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (filteredCreators.isNotEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    Column {
                        Text(
                            text = if (searchQuery.isBlank()) "Suggested Creators" else "Matching Creators",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(filteredCreators, key = { it.userId.ifBlank { it.id } }) { creator ->
                                val targetId = creator.userId.ifBlank { creator.id }
                                val isFollowing = targetId in followingUserIds
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier
                                        .width(156.dp)
                                        .clickable { onCreatorClick(targetId) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        ArkGradientAvatar(
                                            avatarKey = creator.avatarKey,
                                            displayName = creator.displayName,
                                            size = 54.dp,
                                            showStoryRing = true
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = creator.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "@${creator.username}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedButton(
                                            onClick = { onFollowCreator(creator) },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (isFollowing) "Following" else "Follow",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (filteredPosts.isEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    EmptyStateCard(
                        title = "No matches found",
                        subtitle = "Try another keyword or clear your search filter."
                    )
                }
            } else {
                items(filteredPosts, key = { it.id }) { post ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPostClick(post) }
                            .testTag("explore_grid_${post.id}")
                    ) {
                        ArkMediaImage(
                            mediaKey = post.mediaKey,
                            contentDescription = post.caption.ifBlank { "Explore post" },
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
                                    .size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePostScreen(
    isBusy: Boolean,
    initialMode: String = "photo",
    onPublish: (caption: String, mediaType: String, mediaKey: String, location: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember(initialMode) { mutableStateOf(initialMode) } // "photo", "reel", "story"
    var selectedMediaKey by remember { mutableStateOf("preset_arch") }
    var caption by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presets = listOf(
        "preset_arch" to "Travertine",
        "preset_studio" to "35mm Studio",
        "preset_coast" to "Ocean Dusk",
        "preset_hero" to "Prism Light"
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.let { selectedMediaKey = it.toString() }
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 560.dp)) {
            Text(
                text = "Create & Upload",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Share a photo post, vertical reel, or story highlight.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            val modes = listOf("photo" to "Post", "reel" to "Reel", "story" to "Story")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, (key, label) ->
                    SegmentedButton(
                        selected = selectedMode == key,
                        onClick = { selectedMode = key },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
                    ) {
                        Text(label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                ArkMediaImage(
                    mediaKey = selectedMediaKey,
                    contentDescription = "Selected media preview",
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = when (selectedMode) {
                            "reel" -> "Reel Preview"
                            "story" -> "Story Preview"
                            else -> "Photo Preview"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("pick_photo_button")
                ) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pick Photo")
                }

                OutlinedButton(
                    onClick = {
                        selectedMode = "reel"
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("pick_video_button")
                ) {
                    Icon(Icons.Outlined.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pick Video")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Or pick a studio canvas",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(presets) { (key, label) ->
                    val isSelected = selectedMediaKey == key
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedMediaKey = key }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .then(
                                    if (isSelected) Modifier.border(2.5.dp, ArkCoral, RoundedCornerShape(14.dp))
                                    else Modifier
                                )
                        ) {
                            ArkMediaImage(
                                mediaKey = key,
                                contentDescription = label,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = caption,
                onValueChange = { if (it.length <= 1000) caption = it },
                label = { Text("Caption") },
                placeholder = { Text("Write something memorable...") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(16.dp),
                supportingText = { Text("${caption.length}/1000") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_caption_input")
            )

            if (selectedMode != "story") {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { if (it.length <= 80) location = it },
                    label = { Text("Location (optional)") },
                    placeholder = { Text("e.g. Tokyo, Japan") },
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_location_input")
                )
            }

            if (selectedMediaKey.startsWith("content://")) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Local gallery URI selected. Note: cross-device media upload requires enabling Firebase Storage in the Firebase Console.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            ArkGradientButton(
                text = when (selectedMode) {
                    "reel" -> "Publish Reel"
                    "story" -> "Share to Story"
                    else -> "Share Post"
                },
                isLoading = isBusy,
                onClick = {
                    if (caption.isBlank() && selectedMode != "story") {
                        errorMessage = "Add a short caption before sharing."
                        return@ArkGradientButton
                    }
                    errorMessage = null
                    onPublish(caption, selectedMode, selectedMediaKey, location)
                },
                testTag = "publish_post_button"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarKey: String = "preset_arch",
    val website: String = "",
    val discoverable: Boolean = true,
    val postsCount: Long = 0L,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivateAccountSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val privateAccount: Boolean = false,
    val pushLikes: Boolean = true,
    val pushComments: Boolean = true,
    val pushFollows: Boolean = true,
    val showActivityStatus: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorDisplayName: String = "",
    val authorAvatarKey: String = "preset_arch",
    val caption: String = "",
    val mediaType: String = "photo",
    val mediaKey: String = "preset_arch",
    val location: String = "",
    val visibility: String = "public",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val isShowcase: Boolean
        get() = id.startsWith("showcase_")
}

@Keep
@IgnoreExtraProperties
data class Comment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val postVisibility: String = "public",
    val authorId: String = "",
    val authorUsername: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LikedPost(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedPost(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    val authorUsername: String = "",
    val caption: String = "",
    val mediaKey: String = "",
    val mediaType: String = "photo",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FollowingRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetUserId: String = "",
    val targetUsername: String = "",
    val targetDisplayName: String = "",
    val targetAvatarKey: String = "preset_studio",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Story(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarKey: String = "preset_arch",
    val mediaKey: String = "preset_arch",
    val caption: String = "",
    val visibility: String = "public",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actorId: String = "",
    val actorUsername: String = "",
    val type: String = "welcome",
    val message: String = "",
    val postId: String = "",
    val unread: Boolean = true,
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantLabel: String = "",
    val lastMessage: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val participantIds: List<String> = emptyList(),
    val senderId: String = "",
    val senderUsername: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
)

object CuratedFeedProvider {
    val showcaseCreators = listOf(
        UserProfile(
            id = "creator_maya_lin",
            userId = "creator_maya_lin",
            username = "maya.atelier",
            displayName = "Maya Lin",
            bio = "Architectural daylight, travertine & calm minimalism.",
            avatarKey = "preset_arch",
            postsCount = 18,
            followersCount = 12400,
            followingCount = 215
        ),
        UserProfile(
            id = "creator_noah_k",
            userId = "creator_noah_k",
            username = "noah.analog",
            displayName = "Noah K.",
            bio = "35mm film grain, espresso & studio mornings.",
            avatarKey = "preset_studio",
            postsCount = 24,
            followersCount = 8950,
            followingCount = 340
        ),
        UserProfile(
            id = "creator_aria_tide",
            userId = "creator_aria_tide",
            username = "aria.horizons",
            displayName = "Aria Tide",
            bio = "Volcanic coastlines & aerial ocean expeditions.",
            avatarKey = "preset_coast",
            postsCount = 31,
            followersCount = 27800,
            followingCount = 180
        ),
        UserProfile(
            id = "creator_ark_studio",
            userId = "creator_ark_studio",
            username = "ark.design",
            displayName = "ARK Collective",
            bio = "Curated visual culture on ARK SOCIAL.",
            avatarKey = "preset_hero",
            postsCount = 42,
            followersCount = 54100,
            followingCount = 92
        )
    )

    val showcaseStories = listOf(
        Story(
            id = "showcase_story_1",
            authorId = "creator_maya_lin",
            authorUsername = "maya.atelier",
            authorAvatarKey = "preset_arch",
            mediaKey = "preset_arch",
            caption = "Golden hour shadows across the courtyard"
        ),
        Story(
            id = "showcase_story_2",
            authorId = "creator_noah_k",
            authorUsername = "noah.analog",
            authorAvatarKey = "preset_studio",
            mediaKey = "preset_studio",
            caption = "Loading Portra 400 before morning light fades"
        ),
        Story(
            id = "showcase_story_3",
            authorId = "creator_aria_tide",
            authorUsername = "aria.horizons",
            authorAvatarKey = "preset_coast",
            mediaKey = "preset_coast",
            caption = "Atlantic swells off black basalt cliffs"
        ),
        Story(
            id = "showcase_story_4",
            authorId = "creator_ark_studio",
            authorUsername = "ark.design",
            authorAvatarKey = "preset_hero",
            mediaKey = "preset_hero",
            caption = "New prism light studies live now"
        )
    )

    val showcasePosts = listOf(
        Post(
            id = "showcase_post_1",
            authorId = "creator_maya_lin",
            authorUsername = "maya.atelier",
            authorDisplayName = "Maya Lin",
            authorAvatarKey = "preset_arch",
            caption = "Sunlit travertine arches and quiet geometry at 5:30 PM. Sometimes negative space tells the whole story.",
            mediaType = "photo",
            mediaKey = "preset_arch",
            location = "Lisbon, Portugal",
            likesCount = 1284,
            commentsCount = 42
        ),
        Post(
            id = "showcase_post_2",
            authorId = "creator_noah_k",
            authorUsername = "noah.analog",
            authorDisplayName = "Noah K.",
            authorAvatarKey = "preset_studio",
            caption = "Morning espresso ritual before scanning the weekend roll. Shooting tactile 35mm slows everything down.",
            mediaType = "photo",
            mediaKey = "preset_studio",
            location = "Kyoto Studio",
            likesCount = 892,
            commentsCount = 19
        ),
        Post(
            id = "showcase_post_3",
            authorId = "creator_aria_tide",
            authorUsername = "aria.horizons",
            authorDisplayName = "Aria Tide",
            authorAvatarKey = "preset_coast",
            caption = "Where turquoise surf meets volcanic basalt at dusk. Vertical motion reel captured from 120m.",
            mediaType = "reel",
            mediaKey = "preset_coast",
            location = "Vik, Iceland",
            likesCount = 4310,
            commentsCount = 114
        ),
        Post(
            id = "showcase_post_4",
            authorId = "creator_ark_studio",
            authorUsername = "ark.design",
            authorDisplayName = "ARK Collective",
            authorAvatarKey = "preset_hero",
            caption = "Refracted prism ribbons in our autumn visual series. Built for creators who care about craft.",
            mediaType = "reel",
            mediaKey = "preset_hero",
            location = "ARK Studio",
            likesCount = 2640,
            commentsCount = 67
        )
    )
}

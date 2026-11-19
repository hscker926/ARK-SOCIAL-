package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ArkSocialRepository
import com.example.data.model.AppNotification
import com.example.data.model.Comment
import com.example.data.model.Conversation
import com.example.data.model.CuratedFeedProvider
import com.example.data.model.DirectMessage
import com.example.data.model.FollowingRecord
import com.example.data.model.Post
import com.example.data.model.PrivateAccountSettings
import com.example.data.model.SavedPost
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: ArkSocialRepository,
    val currentUserId: String
) : ViewModel() {

    private val _statusMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val statusMessages = _statusMessages.asSharedFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    // Local comments cache for featured showcase posts so interaction feels instant
    private val _localShowcaseComments = MutableStateFlow<Map<String, List<Comment>>>(
        mapOf(
            "showcase_post_1" to listOf(
                Comment(
                    id = "sc_1",
                    postId = "showcase_post_1",
                    authorUsername = "noah.analog",
                    text = "The afternoon shadows on that travertine wall are unreal."
                ),
                Comment(
                    id = "sc_2",
                    postId = "showcase_post_1",
                    authorUsername = "aria.horizons",
                    text = "Need to visit Lisbon this autumn!"
                )
            )
        )
    )

    private val _selectedPostIdForComments = MutableStateFlow<String?>(null)
    private val _selectedConversationId = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            Firebase.auth.currentUser?.let { user ->
                repository.ensureUserProfileInitialized(user)
            }
        }
    }

    val userProfile: StateFlow<UiState<UserProfile>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile>> { profile ->
            UiState.Success(
                profile ?: UserProfile(
                    id = currentUserId,
                    userId = currentUserId,
                    username = "ark_${currentUserId.take(6).lowercase()}",
                    displayName = Firebase.auth.currentUser?.displayName ?: "ARK Creator",
                    bio = "Creating & sharing moments on ARK SOCIAL."
                )
            )
        }
        .unwrapFirestoreErrors("profile")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val privateSettings: StateFlow<PrivateAccountSettings> = repository.observePrivateSettings(currentUserId)
        .map { it ?: PrivateAccountSettings(userId = currentUserId) }
        .catch { error ->
            handleStreamCancellationOrLog(error, "privateSettings")
            emit(PrivateAccountSettings(userId = currentUserId))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PrivateAccountSettings(userId = currentUserId))

    val feedPosts: StateFlow<UiState<List<Post>>> = repository.observePublicPosts(currentUserId)
        .map<List<Post>, UiState<List<Post>>> { livePosts ->
            UiState.Success(livePosts + CuratedFeedProvider.showcasePosts)
        }
        .unwrapFirestoreErrors("feedPosts")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val userPosts: StateFlow<List<Post>> = repository.observeUserPosts(currentUserId)
        .catch { error ->
            handleStreamCancellationOrLog(error, "userPosts")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val stories: StateFlow<List<Story>> = repository.observePublicStories(currentUserId)
        .map { liveStories -> liveStories + CuratedFeedProvider.showcaseStories }
        .catch { error ->
            handleStreamCancellationOrLog(error, "stories")
            emit(CuratedFeedProvider.showcaseStories)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CuratedFeedProvider.showcaseStories)

    val discoverableUsers: StateFlow<List<UserProfile>> = repository.observeDiscoverableUsers(currentUserId)
        .map { liveUsers ->
            val liveIds = liveUsers.map { it.userId }.toSet()
            liveUsers.filter { it.userId != currentUserId } +
                CuratedFeedProvider.showcaseCreators.filter { it.userId !in liveIds }
        }
        .catch { error ->
            handleStreamCancellationOrLog(error, "discoverableUsers")
            emit(CuratedFeedProvider.showcaseCreators)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CuratedFeedProvider.showcaseCreators)

    val likedPostIds: StateFlow<Set<String>> = repository.observeLikedPostIds(currentUserId)
        .catch { error ->
            handleStreamCancellationOrLog(error, "likedPostIds")
            emit(emptySet())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val savedPosts: StateFlow<List<SavedPost>> = repository.observeSavedPosts(currentUserId)
        .catch { error ->
            handleStreamCancellationOrLog(error, "savedPosts")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val savedPostIds: StateFlow<Set<String>> = savedPosts
        .map { list -> list.map { it.postId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val following: StateFlow<List<FollowingRecord>> = repository.observeFollowing(currentUserId)
        .catch { error ->
            handleStreamCancellationOrLog(error, "following")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val followingUserIds: StateFlow<Set<String>> = following
        .map { list -> list.map { it.targetUserId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val notifications: StateFlow<List<AppNotification>> = repository.observeNotifications(currentUserId)
        .catch { error ->
            handleStreamCancellationOrLog(error, "notifications")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val conversations: StateFlow<List<Conversation>> = repository.observeConversations(currentUserId)
        .catch { error ->
            handleStreamCancellationOrLog(error, "conversations")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeComments: StateFlow<List<Comment>> = _selectedPostIdForComments
        .flatMapLatest { postId ->
            if (postId == null) {
                flowOf(emptyList())
            } else if (postId.startsWith("showcase_")) {
                _localShowcaseComments.map { it[postId].orEmpty() }
            } else {
                repository.observeComments(postId, currentUserId)
                    .catch { error ->
                        handleStreamCancellationOrLog(error, "comments")
                        emit(emptyList())
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeMessages: StateFlow<List<DirectMessage>> = _selectedConversationId
        .flatMapLatest { convId ->
            if (convId == null) flowOf(emptyList())
            else repository.observeMessages(convId, currentUserId)
                .catch { error ->
                    handleStreamCancellationOrLog(error, "messages")
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = notifications
        .map { list -> list.count { it.unread } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    fun selectPostForComments(postId: String) {
        _selectedPostIdForComments.value = postId
    }

    fun selectConversation(conversationId: String?) {
        _selectedConversationId.value = conversationId
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            val liked = post.id in likedPostIds.value
            val result = repository.toggleLike(post, liked)
            if (result.isFailure) {
                _statusMessages.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Unable to update like")
            }
        }
    }

    fun toggleSave(post: Post) {
        viewModelScope.launch {
            val saved = post.id in savedPostIds.value
            val result = repository.toggleSave(post, saved)
            if (result.isSuccess) {
                _statusMessages.tryEmit(if (saved) "Removed from Saved" else "Saved to your collection")
            } else {
                _statusMessages.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Unable to save post")
            }
        }
    }

    fun toggleFollow(target: UserProfile) {
        viewModelScope.launch {
            val targetId = target.userId.ifBlank { target.id }
            val isFollowing = targetId in followingUserIds.value
            val result = repository.toggleFollow(target, isFollowing)
            if (result.isSuccess) {
                _statusMessages.tryEmit(if (isFollowing) "Unfollowed @${target.username}" else "Following @${target.username}")
            } else {
                _statusMessages.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Unable to update follow")
            }
        }
    }

    fun addComment(post: Post, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val currentProfile = (userProfile.value as? UiState.Success)?.data ?: UserProfile(
            userId = currentUserId,
            username = "ark_creator"
        )
        if (post.isShowcase) {
            val newComment = Comment(
                id = "local_${System.currentTimeMillis()}",
                postId = post.id,
                authorId = currentUserId,
                authorUsername = currentProfile.username.ifBlank { "you" },
                text = clean
            )
            val currentMap = _localShowcaseComments.value.toMutableMap()
            currentMap[post.id] = currentMap[post.id].orEmpty() + newComment
            _localShowcaseComments.value = currentMap
            return
        }

        viewModelScope.launch {
            val result = repository.addComment(currentProfile, post, clean)
            if (result.isFailure) {
                _statusMessages.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to post comment")
            }
        }
    }

    fun publishPost(
        caption: String,
        mediaType: String,
        mediaKey: String,
        location: String,
        onPublished: () -> Unit
    ) {
        val currentProfile = (userProfile.value as? UiState.Success)?.data ?: UserProfile(
            userId = currentUserId,
            username = "ark_creator",
            displayName = "ARK Creator"
        )
        viewModelScope.launch {
            _isBusy.value = true
            val result = if (mediaType == "story") {
                repository.createStory(currentProfile, mediaKey, caption).map { it }
            } else {
                repository.createPost(currentProfile, caption, mediaType, mediaKey, location)
            }
            _isBusy.value = false
            if (result.isSuccess) {
                _statusMessages.tryEmit(if (mediaType == "story") "Story added to your ring!" else "Published to ARK SOCIAL!")
                onPublished()
            } else {
                _statusMessages.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Could not publish")
            }
        }
    }

    fun updateProfile(
        username: String,
        displayName: String,
        bio: String,
        website: String,
        avatarKey: String,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateUserProfile(username, displayName, bio, website, avatarKey)
            _isBusy.value = false
            if (result.isSuccess) {
                _statusMessages.tryEmit("Profile updated")
                onSaved()
            } else {
                _statusMessages.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Unable to update profile")
            }
        }
    }

    fun updatePrivacySettings(
        privateAccount: Boolean,
        pushLikes: Boolean,
        pushComments: Boolean,
        pushFollows: Boolean,
        showActivityStatus: Boolean
    ) {
        viewModelScope.launch {
            val result = repository.updatePrivateSettings(
                privateAccount = privateAccount,
                pushLikes = pushLikes,
                pushComments = pushComments,
                pushFollows = pushFollows,
                showActivityStatus = showActivityStatus
            )
            if (result.isFailure) {
                _statusMessages.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to update settings")
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    fun sendDirectMessage(targetUser: UserProfile, text: String) {
        val currentProfile = (userProfile.value as? UiState.Success)?.data ?: UserProfile(
            userId = currentUserId,
            username = "ark_creator",
            displayName = "ARK Creator"
        )
        viewModelScope.launch {
            val result = repository.sendDirectMessage(currentProfile, targetUser, text)
            if (result.isSuccess) {
                _selectedConversationId.value = result.getOrNull()
            } else {
                _statusMessages.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to send message")
            }
        }
    }

    private fun <T> Flow<UiState<T>>.unwrapFirestoreErrors(label: String): Flow<UiState<T>> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing $label", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Unable to sync $label"))
        }

    private fun handleStreamCancellationOrLog(error: Throwable, label: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Stream error on $label", firestoreError ?: error)
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}

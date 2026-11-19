package com.example.data

import android.content.Context
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.Comment
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.FollowingRecord
import com.example.data.model.Post
import com.example.data.model.PrivateAccountSettings
import com.example.data.model.SavedPost
import com.example.data.model.Story
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ArkSocialRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeUsername(raw: String, fallbackUid: String): String {
        val cleaned = raw.lowercase()
            .replace(Regex("[^a-z0-9_.]"), "")
            .trim('.')
        return when {
            cleaned.length >= 3 -> cleaned.take(28)
            else -> "ark_${fallbackUid.take(6).lowercase()}"
        }
    }

    suspend fun ensureUserProfileInitialized(
        user: FirebaseUser,
        preferredUsername: String? = null,
        preferredDisplayName: String? = null,
        preferredBio: String? = null
    ): Result<Unit> {
        val uid = user.uid
        val profilePath = "users/$uid"
        return try {
            val userRef = db.collection("users").document(uid)
            val existingSnap = userRef.get().await()
            if (!existingSnap.exists()) {
                val baseDisplay = preferredDisplayName?.trim()?.take(50).orEmpty()
                    .ifBlank { user.displayName?.trim()?.take(50).orEmpty() }
                    .ifBlank { "ARK Creator" }
                val baseUsername = sanitizeUsername(
                    preferredUsername?.trim().orEmpty()
                        .ifBlank { user.email?.substringBefore("@") ?: baseDisplay },
                    uid
                )
                val bio = preferredBio?.trim()?.take(200).orEmpty()
                    .ifBlank { "Creating & sharing moments on ARK SOCIAL." }

                val profilePayload = mapOf(
                    "userId" to uid,
                    "username" to baseUsername,
                    "displayName" to baseDisplay,
                    "bio" to bio,
                    "avatarKey" to "preset_hero",
                    "website" to "",
                    "discoverable" to true,
                    "postsCount" to 0L,
                    "followersCount" to 0L,
                    "followingCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(profilePayload).await()

                val privatePayload = mapOf(
                    "userId" to uid,
                    "email" to (user.email?.take(150) ?: ""),
                    "privateAccount" to false,
                    "pushLikes" to true,
                    "pushComments" to true,
                    "pushFollows" to true,
                    "showActivityStatus" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.collection("private").document("account").set(privatePayload).await()

                val welcomeNotifId = "welcome_${uid.take(8)}"
                val notifPayload = mapOf(
                    "userId" to uid,
                    "actorId" to uid,
                    "actorUsername" to "ark.design",
                    "type" to "welcome",
                    "message" to "Welcome to ARK SOCIAL! Customize your profile or share your first post.",
                    "unread" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                userRef.collection("notifications").document(welcomeNotifId).set(notifPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, profilePath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.GET, path, userId); throw error }
    }

    fun observePrivateSettings(userId: String): Flow<PrivateAccountSettings?> {
        val path = "users/$userId/private/account"
        return db.collection("users").document(userId).collection("private").document("account")
            .snapshots()
            .map { it.toObject(PrivateAccountSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.GET, path, userId); throw error }
    }

    fun observePublicPosts(currentUserId: String): Flow<List<Post>> {
        val path = "posts"
        return db.collection("posts")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error -> logFlowError(error, OperationType.LIST, path, currentUserId); throw error }
    }

    fun observeUserPosts(userId: String): Flow<List<Post>> {
        val path = "posts"
        return db.collection("posts")
            .whereEqualTo("authorId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
    }

    fun observePublicStories(currentUserId: String): Flow<List<Story>> {
        val path = "stories"
        return db.collection("stories")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error -> logFlowError(error, OperationType.LIST, path, currentUserId); throw error }
    }

    fun observeDiscoverableUsers(currentUserId: String): Flow<List<UserProfile>> {
        val path = "users"
        return db.collection("users")
            .whereEqualTo("discoverable", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error -> logFlowError(error, OperationType.LIST, path, currentUserId); throw error }
    }

    fun observeLikedPostIds(userId: String): Flow<Set<String>> {
        val path = "users/$userId/likedPosts"
        return db.collection("users").document(userId).collection("likedPosts")
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
            .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
    }

    fun observeSavedPosts(userId: String): Flow<List<SavedPost>> {
        val path = "users/$userId/savedPosts"
        return db.collection("users").document(userId).collection("savedPosts")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SavedPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
    }

    fun observeFollowing(userId: String): Flow<List<FollowingRecord>> {
        val path = "users/$userId/following"
        return db.collection("users").document(userId).collection("following")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FollowingRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
    }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> {
        val path = "users/$userId/notifications"
        return db.collection("users").document(userId).collection("notifications")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
    }

    fun observeComments(postId: String, currentUserId: String): Flow<List<Comment>> {
        if (postId.startsWith("showcase_")) {
            return flow { emit(emptyList()) }
        }
        val path = "posts/$postId/comments"
        return db.collection("posts").document(postId).collection("comments")
            .whereEqualTo("postVisibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Comment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error -> logFlowError(error, OperationType.LIST, path, currentUserId); throw error }
    }

    fun observeConversations(userId: String): Flow<List<Conversation>> {
        val path = "conversations"
        return db.collection("conversations")
            .whereArrayContains("participantIds", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
            }
            .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
    }

    fun observeMessages(conversationId: String, userId: String): Flow<List<DirectMessage>> {
        val path = "conversations/$conversationId/messages"
        return db.collection("conversations").document(conversationId).collection("messages")
            .whereArrayContains("participantIds", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DirectMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
    }

    suspend fun updateUserProfile(
        username: String,
        displayName: String,
        bio: String,
        website: String,
        avatarKey: String
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val cleanUsername = username.trim().lowercase()
            val cleanDisplay = displayName.trim()
            val cleanBio = bio.trim()
            val cleanWebsite = website.trim()

            require(cleanUsername.length in 3..30) { "Username must be between 3 and 30 characters." }
            require(cleanDisplay.length in 1..50) { "Display name must be between 1 and 50 characters." }
            require(cleanBio.length <= 200) { "Bio cannot exceed 200 characters." }
            require(cleanWebsite.length <= 120) { "Website cannot exceed 120 characters." }

            val docRef = db.collection("users").document(uid)
            path = docRef.path
            docRef.update(
                mapOf(
                    "username" to cleanUsername,
                    "displayName" to cleanDisplay,
                    "bio" to cleanBio,
                    "website" to cleanWebsite,
                    "avatarKey" to avatarKey.ifBlank { "preset_hero" },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePrivateSettings(
        privateAccount: Boolean,
        pushLikes: Boolean,
        pushComments: Boolean,
        pushFollows: Boolean,
        showActivityStatus: Boolean
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("private").document("account")
            path = docRef.path
            docRef.update(
                mapOf(
                    "privateAccount" to privateAccount,
                    "pushLikes" to pushLikes,
                    "pushComments" to pushComments,
                    "pushFollows" to pushFollows,
                    "showActivityStatus" to showActivityStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createPost(
        profile: UserProfile,
        caption: String,
        mediaType: String,
        mediaKey: String,
        location: String
    ): Result<String> {
        var path = "posts"
        return try {
            val uid = requireUserId()
            val cleanCaption = caption.trim()
            val cleanLocation = location.trim()
            val validMediaType = if (mediaType == "reel") "reel" else "photo"
            require(cleanCaption.length <= 1000) { "Caption cannot exceed 1000 characters." }
            require(mediaKey.isNotBlank() && mediaKey.length <= 500) { "Please select media to share." }
            require(cleanLocation.length <= 80) { "Location cannot exceed 80 characters." }

            val postId = "post_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val docRef = db.collection("posts").document(postId)
            path = docRef.path

            val payload = mapOf(
                "authorId" to uid,
                "authorUsername" to profile.username.ifBlank { "ark_creator" }.take(30),
                "authorDisplayName" to profile.displayName.ifBlank { "ARK Creator" }.take(50),
                "authorAvatarKey" to profile.avatarKey.ifBlank { "preset_hero" },
                "caption" to cleanCaption,
                "mediaType" to validMediaType,
                "mediaKey" to mediaKey.take(500),
                "location" to cleanLocation,
                "visibility" to "public",
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            db.collection("users").document(uid).update(
                mapOf(
                    "postsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(postId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createStory(
        profile: UserProfile,
        mediaKey: String,
        caption: String
    ): Result<String> {
        var path = "stories"
        return try {
            val uid = requireUserId()
            val cleanCaption = caption.trim().take(200)
            require(mediaKey.isNotBlank() && mediaKey.length <= 500) { "Select an image for your story." }

            val storyId = "story_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val docRef = db.collection("stories").document(storyId)
            path = docRef.path
            docRef.set(
                mapOf(
                    "authorId" to uid,
                    "authorUsername" to profile.username.ifBlank { "ark_creator" }.take(30),
                    "authorAvatarKey" to profile.avatarKey.ifBlank { "preset_hero" },
                    "mediaKey" to mediaKey.take(500),
                    "caption" to cleanCaption,
                    "visibility" to "public",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(storyId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleLike(post: Post, currentlyLiked: Boolean): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            require(post.id.isNotBlank()) { "Post ID required" }
            val likeRef = db.collection("users").document(uid).collection("likedPosts").document(post.id)
            path = likeRef.path

            if (currentlyLiked) {
                likeRef.delete().await()
                if (!post.isShowcase) {
                    db.collection("posts").document(post.id).update(
                        mapOf(
                            "likesCount" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } else {
                likeRef.set(
                    mapOf(
                        "userId" to uid,
                        "postId" to post.id,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                if (!post.isShowcase) {
                    db.collection("posts").document(post.id).update(
                        mapOf(
                            "likesCount" to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleSave(post: Post, currentlySaved: Boolean): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            require(post.id.isNotBlank()) { "Post ID required" }
            val saveRef = db.collection("users").document(uid).collection("savedPosts").document(post.id)
            path = saveRef.path

            if (currentlySaved) {
                saveRef.delete().await()
            } else {
                saveRef.set(
                    mapOf(
                        "userId" to uid,
                        "postId" to post.id,
                        "authorUsername" to post.authorUsername.ifBlank { "creator" }.take(30),
                        "caption" to post.caption.take(1000),
                        "mediaKey" to post.mediaKey.ifBlank { "preset_arch" }.take(500),
                        "mediaType" to if (post.mediaType == "reel") "reel" else "photo",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun addComment(profile: UserProfile, post: Post, text: String): Result<Unit> {
        var path = "posts/${post.id}/comments"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim()
            require(cleanText.isNotEmpty() && cleanText.length <= 500) { "Comment must be 1 to 500 characters." }
            require(!post.isShowcase) { "Comments on featured showcase posts stay local; post on live community posts to sync." }

            val commentId = "c_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val commentRef = db.collection("posts").document(post.id).collection("comments").document(commentId)
            path = commentRef.path

            commentRef.set(
                mapOf(
                    "postId" to post.id,
                    "postVisibility" to post.visibility.ifBlank { "public" },
                    "authorId" to uid,
                    "authorUsername" to profile.username.ifBlank { "ark_creator" }.take(30),
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("posts").document(post.id).update(
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFollow(target: UserProfile, currentlyFollowing: Boolean): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val targetId = target.userId.ifBlank { target.id }
            require(targetId.isNotBlank() && targetId != uid) { "Cannot follow this account." }
            val followRef = db.collection("users").document(uid).collection("following").document(targetId)
            path = followRef.path

            if (currentlyFollowing) {
                followRef.delete().await()
                db.collection("users").document(uid).update(
                    mapOf(
                        "followingCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                followRef.set(
                    mapOf(
                        "userId" to uid,
                        "targetUserId" to targetId,
                        "targetUsername" to target.username.ifBlank { "creator" }.take(30),
                        "targetDisplayName" to target.displayName.ifBlank { "Creator" }.take(50),
                        "targetAvatarKey" to target.avatarKey.ifBlank { "preset_arch" }.take(500),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                db.collection("users").document(uid).update(
                    mapOf(
                        "followingCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("notifications").document(notificationId)
            path = docRef.path
            docRef.update(mapOf("unread" to false)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendDirectMessage(
        senderProfile: UserProfile,
        targetUser: UserProfile,
        text: String
    ): Result<String> {
        var path = "conversations"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim()
            require(cleanText.isNotEmpty() && cleanText.length <= 1000) { "Message must be 1 to 1000 characters." }

            val targetId = targetUser.userId.ifBlank { targetUser.id }
            val participants = listOf(uid, targetId).sorted()
            val conversationId = "${participants[0]}_${participants[1]}".take(120)
            val convRef = db.collection("conversations").document(conversationId)
            path = convRef.path

            val existing = convRef.get().await()
            if (!existing.exists()) {
                convRef.set(
                    mapOf(
                        "participantIds" to participants,
                        "participantLabel" to "${senderProfile.displayName} & ${targetUser.displayName}".take(100),
                        "lastMessage" to cleanText.take(300),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                convRef.update(
                    mapOf(
                        "lastMessage" to cleanText.take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val messageId = "m_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val msgRef = convRef.collection("messages").document(messageId)
            msgRef.set(
                mapOf(
                    "conversationId" to conversationId,
                    "participantIds" to participants,
                    "senderId" to uid,
                    "senderUsername" to senderProfile.username.ifBlank { "ark_creator" }.take(30),
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(conversationId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun logFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}

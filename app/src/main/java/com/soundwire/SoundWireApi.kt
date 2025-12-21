package com.soundwire

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface SoundWireApi {

    // ---------- AUTH ----------

    @POST("auth/register")
    suspend fun register(@Body req: RegisterRequest): ApiOkResponse

    @POST("auth/login")
    suspend fun login(@Body req: LoginRequest): AuthResponse

    // ---------- USERS ----------

    @GET("users/me")
    suspend fun me(): User

    @PUT("users/me")
    suspend fun updateMe(@Body req: UpdateProfileRequest): User

    @Multipart
    @POST("users/me/avatar")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): UploadAvatarResponse

    @GET("users/list")
    suspend fun listUsers(): List<User>

    @GET("users/search")
    suspend fun searchUsers(@Query("q") q: String): List<User>

    // ---------- CONTACTS ----------

    @GET("contacts")
    suspend fun listContacts(): List<User>

    @POST("contacts/request")
    suspend fun sendContactRequest(@Body req: SendContactRequest): SendContactRequestResponse

    @GET("contacts/requests/incoming")
    suspend fun incomingRequests(): List<ContactRequest>

    @GET("contacts/requests/outgoing")
    suspend fun outgoingRequests(): List<ContactRequest>

    @POST("contacts/requests/{id}/accept")
    suspend fun acceptRequest(@Path("id") id: Int): ApiOkResponse

    @POST("contacts/requests/{id}/decline")
    suspend fun declineRequest(@Path("id") id: Int): ApiOkResponse

    @DELETE("contacts/{contactId}")
    suspend fun removeContact(@Path("contactId") contactId: Int): ApiOkResponse

    // ---------- CHATS ----------

    @GET("chats")
    suspend fun listChats(): List<ChatSummary>

    @POST("chats/private")
    suspend fun createOrGetPrivateChat(@Body req: CreateChatRequest): ChatSummary

    @POST("chats/group")
    suspend fun createGroupChat(@Body req: CreateGroupChatRequest): ChatSummary

    @GET("chats/{chatId}/participants")
    suspend fun getParticipants(@Path("chatId") chatId: Int): List<ChatParticipant>

    @POST("chats/{chatId}/participants")
    suspend fun addParticipants(
        @Path("chatId") chatId: Int,
        @Body req: AddParticipantsRequest
    ): ApiOkResponse

    @Multipart
    @POST("chats/{chatId}/avatar")
    suspend fun uploadChatAvatar(
        @Path("chatId") chatId: Int,
        @Part avatar: MultipartBody.Part
    ): UploadChatAvatarResponse

    @GET("chats/{chatId}/messages")
    suspend fun getMessages(
        @Path("chatId") chatId: Int,
        @Query("limit") limit: Int = 50,
        @Query("beforeId") beforeId: Long? = null
    ): List<Message>

    @Multipart
    @POST("chats/{chatId}/messages")
    suspend fun sendMessageWithFile(
        @Path("chatId") chatId: Int,
        @Part("type") type: RequestBody,
        @Part("text") text: RequestBody?,
        @Part("durationMs") durationMs: RequestBody?,
        @Part file: MultipartBody.Part
    ): Message

    // ---------- MUSIC (server library) ----------

    @GET("music/list")
    suspend fun listServerTracks(): List<TrackDto>
}

/** То, что отдаёт backend по /music/list */
data class TrackDto(
    val id: String,
    val title: String,
    val artist: String? = null,
    val url: String
)

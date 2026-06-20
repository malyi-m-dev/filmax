package com.filmax.data.user.remote

import com.filmax.data.catalog.remote.dto.ItemsResponseDto
import com.filmax.data.user.remote.dto.AccountInfoDto
import com.filmax.data.user.remote.dto.BookmarkFolderDto
import com.filmax.data.user.remote.dto.BookmarksDto
import com.filmax.data.user.remote.dto.DeviceSettingsDto
import com.filmax.data.user.remote.dto.FolderStatusDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface UserApi {

    @GET("api/v1/user")
    suspend fun getAccountInfo(): AccountInfoDto

    @GET("api/v1/device/info")
    suspend fun getDeviceSettings(): DeviceSettingsDto

    @FormUrlEncoded
    @POST("api/v1/device/{id}/settings")
    suspend fun updateDeviceSettings(
        @Path("id")                id: Int,
        @Field("supportSsl")       supportSsl: Int,
        @Field("supportHevc")      supportHevc: Int,
        @Field("supportHdr")       supportHdr: Int,
        @Field("support4k")        support4k: Int,
        @Field("mixedPlaylist")    mixedPlaylist: Int,
        @Field("streamingType")    streamingType: Int,
        @Field("serverLocation")   serverLocation: Int,
    ): DeviceSettingsDto

    @FormUrlEncoded
    @POST("api/v1/device/notify")
    suspend fun registerDevice(
        @Field("title")    title: String,
        @Field("hardware") hardware: String,
        @Field("software") software: String,
    )

    @GET("api/v1/bookmarks")
    suspend fun getBookmarks(): BookmarksDto

    @GET("api/v1/bookmarks/{id}")
    suspend fun getBookmarkItems(
        @Path("id")    id: Int,
        @Query("page") page: Int,
    ): ItemsResponseDto

    @FormUrlEncoded
    @POST("api/v1/bookmarks/create")
    suspend fun createBookmark(@Field("title") title: String): FolderStatusDto

    @FormUrlEncoded
    @POST("api/v1/bookmarks/remove-folder")
    suspend fun deleteBookmark(@Field("folder") folderId: Int)

    @FormUrlEncoded
    @POST("api/v1/bookmarks/add")
    suspend fun addBookmarkItem(
        @Field("item")   itemId: Int,
        @Field("folder") folderId: Int,
    )

    @FormUrlEncoded
    @POST("api/v1/bookmarks/remove-item")
    suspend fun removeBookmarkItem(
        @Field("item")   itemId: Int,
        @Field("folder") folderId: Int,
    )
}

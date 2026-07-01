package com.filmax.data.user.remote

import com.filmax.data.catalog.remote.dto.ItemsResponseDto
import com.filmax.data.user.remote.dto.AccountInfoDto
import com.filmax.data.user.remote.dto.BookmarksDto
import com.filmax.data.user.remote.dto.DeviceSettingsDto
import com.filmax.data.user.remote.dto.FolderStatusDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.Parameters

internal class UserApi(private val client: HttpClient) {

    suspend fun getAccountInfo(): AccountInfoDto =
        client.get("api/v1/user").body()

    suspend fun getDeviceSettings(): DeviceSettingsDto =
        client.get("api/v1/device/info").body()

    suspend fun updateDeviceSettings(params: UpdateDeviceSettingsParams): DeviceSettingsDto =
        client.submitForm(
            url = "api/v1/device/${params.id}/settings",
            formParameters = Parameters.build {
                append("supportSsl", params.supportSsl.toString())
                append("supportHevc", params.supportHevc.toString())
                append("supportHdr", params.supportHdr.toString())
                append("support4k", params.support4k.toString())
                append("mixedPlaylist", params.mixedPlaylist.toString())
                append("streamingType", params.streamingType.toString())
                append("serverLocation", params.serverLocation.toString())
            },
        ).body()

    suspend fun registerDevice(title: String, hardware: String, software: String) {
        client.submitForm(
            url = "api/v1/device/notify",
            formParameters = Parameters.build {
                append("title", title)
                append("hardware", hardware)
                append("software", software)
            },
        )
    }

    suspend fun getBookmarks(): BookmarksDto =
        client.get("api/v1/bookmarks").body()

    suspend fun getBookmarkItems(id: Int, page: Int): ItemsResponseDto =
        client.get("api/v1/bookmarks/$id") {
            parameter("page", page)
        }.body()

    suspend fun createBookmark(title: String): FolderStatusDto =
        client.submitForm(
            url = "api/v1/bookmarks/create",
            formParameters = Parameters.build { append("title", title) },
        ).body()

    suspend fun deleteBookmark(folderId: Int) {
        client.submitForm(
            url = "api/v1/bookmarks/remove-folder",
            formParameters = Parameters.build { append("folder", folderId.toString()) },
        )
    }

    suspend fun addBookmarkItem(itemId: Int, folderId: Int) {
        client.submitForm(
            url = "api/v1/bookmarks/add",
            formParameters = Parameters.build {
                append("item", itemId.toString())
                append("folder", folderId.toString())
            },
        )
    }

    suspend fun removeBookmarkItem(itemId: Int, folderId: Int) {
        client.submitForm(
            url = "api/v1/bookmarks/remove-item",
            formParameters = Parameters.build {
                append("item", itemId.toString())
                append("folder", folderId.toString())
            },
        )
    }
}

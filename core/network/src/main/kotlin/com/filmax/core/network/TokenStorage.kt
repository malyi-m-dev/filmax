package com.filmax.core.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore("filmax_tokens")

class TokenStorage(
    private val context: Context,
) {
    private val store get() = context.tokenDataStore

    val accessToken: Flow<String?> = store.data.map { it[KEY_ACCESS] }
    val refreshToken: Flow<String?> = store.data.map { it[KEY_REFRESH] }

    suspend fun getAccessToken(): String? = accessToken.firstOrNull()

    suspend fun save(accessToken: String, refreshToken: String) {
        store.edit {
            it[KEY_ACCESS] = accessToken
            it[KEY_REFRESH] = refreshToken
        }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }

    private companion object {
        val KEY_ACCESS = stringPreferencesKey("access_token")
        val KEY_REFRESH = stringPreferencesKey("refresh_token")
    }
}

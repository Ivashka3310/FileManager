package com.example.filemanager.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.filemanager.domain.interfaces.SessionStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")
class SessionStorageImpl(private val context: Context) : SessionStorage{
    private val URL = stringPreferencesKey("server_domain")
    private val JWT_TOKEN = stringPreferencesKey("jwt_token")

    override val url: Flow<String?> = context.sessionDataStore.data.map { it[URL] }
    override val accessToken: Flow<String?> = context.sessionDataStore.data.map { it[JWT_TOKEN] }

    override suspend fun saveUrl(newUrl: String) {
        context.sessionDataStore.edit { it[URL] = newUrl }
    }

    override suspend fun saveToken(token: String) {
        context.sessionDataStore.edit { it[JWT_TOKEN] = token }
    }

    override suspend fun clearSession() {
        context.sessionDataStore.edit { it.clear() }
    }
}
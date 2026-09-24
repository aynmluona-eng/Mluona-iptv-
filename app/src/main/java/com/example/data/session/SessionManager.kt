package com.example.data.session

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AccountSession
import com.example.data.model.AccountType
import org.json.JSONArray
import org.json.JSONObject

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mluona_iptv_sessions", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCOUNTS = "key_saved_accounts"
        private const val KEY_ACTIVE_ACCOUNT_ID = "key_active_account_id"
        private const val KEY_APP_LANGUAGE = "key_app_language"
        private const val KEY_STREAM_FORMAT = "key_stream_format"
        private const val KEY_AUDIO_TYPE = "key_audio_type"
    }

    fun getLanguage(): String = prefs.getString(KEY_APP_LANGUAGE, "en") ?: "en"

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_APP_LANGUAGE, lang).apply()
    }

    fun getStreamFormat(): String = prefs.getString(KEY_STREAM_FORMAT, "m3u8") ?: "m3u8"

    fun setStreamFormat(format: String) {
        prefs.edit().putString(KEY_STREAM_FORMAT, format).apply()
    }

    fun getAudioType(): String = prefs.getString(KEY_AUDIO_TYPE, "auto") ?: "auto"

    fun setAudioType(type: String) {
        prefs.edit().putString(KEY_AUDIO_TYPE, type).apply()
    }

    fun saveAccount(account: AccountSession) {
        val accounts = getAllAccounts().toMutableList()
        val index = accounts.indexOfFirst { it.id == account.id }
        if (index >= 0) {
            accounts[index] = account
        } else {
            accounts.add(0, account)
        }
        persistAccounts(accounts)
        setActiveAccountId(account.id)
    }

    fun getActiveAccount(): AccountSession? {
        val activeId = prefs.getString(KEY_ACTIVE_ACCOUNT_ID, null) ?: return getAllAccounts().firstOrNull()
        return getAllAccounts().find { it.id == activeId } ?: getAllAccounts().firstOrNull()
    }

    fun setActiveAccountId(id: String?) {
        prefs.edit().putString(KEY_ACTIVE_ACCOUNT_ID, id).apply()
    }

    fun getAllAccounts(): List<AccountSession> {
        val jsonString = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<AccountSession>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    AccountSession(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        type = try {
                            AccountType.valueOf(obj.optString("type", AccountType.XTREAM.name))
                        } catch (_: Exception) {
                            AccountType.XTREAM
                        },
                        serverUrl = obj.optString("serverUrl"),
                        username = obj.optString("username"),
                        password = obj.optString("password"),
                        m3uUrl = obj.optString("m3uUrl"),
                        status = obj.optString("status", "Active"),
                        expDate = obj.optString("expDate", ""),
                        maxConnections = obj.optString("maxConnections", "1"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        lastActiveAt = obj.optLong("lastActiveAt", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun deleteAccount(id: String) {
        val accounts = getAllAccounts().filter { it.id != id }
        persistAccounts(accounts)
        if (getActiveAccount()?.id == id) {
            setActiveAccountId(accounts.firstOrNull()?.id)
        }
    }

    fun clearActiveSession() {
        setActiveAccountId(null)
    }

    private fun persistAccounts(accounts: List<AccountSession>) {
        val jsonArray = JSONArray()
        for (account in accounts) {
            val obj = JSONObject().apply {
                put("id", account.id)
                put("name", account.name)
                put("type", account.type.name)
                put("serverUrl", account.serverUrl)
                put("username", account.username)
                put("password", account.password)
                put("m3uUrl", account.m3uUrl)
                put("status", account.status)
                put("expDate", account.expDate)
                put("maxConnections", account.maxConnections)
                put("createdAt", account.createdAt)
                put("lastActiveAt", account.lastActiveAt)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_ACCOUNTS, jsonArray.toString()).apply()
    }
}

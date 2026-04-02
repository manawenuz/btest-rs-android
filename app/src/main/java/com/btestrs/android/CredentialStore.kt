package com.btestrs.android

import android.content.Context
import android.content.SharedPreferences

data class SavedCredential(
    val host: String,
    val username: String,
    val password: String
) {
    val displayName: String get() = "$username@$host"
}

class CredentialStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("credentials", Context.MODE_PRIVATE)

    fun save(credential: SavedCredential) {
        val all = loadAll().toMutableList()
        // Replace existing entry for same host+user, or add new
        val idx = all.indexOfFirst { it.host == credential.host && it.username == credential.username }
        if (idx >= 0) {
            all[idx] = credential
        } else {
            all.add(credential)
        }
        writeAll(all)
        prefs.edit().putString("last_host", credential.host)
            .putString("last_user", credential.username).apply()
    }

    fun delete(credential: SavedCredential) {
        val all = loadAll().toMutableList()
        all.removeAll { it.host == credential.host && it.username == credential.username }
        writeAll(all)
    }

    fun loadAll(): List<SavedCredential> {
        val count = prefs.getInt("count", 0)
        return (0 until count).mapNotNull { i ->
            val host = prefs.getString("host_$i", null) ?: return@mapNotNull null
            val user = prefs.getString("user_$i", null) ?: return@mapNotNull null
            val pass = prefs.getString("pass_$i", null) ?: return@mapNotNull null
            SavedCredential(host, user, pass)
        }
    }

    fun loadLast(): SavedCredential? {
        val host = prefs.getString("last_host", null) ?: return null
        val user = prefs.getString("last_user", null) ?: return null
        return loadAll().find { it.host == host && it.username == user }
    }

    private fun writeAll(list: List<SavedCredential>) {
        val editor = prefs.edit()
        // Clear old entries
        val oldCount = prefs.getInt("count", 0)
        for (i in 0 until oldCount) {
            editor.remove("host_$i").remove("user_$i").remove("pass_$i")
        }
        // Write new
        editor.putInt("count", list.size)
        list.forEachIndexed { i, c ->
            editor.putString("host_$i", c.host)
            editor.putString("user_$i", c.username)
            editor.putString("pass_$i", c.password)
        }
        editor.apply()
    }
}

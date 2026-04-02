package com.btestrs.android

import android.content.Context
import android.content.SharedPreferences

data class RendererConfig(
    val url: String = "https://btest-rs-web.vercel.app",
    val apiKey: String = "",
    val syncEnabled: Boolean = false
) {
    val isConfigured: Boolean get() = url.isNotBlank() && apiKey.isNotBlank()
}

class RendererSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("renderer", Context.MODE_PRIVATE)

    fun load(): RendererConfig = RendererConfig(
        url = prefs.getString("url", "https://btest-rs-web.vercel.app") ?: "https://btest-rs-web.vercel.app",
        apiKey = prefs.getString("api_key", "") ?: "",
        syncEnabled = prefs.getBoolean("sync_enabled", false)
    )

    fun save(config: RendererConfig) {
        prefs.edit()
            .putString("url", config.url.trimEnd('/'))
            .putString("api_key", config.apiKey)
            .putBoolean("sync_enabled", config.syncEnabled)
            .apply()
    }
}

package es.sasogu.minilauncher

import android.content.Context

data class UsageBlock(
    val packageName: String,
    val appLabel: String,
    val minutes: Int,
)

object UsageBlockStore {
    private const val PREFS_NAME = "usage_block"
    private const val KEY_PACKAGE_NAME = "package_name"
    private const val KEY_APP_LABEL = "app_label"
    private const val KEY_MINUTES = "minutes"

    fun markBlocked(context: Context, packageName: String, appLabel: String, minutes: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PACKAGE_NAME, packageName)
            .putString(KEY_APP_LABEL, appLabel)
            .putInt(KEY_MINUTES, minutes)
            .apply()
    }

    fun loadBlocked(context: Context): UsageBlock? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val packageName = prefs.getString(KEY_PACKAGE_NAME, null) ?: return null
        val appLabel = prefs.getString(KEY_APP_LABEL, null).orEmpty().ifBlank { "esta app" }
        val minutes = prefs.getInt(KEY_MINUTES, 0)
        return UsageBlock(packageName = packageName, appLabel = appLabel, minutes = minutes)
    }

    fun isBlocked(context: Context, packageName: String): Boolean {
        return loadBlocked(context)?.packageName == packageName
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}

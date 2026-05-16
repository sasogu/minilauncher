package es.sasogu.minilauncher

import android.content.Context
import java.time.LocalDate

object UnlimitedDayStore {
    private const val PREFS_NAME = "unlimited_day"

    fun markUnlimitedToday(context: Context, packageName: String) {
        val today = LocalDate.now().toString()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(packageName, today)
            .apply()
    }

    fun isUnlimitedToday(context: Context, packageName: String): Boolean {
        val today = LocalDate.now().toString()
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(packageName, null)
        return stored == today
    }
}

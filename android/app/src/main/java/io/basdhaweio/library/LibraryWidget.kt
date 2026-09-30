package io.basdhaweio.library

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

// Home-screen widget (docs/WIDGET.md): the system asks for an update every
// 30 minutes (widget_library_info.xml); every update, resize, chart tap or
// refresh tap runs WidgetWorker, which fetches data/widget.json and the
// chosen chart PNG and redraws every instance. Nothing here writes anywhere:
// the buttons open MainActivity on a page URL and the page does the work.
class LibraryWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetWorker.enqueue(context)
    }

    override fun onEnabled(context: Context) {
        WidgetWorker.enqueue(context)
    }

    // No onAppWidgetOptionsChanged: some launchers (Samsung) fire it after
    // every redraw, and redrawing in response made the widget blink forever
    // (John, 2026-09-30). The layout is the same at any size anyway.

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_NEXT_CHART -> {
                Prefs.nextChart(context)
                WidgetWorker.enqueue(context, force = true)
            }
            ACTION_REFRESH -> WidgetWorker.enqueue(context, force = true)
        }
    }

    companion object {
        const val ACTION_NEXT_CHART = "io.basdhaweio.library.widget.NEXT_CHART"
        const val ACTION_REFRESH = "io.basdhaweio.library.widget.REFRESH"
    }
}

object Prefs {
    private const val FILE = "widget"
    private const val KEY_CHART = "chart"
    private const val KEY_JSON = "last_json"
    private const val KEY_LAST_RUN = "last_run"

    fun chartIndex(context: Context): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY_CHART, 0)

    fun lastJson(context: Context): String? =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_JSON, null)

    fun saveJson(context: Context, json: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY_JSON, json).apply()
    }

    fun lastRun(context: Context): Long =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getLong(KEY_LAST_RUN, 0L)

    fun setLastRun(context: Context, at: Long) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putLong(KEY_LAST_RUN, at).apply()
    }

    fun nextChart(context: Context) {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        p.edit().putInt(KEY_CHART, (p.getInt(KEY_CHART, 0) + 1) % WidgetWorker.CHARTS.size).apply()
    }
}

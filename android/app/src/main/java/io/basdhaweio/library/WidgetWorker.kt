package io.basdhaweio.library

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Fetches the widget food jerry publishes beside the bundle and redraws the
// widget. Runs on a WorkManager thread so the fetch never blocks a receiver.
class WidgetWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val ctx = applicationContext
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, LibraryWidget::class.java))
        if (ids.isEmpty()) return Result.success()

        // Offline keeps the last good JSON and chart instead of blanking the widget.
        var offline = false
        val jsonText = try {
            fetchText("$SITE_DATA/widget.json").also { Prefs.saveJson(ctx, it) }
        } catch (e: Exception) {
            offline = true
            Prefs.lastJson(ctx)
        }
        val data = jsonText?.let { try { JSONObject(it) } catch (e: Exception) { null } }
        val night = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val chart = CHARTS[Prefs.chartIndex(ctx) % CHARTS.size]
        val suffix = if (night) "-dark" else ""
        val cache = File(ctx.cacheDir, "widget-${chart.first}$suffix.png")
        val bitmap = try {
            fetchBitmap("$SITE_DATA/widget/${chart.first}$suffix.png").also { bmp ->
                try { FileOutputStream(cache).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } } catch (e: Exception) { }
            }
        } catch (e: Exception) {
            if (cache.exists()) BitmapFactory.decodeFile(cache.path) else null
        }

        val views = buildViews(ctx, data, bitmap, chart, offline)
        for (id in ids) mgr.updateAppWidget(id, views)
        return if (!offline) Result.success() else Result.retry()
    }

    private fun buildViews(ctx: Context, data: JSONObject?, bitmap: Bitmap?, chart: Pair<String, String>, offline: Boolean): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_library)

        // quick actions: each opens the app on a page URL (docs/WIDGET.md)
        v.setOnClickPendingIntent(R.id.btn_scan, openPage(ctx, 1, "#scan"))
        v.setOnClickPendingIntent(R.id.btn_add, openPage(ctx, 2, "#add"))
        v.setOnClickPendingIntent(R.id.btn_order, openPage(ctx, 3, "#order"))
        v.setOnClickPendingIntent(R.id.btn_read, openPage(ctx, 4, "#tbr"))
        v.setOnClickPendingIntent(R.id.chart, broadcast(ctx, 10, LibraryWidget.ACTION_NEXT_CHART))
        v.setOnClickPendingIntent(R.id.status, broadcast(ctx, 11, LibraryWidget.ACTION_REFRESH))

        val year = data?.optJSONObject("year")
        val reading = data?.optJSONArray("reading_now")
        val orders = data?.optJSONObject("orders")
        val lines = orders?.optJSONArray("lines")

        v.setTextViewText(
            R.id.reading,
            if (reading != null && reading.length() > 0)
                "Reading: " + reading.getJSONObject(0).optString("title") +
                    (if (reading.length() > 1) "  (+${reading.length() - 1})" else "")
            else "Reading: —"
        )
        v.setTextViewText(
            R.id.year,
            if (year != null)
                "${year.optInt("read")} read · pace ${year.optInt("pace")} · " +
                    "${year.optInt("last_year_by_now")} this time last year"
            else "The Library — pull to refresh"
        )
        v.setOnClickPendingIntent(R.id.reading, openPage(ctx, 5, "#tbr"))
        v.setOnClickPendingIntent(R.id.year, openPage(ctx, 6, "#reading/thisyear"))

        val rowIds = intArrayOf(R.id.order1, R.id.order2, R.id.order3)
        for ((i, id) in rowIds.withIndex()) {
            val line = if (lines != null && i < lines.length()) lines.getJSONObject(i) else null
            if (line == null) {
                v.setTextViewText(id, if (i == 0) (if (orders != null) "Nothing on order" else "") else "")
                v.setOnClickPendingIntent(id, openPage(ctx, 20 + i, "#order"))
                continue
            }
            val status = line.optString("status")
            val eta = line.optString("eta")
            val store = line.optString("store")
            val tail = listOf(
                if (status == "shipped") "shipped" else "",
                if (eta.isNotEmpty()) "due $eta" else "",
                store
            ).filter { it.isNotEmpty() }.joinToString(" · ")
            v.setTextViewText(id, "📦 " + line.optString("title") + if (tail.isNotEmpty()) "  ·  $tail" else "")
            v.setOnClickPendingIntent(id, openPage(ctx, 20 + i, "#order/" + line.optInt("order_id")))
        }
        val open = orders?.optInt("open") ?: 0
        val shipped = orders?.optInt("shipped") ?: 0

        if (bitmap != null) v.setImageViewBitmap(R.id.chart, bitmap)
        else v.setImageViewResource(R.id.chart, android.R.color.transparent)

        val stamp = SimpleDateFormat("h:mm a", Locale.US).format(Date())
        v.setTextViewText(
            R.id.status,
            (if (offline) "offline · last known · " else "") +
                "${chart.second} · $open on order" + (if (shipped > 0) " ($shipped shipped)" else "") +
                " · tap chart to switch · $stamp"
        )
        return v
    }

    private fun openPage(ctx: Context, code: Int, hash: String): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse(LIVE_URL + hash))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun broadcast(ctx: Context, code: Int, action: String): PendingIntent {
        val i = Intent(ctx, LibraryWidget::class.java).setAction(action)
        return PendingIntent.getBroadcast(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun fetchText(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        c.setRequestProperty("Cache-Control", "no-cache")
        try {
            if (c.responseCode != 200) throw IllegalStateException("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    private fun fetchBitmap(url: String): Bitmap {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        try {
            if (c.responseCode != 200) throw IllegalStateException("HTTP ${c.responseCode}")
            val raw = c.inputStream.use { BitmapFactory.decodeStream(it) }
                ?: throw IllegalStateException("not an image")
            // 960×520 from jerry; keep RemoteViews' bitmap budget comfortable
            val w = minOf(raw.width, 720)
            val h = raw.height * w / raw.width
            return if (w == raw.width) raw else Bitmap.createScaledBitmap(raw, w, h, true)
        } finally {
            c.disconnect()
        }
    }

    companion object {
        const val SITE_DATA = LIVE_URL + "data"
        val CHARTS = listOf(
            "progress" to "Progress",
            "history" to "Per year",
            "months" to "By month",
            "buying" to "Bought · arriving",
        )

        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "library-widget",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WidgetWorker>().build()
            )
        }
    }
}

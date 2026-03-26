package com.example.snapeats.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.snapeats.data.local.entity.MealLog
import com.example.snapeats.data.local.entity.User
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Generates a one-page A4 PDF health summary and shares it via [Intent.ACTION_SEND].
 *
 * The PDF contains:
 * - App title and export date
 * - User profile (height, weight, BMI, daily calorie target)
 * - Table of meal logs for the past 30 days
 * - 7-day average calories
 *
 * On Android 10+ (API 29+) the file is written to the public Downloads folder
 * via [MediaStore]. On older versions it is written directly to
 * [Environment.DIRECTORY_DOWNLOADS].
 *
 * After saving, the file is shared via a [FileProvider] URI so third-party
 * apps (Gmail, Drive, WhatsApp, etc.) can receive it.
 */
object PdfExporter {

    // A4 at 72 dpi
    private const val PAGE_WIDTH  = 595
    private const val PAGE_HEIGHT = 842

    // Layout constants
    private const val MARGIN_LEFT   = 48f
    private const val MARGIN_RIGHT  = 48f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

    private const val FILE_PREFIX = "SnapEats_Summary"

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /**
     * Builds and saves the PDF, then launches a share intent.
     * Must be called from a background coroutine — file I/O blocks the thread.
     */
    fun export(context: Context, user: User, meals: List<MealLog>) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val cursor = Cursor(y = 60f)
        drawContent(canvas, user, meals, cursor)

        document.finishPage(page)

        val fileName = buildFileName()
        val outputStream = openOutputStream(context, fileName)
        outputStream?.use { stream ->
            document.writeTo(stream)
        }
        document.close()

        val fileUri = resolveFileUri(context, fileName)
        if (fileUri != null) {
            launchShareIntent(context, fileUri)
        }
    }

    // ---------------------------------------------------------------------------
    // Drawing
    // ---------------------------------------------------------------------------

    private fun drawContent(
        canvas: Canvas,
        user: User,
        meals: List<MealLog>,
        cursor: Cursor
    ) {
        // ---- Title block -------------------------------------------------------
        drawText(
            canvas, "SnapEats Health Summary",
            x = MARGIN_LEFT, y = cursor.y,
            paint = titlePaint()
        )
        cursor.advance(10f)

        drawText(
            canvas, "Exported on ${formattedDate(System.currentTimeMillis())}",
            x = MARGIN_LEFT, y = cursor.y,
            paint = subtitlePaint()
        )
        cursor.advance(24f)

        drawHorizontalRule(canvas, cursor.y)
        cursor.advance(20f)

        // ---- Profile summary ---------------------------------------------------
        drawText(
            canvas, "Profile",
            x = MARGIN_LEFT, y = cursor.y,
            paint = sectionHeaderPaint()
        )
        cursor.advance(18f)

        val profileRows = listOf(
            "Height" to "${user.height} cm",
            "Weight" to "${user.weight} kg",
            "Age"    to "${user.age} years",
            "Sex"    to if (user.isMale) "Male" else "Female",
            "BMI"    to "%.1f (%s)".format(user.bmi, bmiCategory(user.bmi)),
            "Daily Calorie Target" to "${user.dailyCalTarget} kcal"
        )
        profileRows.forEach { (label, value) ->
            drawKeyValue(canvas, label, value, cursor.y)
            cursor.advance(16f)
        }
        cursor.advance(12f)

        drawHorizontalRule(canvas, cursor.y)
        cursor.advance(20f)

        // ---- 7-day average -----------------------------------------------------
        val thirtyDayLogs = logsWithinDays(meals, 30)
        val sevenDayLogs  = logsWithinDays(meals, 7)
        val sevenDayAvg   = if (sevenDayLogs.isEmpty()) 0
        else sevenDayLogs.sumOf { it.totalCal } / 7

        drawText(
            canvas, "Statistics",
            x = MARGIN_LEFT, y = cursor.y,
            paint = sectionHeaderPaint()
        )
        cursor.advance(18f)

        drawKeyValue(canvas, "7-Day Average", "$sevenDayAvg kcal / day", cursor.y)
        cursor.advance(16f)
        drawKeyValue(canvas, "Meals logged (last 30 days)", "${thirtyDayLogs.size}", cursor.y)
        cursor.advance(16f)
        cursor.advance(12f)

        drawHorizontalRule(canvas, cursor.y)
        cursor.advance(20f)

        // ---- Meal log table ----------------------------------------------------
        drawText(
            canvas, "Meal Logs — Last 30 Days",
            x = MARGIN_LEFT, y = cursor.y,
            paint = sectionHeaderPaint()
        )
        cursor.advance(18f)

        // Table header
        drawTableRow(canvas, "Date", "Time", "Total Calories", cursor.y, isHeader = true)
        cursor.advance(16f)
        drawHorizontalRule(canvas, cursor.y, color = Color.LTGRAY)
        cursor.advance(6f)

        if (thirtyDayLogs.isEmpty()) {
            drawText(
                canvas, "No meal logs in the past 30 days.",
                x = MARGIN_LEFT, y = cursor.y,
                paint = bodyPaint()
            )
            cursor.advance(16f)
        } else {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

            thirtyDayLogs
                .sortedByDescending { it.timestamp }
                .forEach { log ->
                    if (cursor.y > PAGE_HEIGHT - 60f) return // Guard against overflow
                    val dateStr = dateFormat.format(Date(log.timestamp))
                    val timeStr = timeFormat.format(Date(log.timestamp))
                    drawTableRow(canvas, dateStr, timeStr, "${log.totalCal} kcal", cursor.y)
                    cursor.advance(15f)
                }
        }
    }

    // ---------------------------------------------------------------------------
    // Drawing primitives
    // ---------------------------------------------------------------------------

    private fun drawText(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
        canvas.drawText(text, x, y, paint)
    }

    private fun drawHorizontalRule(
        canvas: Canvas,
        y: Float,
        color: Int = Color.DKGRAY
    ) {
        val paint = Paint().apply {
            this.color = color
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y, paint)
    }

    private fun drawKeyValue(canvas: Canvas, label: String, value: String, y: Float) {
        drawText(canvas, label, x = MARGIN_LEFT, y = y, paint = labelPaint())
        drawText(canvas, value, x = MARGIN_LEFT + 200f, y = y, paint = bodyPaint())
    }

    private fun drawTableRow(
        canvas: Canvas,
        col1: String,
        col2: String,
        col3: String,
        y: Float,
        isHeader: Boolean = false
    ) {
        val paint = if (isHeader) tableHeaderPaint() else bodyPaint()
        val col2X = MARGIN_LEFT + (CONTENT_WIDTH * 0.38f)
        val col3X = MARGIN_LEFT + (CONTENT_WIDTH * 0.68f)
        drawText(canvas, col1, x = MARGIN_LEFT, y = y, paint = paint)
        drawText(canvas, col2, x = col2X, y = y, paint = paint)
        drawText(canvas, col3, x = col3X, y = y, paint = paint)
    }

    // ---------------------------------------------------------------------------
    // Paint factories
    // ---------------------------------------------------------------------------

    private fun titlePaint() = Paint().apply {
        color = Color.BLACK
        textSize = 22f
        isFakeBoldText = true
        isAntiAlias = true
    }

    private fun subtitlePaint() = Paint().apply {
        color = Color.DKGRAY
        textSize = 11f
        isAntiAlias = true
    }

    private fun sectionHeaderPaint() = Paint().apply {
        color = Color.rgb(30, 100, 200) // Branded blue accent
        textSize = 14f
        isFakeBoldText = true
        isAntiAlias = true
    }

    private fun labelPaint() = Paint().apply {
        color = Color.DKGRAY
        textSize = 11f
        isAntiAlias = true
    }

    private fun bodyPaint() = Paint().apply {
        color = Color.BLACK
        textSize = 11f
        isAntiAlias = true
    }

    private fun tableHeaderPaint() = Paint().apply {
        color = Color.BLACK
        textSize = 11f
        isFakeBoldText = true
        isAntiAlias = true
    }

    // ---------------------------------------------------------------------------
    // File I/O — MediaStore (API 29+) and legacy fallback
    // ---------------------------------------------------------------------------

    private fun openOutputStream(context: Context, fileName: String): OutputStream? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            openOutputStreamMediaStore(context, fileName)
        } else {
            openOutputStreamLegacy(fileName)
        }
    }

    /** Android 10+ (API 29+): write to Downloads via MediaStore. */
    private fun openOutputStreamMediaStore(context: Context, fileName: String): OutputStream? {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }
        val itemUri = resolver.insert(collection, values) ?: return null
        val stream = resolver.openOutputStream(itemUri)

        // Mark the file as complete after writing
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(itemUri, values, null, null)

        return stream
    }

    /** Android 9 and below: write directly to the public Downloads folder. */
    @Suppress("DEPRECATION")
    private fun openOutputStreamLegacy(fileName: String): OutputStream? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        downloadsDir.mkdirs()
        val file = File(downloadsDir, fileName)
        return FileOutputStream(file)
    }

    // ---------------------------------------------------------------------------
    // FileProvider URI resolution for sharing
    // ---------------------------------------------------------------------------

    private fun resolveFileUri(context: Context, fileName: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On API 29+ use MediaStore to find the just-saved file
            val projection = arrayOf(MediaStore.Downloads._ID)
            val selection  = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)
            val collection = MediaStore.Downloads.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
            context.contentResolver
                .query(collection, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                        val id = cursor.getLong(idColumn)
                        Uri.withAppendedPath(collection, id.toString())
                    } else null
                }
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            val file = File(downloadsDir, fileName)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
    }

    // ---------------------------------------------------------------------------
    // Share intent
    // ---------------------------------------------------------------------------

    private fun launchShareIntent(context: Context, fileUri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "SnapEats Health Summary")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Health Summary").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    // ---------------------------------------------------------------------------
    // Utility helpers
    // ---------------------------------------------------------------------------

    /** Filters [meals] to only those whose timestamp falls within the last [days] days. */
    private fun logsWithinDays(meals: List<MealLog>, days: Int): List<MealLog> {
        val cutoff = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000)
        return meals.filter { it.timestamp >= cutoff }
    }

    private fun formattedDate(timestamp: Long): String =
        SimpleDateFormat("dd MMM yyyy 'at' hh:mm a", Locale.getDefault())
            .format(Date(timestamp))

    private fun bmiCategory(bmi: Float): String = when {
        bmi < 18.5f -> "Underweight"
        bmi < 25.0f -> "Normal"
        bmi < 30.0f -> "Overweight"
        else        -> "Obese"
    }

    private fun buildFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        return "${FILE_PREFIX}_$timestamp.pdf"
    }

    /** Mutable y-position cursor passed through drawing calls. */
    private class Cursor(var y: Float) {
        fun advance(by: Float) { y += by }
    }
}

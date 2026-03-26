package com.example.snapeats.util

import android.graphics.Bitmap

/**
 * Utility functions for [Bitmap] manipulation used in the food scan pipeline.
 */
object BitmapUtils {

    /**
     * Scales [bitmap] so that it fits within [targetWidth] × [targetHeight] while
     * preserving the original aspect ratio. If the bitmap already fits within the
     * bounds, it is returned unchanged (no unnecessary copy is made).
     *
     * Downsampling before ML Kit inference prevents [OutOfMemoryError] on
     * low-RAM devices and improves detection latency.
     *
     * @param bitmap       Source bitmap. The caller is responsible for recycling
     *                     the original after this function returns if it is no
     *                     longer needed.
     * @param targetWidth  Maximum width in pixels. Default 640.
     * @param targetHeight Maximum height in pixels. Default 480.
     * @return             A new [Bitmap] scaled to fit within the target bounds,
     *                     or the original [bitmap] if it already fits.
     */
    fun scaleBitmap(
        bitmap: Bitmap,
        targetWidth: Int = 640,
        targetHeight: Int = 480
    ): Bitmap {
        val srcWidth = bitmap.width
        val srcHeight = bitmap.height

        // If the bitmap already fits, skip the allocation entirely.
        if (srcWidth <= targetWidth && srcHeight <= targetHeight) {
            return bitmap
        }

        // Compute uniform scale factor so both dimensions fit within the target.
        val scaleX = targetWidth.toFloat() / srcWidth
        val scaleY = targetHeight.toFloat() / srcHeight
        val scale = minOf(scaleX, scaleY)

        val newWidth = (srcWidth * scale).toInt().coerceAtLeast(1)
        val newHeight = (srcHeight * scale).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, /* filter= */ true)
    }

    fun toBase64(bitmap: Bitmap): String {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return android.util.Base64.encodeToString(
            outputStream.toByteArray(), 
            android.util.Base64.NO_WRAP
        )
    }
}

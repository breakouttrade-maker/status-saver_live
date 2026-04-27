package com.breakout.statussaver.utils

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File

object FileUtils {
    fun getShareUri(context: Context, file: File): android.net.Uri? {
        return try { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file) } catch (e: Exception) { null }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }

    fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "mp4" -> "video/mp4"
            else -> "*/*"
        }
    }
}

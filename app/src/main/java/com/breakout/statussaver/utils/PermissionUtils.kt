package com.breakout.statussaver.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionUtils {

    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+): Ask for specific Media permissions
            hasPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) &&
            hasPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            // Android 12 and below: Ask for standard Storage permission
            hasPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun isWhatsAppInstalled(context: Context): Boolean {
        return try { context.packageManager.getPackageInfo("com.whatsapp", 0); true } catch (e: Exception) { false }
    }
}

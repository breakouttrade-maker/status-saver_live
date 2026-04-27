package com.breakout.statussaver.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.documentfile.provider.DocumentFile
import com.breakout.statussaver.data.model.StatusResult
import java.io.File

class WhatsAppStatusReader(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "status_saver_prefs"
        private const val KEY_SAF_URI = "persisted_statuses_uri"
        private val SUPPORTED_EXT = setOf("jpg", "jpeg", "png", "webp", "mp4", "3gp")
        private val SAVED_DIRECT_PATHS = listOf(
            "/storage/emulated/0/Pictures/Status Saver",
            "/storage/emulated/0/Movies/Status Saver"
        )
        private val TARGET_FOLDERS = listOf("com.whatsapp", "com.whatsapp.w4b", "com.whatsapp.w4b")
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readStatuses(): StatusResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            try {
                val dir = File("/storage/emulated/0/WhatsApp/Media/.Statuses")
                if (dir.exists() && dir.canRead()) {
                    val files = dir.listFiles()?.filter { it.isFile && it.extension.lowercase() in SUPPORTED_EXT }?.sortedByDescending { it.lastModified() }?.map { DocumentFile.fromFile(it) }
                    if (!files.isNullOrEmpty()) return StatusResult.Success(files)
                }
            } catch (e: Exception) {}
        }
        return StatusResult.NeedsUserPicker
    }

    fun persistSafUri(treeUri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            prefs.edit().putString(KEY_SAF_URI, treeUri.toString()).apply()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun readFromSaf(treeUri: Uri): List<DocumentFile> {
        try {
            val doc = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
            if (!doc.isDirectory || !doc.canRead()) return emptyList()
            
            if (doc.name?.equals(".Statuses", ignoreCase = true) == true) {
                return doc.listFiles()?.filter { it.isFile && it.name?.substringAfterLast(".", "") in SUPPORTED_EXT }?.sortedByDescending { it.lastModified() } ?: emptyList()
            }

            // THE MAGIC TRICK: findFile("com.whatsapp") takes 0.01 seconds. NO LAG!
            val allFiles = mutableListOf<DocumentFile>()
            
            for (targetName in TARGET_FOLDERS) {
                val whatsappDir = doc.findFile(targetName)
                if (whatsappDir != null && whatsappDir.isDirectory && whatsappDir.canRead()) {
                    findStatusesInDir(whatsappDir, allFiles)
                }
            }

            // Fallback: If user selected com.whatsapp directly
            if (allFiles.isEmpty()) {
                findStatusesInDir(doc, allFiles)
            }

            return allFiles
        } catch (e: Exception) {
            return emptyList()
        }
    }

    // Instantly finds .Statuses without listing useless folders
    private fun findStatusesInDir(dir: DocumentFile, fileList: MutableList<DocumentFile>) {
        try {
            val statusDir = dir.findFile("WhatsApp")?.findFile("Media")?.findFile(".Statuses")
            if (statusDir != null && statusDir.isDirectory) {
                statusDir.listFiles()?.filter { it.isFile && it.name?.substringAfterLast(".", "") in SUPPORTED_EXT }?.let { fileList.addAll(it) }
            }
            
            // Secondary fallback
            val mediaDir = dir.findFile("Media")
            if (mediaDir != null && mediaDir.isDirectory) {
                val statuses = mediaDir.findFile(".Statuses")
                if (statuses != null) {
                    statuses.listFiles()?.filter { it.isFile && it.name?.substringAfterLast(".", "") in SUPPORTED_EXT }?.let { fileList.addAll(it) }
                }
            }
        } catch (e: Exception) {}
    }

    fun loadSavedStatuses(): List<DocumentFile> {
        val allSavedFiles = mutableListOf<File>()
        for (path in SAVED_DIRECT_PATHS) {
            try {
                val dir = File(path)
                if (dir.exists() && dir.isDirectory && dir.canRead()) dir.listFiles()?.let { allSavedFiles.addAll(it) }
            } catch (e: Exception) {}
        }
        return allSavedFiles.filter { it.isFile && it.extension.lowercase() in SUPPORTED_EXT }.sortedByDescending { it.lastModified() }.map { DocumentFile.fromFile(it) }
    }
}

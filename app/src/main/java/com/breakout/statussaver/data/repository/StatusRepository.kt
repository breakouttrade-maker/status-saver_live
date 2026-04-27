package com.breakout.statussaver.data.repository

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.breakout.statussaver.data.model.StatusFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Collections

class StatusRepository(private val context: Context) {

    companion object {
        private const val SAVE_FOLDER = "Status Saver"
        var diagnosticLog = ""
    }

    suspend fun loadStatuses(filterType: FilterType = FilterType.ALL): List<StatusFile> {
        return withContext(Dispatchers.IO) {
            try {
                val allFiles = mutableListOf<StatusFile>()
                val uris = mutableListOf<Uri>()
                if (filterType == FilterType.ALL || filterType == FilterType.IMAGE) {
                    uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                }
                if (filterType == FilterType.ALL || filterType == FilterType.VIDEO) {
                    uris.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                }
                val projection = arrayOf(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    MediaStore.MediaColumns.DISPLAY_NAME
                )
                val sel = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selArgs = arrayOf("%WhatsApp%/.Statuses%", "%com.whatsapp%/.Statuses%")
                val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
                val baseStorage = Environment.getExternalStorageDirectory().absolutePath
                for (uri in uris) {
                    try {
                        context.contentResolver.query(uri, projection, sel, selArgs, sortOrder)?.use { cursor ->
                            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                            while (cursor.moveToNext()) {
                                val relPath = cursor.getString(pathCol) ?: continue
                                val fileName = cursor.getString(nameCol) ?: continue
                                val fullPath = "${baseStorage}/${relPath}${fileName}"
                                val file = File(fullPath)
                                if (file.exists() && file.canRead()) {
                                    val isVideo = uri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                    allFiles.add(StatusFile(file = file, isVideo = isVideo))
                                }
                            }
                        }
                    } catch (e: Exception) { }
                }
                Collections.sort(allFiles) { a, b -> b.lastModified.compareTo(a.lastModified) }
                allFiles
            } catch (e: Exception) { emptyList() }
        }
    }

    suspend fun loadSavedStatuses(): List<StatusFile> {
        return withContext(Dispatchers.IO) {
            try {
                val d = getSaveDirectory()
                if (!d.exists()) return@withContext emptyList()
                d.listFiles()?.filter { it.isFile && it.canRead() }?.map {
                    StatusFile(file = it, isVideo = it.extension.lowercase() in setOf(".mp4", ".3gp"))
                } ?: emptyList()
            } catch (e: Exception) { emptyList() }
        }
    }

    suspend fun saveStatus(statusFile: StatusFile): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val d = getSaveDirectory()
                if (!d.exists()) d.mkdirs()
                val f = if (File(d, statusFile.name).exists()) {
                    File(d, statusFile.name.substringBeforeLast(".") + "_" + System.currentTimeMillis() + "." + statusFile.name.substringAfterLast("."))
                } else {
                    File(d, statusFile.name)
                }
                FileInputStream(statusFile.file).use { i ->
                    FileOutputStream(f).use { o -> i.channel.transferTo(0, i.channel.size(), o.channel) }
                }
                MediaScannerConnection.scanFile(context, arrayOf(f.absolutePath), arrayOf(if (statusFile.isVideo) "video/mp4" else "image/jpeg"), null)
                Result.success(f)
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    fun isAlreadySaved(statusFile: StatusFile): Boolean {
        return try {
            val d = getSaveDirectory()
            d.exists() && (d.listFiles()?.any { it.name == statusFile.name } ?: false)
        } catch (e: Exception) { false }
    }

    private fun getSaveDirectory(): File = File(context.getExternalFilesDir(null), SAVE_FOLDER)

    enum class FilterType { ALL, IMAGE, VIDEO }
}
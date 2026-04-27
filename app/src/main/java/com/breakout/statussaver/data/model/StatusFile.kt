package com.breakout.statussaver.data.model

import android.net.Uri
import java.io.File

data class StatusFile(
    val file: File,
    val path: String = file.absolutePath,
    val name: String = file.name,
    val isVideo: Boolean,
    val size: Long = file.length(),
    val lastModified: Long = file.lastModified(),
    val uri: Uri = Uri.fromFile(file)
)

sealed class Resource<out T> {
    data class Loading(val showShimmer: Boolean = true) : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    data object Empty : Resource<Nothing>()
}

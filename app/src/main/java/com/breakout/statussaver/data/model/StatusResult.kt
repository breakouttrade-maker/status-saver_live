package com.breakout.statussaver.data.model

import androidx.documentfile.provider.DocumentFile

sealed class StatusResult {
    data class Success(val files: List<DocumentFile>) : StatusResult()
    object NeedsUserPicker : StatusResult()
}
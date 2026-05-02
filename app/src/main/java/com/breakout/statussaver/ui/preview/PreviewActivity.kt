package com.breakout.statussaver.ui.preview

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.breakout.statussaver.R
import com.breakout.statussaver.ads.AdManager
import com.breakout.statussaver.databinding.ActivityPreviewBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PreviewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URI_STRING = "extra_uri_string"
        const val EXTRA_IS_VIDEO = "extra_is_video"
        const val EXTRA_FILE_NAME = "extra_file_name"
    }

    private lateinit var binding: ActivityPreviewBinding
    private var player: ExoPlayer? = null
    private var isVideo = false
    private lateinit var uriString: String
    private lateinit var fileName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        uriString = intent.getStringExtra(EXTRA_URI_STRING) ?: run { finish(); return }
        isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO, false)
        fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "status"

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Smart Banner Load
        if (AdManager.isInitialized) {
            try { binding.adView.loadAd(AdRequest.Builder().build()) } catch (e: Exception) { binding.adView.visibility = View.GONE }
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try { binding.adView.loadAd(AdRequest.Builder().build()) } catch (e: Exception) { binding.adView.visibility = View.GONE }
            }, 1500)
        }

        AdManager.preloadRewardedAd(this)

        val uri = Uri.parse(uriString)
        if (isVideo) {
            binding.ivPhotoView.visibility = View.GONE
            binding.playerView.visibility = View.VISIBLE
            try {
                player = ExoPlayer.Builder(this).build()
                binding.playerView.player = player
                player?.setMediaItem(MediaItem.fromUri(uri))
                player?.prepare()
                player?.playWhenReady = true
            } catch (e: Exception) { Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show(); finish() }
        } else {
            binding.ivPhotoView.visibility = View.VISIBLE
            binding.playerView.visibility = View.GONE
            com.bumptech.glide.Glide.with(this).load(uri).into(binding.ivPhotoView)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { menuInflater.inflate(R.menu.menu_preview, menu); return true }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> { handleSave(); true }
            R.id.action_share -> { handleShare(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSnackbar(msg: String) { Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show() }

    private fun handleSave() {
        if (AdManager.isRewardedAdReady()) {
            showSnackbar("Watch the ad to save...")
            AdManager.showRewardedAd(this, onResult = { earned -> if (earned) doActualSave() }, onNotReady = { doActualSave() })
        } else {
            showSnackbar("Loading ad, saving directly...")
            AdManager.preloadRewardedAd(this)
            doActualSave()
        }
    }

    private fun doActualSave() {
        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val saved = saveFileToGallery()
                runOnUiThread { binding.progressBar.visibility = View.GONE; if (saved) showSnackbar("Saved to Gallery!") else showSnackbar("Failed to save") }
            } catch (e: Exception) { runOnUiThread { binding.progressBar.visibility = View.GONE; showSnackbar("Failed to save") } }
        }
    }

    private fun saveFileToGallery(): Boolean {
        val sourceUri = Uri.parse(uriString)
        val mimeType = contentResolver.getType(sourceUri) ?: (if (isVideo) "video/mp4" else "image/jpeg")
        val ext = if (isVideo) ".mp4" else ".jpg"
        val displayName = "StatusSaver_${System.currentTimeMillis()}$ext"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, if (isVideo) Environment.DIRECTORY_MOVIES + "/Status Saver" else Environment.DIRECTORY_PICTURES + "/Status Saver")
            }
        }
        val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val destUri = contentResolver.insert(collection, contentValues) ?: throw Exception("Cannot access Gallery")
        contentResolver.openInputStream(sourceUri)?.use { input ->
            contentResolver.openOutputStream(destUri)?.use { output -> input.copyTo(output) }
        } ?: throw Exception("Cannot read source file")
        return true
    }

    private fun handleShare() {
        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sourceUri = Uri.parse(uriString)
                val ext = if (isVideo) ".mp4" else ".jpg"
                val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
                val tempFile = File(cacheDir, "share_temp_${System.currentTimeMillis()}$ext")
                contentResolver.openInputStream(sourceUri)?.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } } ?: throw Exception("Cannot read source file")
                val shareUri = FileProvider.getUriForFile(this@PreviewActivity, "${packageName}.fileprovider", tempFile)
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    if (AdManager.trackShare()) {
                        AdManager.showInterstitial(this@PreviewActivity) { launchShare(shareUri, mimeType) }
                    } else {
                        launchShare(shareUri, mimeType)
                    }
                }
            } catch (e: Exception) { runOnUiThread { binding.progressBar.visibility = View.GONE; showSnackbar("Failed to share") } }
        }
    }

    private fun launchShare(shareUri: Uri, mimeType: String) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = mimeType; putExtra(Intent.EXTRA_STREAM, shareUri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share Status via..."))
    }

    override fun onDestroy() { super.onDestroy(); player?.release(); player = null }
}

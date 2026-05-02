package com.breakout.statussaver.ui.home

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.GridLayoutManager
import com.breakout.statussaver.R
import com.breakout.statussaver.ads.AdManager
import com.breakout.statussaver.data.model.StatusResult
import com.breakout.statussaver.data.repository.WhatsAppStatusReader
import com.breakout.statussaver.databinding.ActivityHomeBinding
import com.breakout.statussaver.ui.adapter.StatusAdapter
import com.breakout.statussaver.ui.preview.PreviewActivity
import com.breakout.statussaver.ui.settings.SettingsActivity
import com.breakout.statussaver.utils.PermissionUtils
import com.google.android.gms.ads.AdRequest
import com.google.android.material.tabs.TabLayout

class HomeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILE_NAME = "file_name"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var binding: ActivityHomeBinding
    private val reader by lazy { WhatsAppStatusReader(this) }
    private val prefs by lazy { getSharedPreferences("status_saver_prefs", MODE_PRIVATE) }
    private val mainHandler = Handler(Looper.getMainLooper())

    private val imgAdapter = StatusAdapter({ openPreview(it) }, {})
    private val vidAdapter = StatusAdapter({ openPreview(it) }, {})
    private val savedAdapter = StatusAdapter({ deleteSingleSavedItem(it) }, {})
    private var curTab = 1

    private val folderPickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            reader.persistSafUri(uri)
            loadAllStatuses()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupTabs()
        setupRVs()
        setupSwipe()

        try { binding.adView.loadAd(AdRequest.Builder().build()) } catch (e: Exception) { binding.adView.visibility = View.GONE }

        AdManager.preloadAppOpenAd(this)

        if (PermissionUtils.hasStoragePermission(this)) {
            showAppOpenThenLoad()
        } else {
            requestPermissions()
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (imgAdapter.isSelectMode || vidAdapter.isSelectMode || savedAdapter.isSelectMode) {
                    imgAdapter.clearSelection(); vidAdapter.clearSelection(); savedAdapter.clearSelection()
                } else finishAffinity()
            }
        })
    }

    private fun showAppOpenThenLoad() {
        mainHandler.postDelayed({
            AdManager.showAppOpenAdIfReady(this) { loadAllStatuses() }
        }, 2000)
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, PermissionUtils.getRequiredPermissions(), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                showAppOpenThenLoad()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Storage Permission Required")
            .setMessage("The app needs access to storage to view statuses.")
            .setPositiveButton("Grant") { _, _ -> requestPermissions() }
            .setNegativeButton("Exit") { _, _ -> finishAffinity() }
            .setCancelable(false)
            .show()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Status Saver"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
            R.id.action_refresh -> { imgAdapter.clearSelection(); vidAdapter.clearSelection(); savedAdapter.clearSelection(); loadAllStatuses(); true }
            R.id.action_delete -> { handleDelete(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleDelete() {
        if (curTab != 2) {
            Toast.makeText(this, "Switch to Saved tab to delete items.", Toast.LENGTH_LONG).show()
            return
        }
        if (!savedAdapter.isSelectMode) {
            Toast.makeText(this, "Long-press saved items first to select them.", Toast.LENGTH_LONG).show()
            return
        }
        val itemsToDelete = savedAdapter.getSelectedItems()
        if (itemsToDelete.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle("Delete " + itemsToDelete.size + " saved items?")
            .setMessage("This will permanently delete the saved files from your device.")
            .setPositiveButton("Delete") { _, _ ->
                var count = 0
                for (doc in itemsToDelete) {
                    try {
                        if (doc.delete()) count++
                        else { contentResolver.delete(doc.uri, null, null); count++ }
                    } catch (e: Exception) { e.printStackTrace() }
                }
                savedAdapter.clearSelection()
                loadAllStatuses()
                Toast.makeText(this, "Deleted " + count + " saved items", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { _, _ -> savedAdapter.clearSelection() }
            .show()
    }

    private fun deleteSingleSavedItem(doc: DocumentFile) {
        AlertDialog.Builder(this)
            .setTitle("Delete Status?")
            .setMessage("Do you want to permanently delete this saved file?")
            .setPositiveButton("Delete") { _, _ ->
                try {
                    if (doc.delete()) {
                        Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        contentResolver.delete(doc.uri, null, null)
                        Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show()
                    }
                    loadAllStatuses()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadAllStatuses() {
        binding.tvSyncStatus.text = "Loading..."
        binding.tvSyncStatus.visibility = View.VISIBLE
        binding.shimmerImages.visibility = View.VISIBLE
        binding.shimmerImages.startShimmer()

        binding.recyclerViewImages.visibility = View.GONE
        binding.recyclerViewVideos.visibility = View.GONE
        binding.recyclerViewSaved.visibility = View.GONE
        binding.layoutEmpty.root.visibility = View.GONE

        Thread {
            var imageList = emptyList<DocumentFile>()
            var videoList = emptyList<DocumentFile>()
            var savedList = emptyList<DocumentFile>()
            var needsPicker = false

            try {
                val result = reader.readStatuses()
                if (result is StatusResult.Success) {
                    imageList = result.files.filter { it.type?.startsWith("image/") == true }
                    videoList = result.files.filter { it.type?.startsWith("video/") == true }
                } else if (result is StatusResult.NeedsUserPicker) {
                    val uriString = prefs.getString("persisted_statuses_uri", null)
                    if (uriString != null) {
                        val safFiles = reader.readFromSaf(Uri.parse(uriString))
                        imageList = safFiles.filter { it.type?.startsWith("image/") == true }
                        videoList = safFiles.filter { it.type?.startsWith("video/") == true }
                        if (safFiles.isEmpty()) needsPicker = true
                    } else { needsPicker = true }
                }
                savedList = reader.loadSavedStatuses()
            } catch (e: Throwable) { needsPicker = true; e.printStackTrace() } finally {
                mainHandler.post {
                    imgAdapter.submitList(imageList)
                    vidAdapter.submitList(videoList)
                    savedAdapter.submitList(savedList)

                    mainHandler.postDelayed({
                        binding.shimmerImages.stopShimmer()
                        binding.shimmerImages.visibility = View.GONE
                        binding.tvSyncStatus.visibility = View.GONE
                        if (needsPicker) showFolderPickerDialog() else showTab(curTab)

                        AdManager.preloadInterstitial(this)
                        AdManager.preloadRewardedAd(this)
                    }, 400)
                }
            }
        }.start()
    }

    private fun showFolderPickerDialog() {
        AlertDialog.Builder(this)
            .setTitle("Select Media Folder")
            .setMessage("Please select the media folder.\n\n1. Tap OK.\n2. Tap USE THIS FOLDER at the bottom.")
            .setPositiveButton("OK") { _, _ ->
                try {
                    folderPickerLauncher.launch(android.provider.DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Android/media"))
                } catch (e: Exception) { folderPickerLauncher.launch(null) }
            }
            .setNegativeButton("Exit") { _, _ -> finishAffinity() }
            .setCancelable(false)
            .show()
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_images))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_videos))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_saved))
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                curTab = tab.position
                imgAdapter.clearSelection(); vidAdapter.clearSelection(); savedAdapter.clearSelection()
                showTab(tab.position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun showTab(p: Int) {
        binding.recyclerViewImages.visibility = View.GONE
        binding.recyclerViewVideos.visibility = View.GONE
        binding.recyclerViewSaved.visibility = View.GONE
        binding.layoutEmpty.root.visibility = View.GONE
        when (p) {
            0 -> if (imgAdapter.currentList.isNotEmpty()) binding.recyclerViewImages.visibility = View.VISIBLE else showEmpty("No image statuses found")
            1 -> if (vidAdapter.currentList.isNotEmpty()) binding.recyclerViewVideos.visibility = View.VISIBLE else showEmpty("No video statuses found")
            else -> if (savedAdapter.currentList.isNotEmpty()) binding.recyclerViewSaved.visibility = View.VISIBLE else showEmpty("No saved statuses yet")
        }
    }

    private fun showEmpty(msg: String) {
        binding.layoutEmpty.tvEmptyMessage.text = msg
        binding.layoutEmpty.root.visibility = View.VISIBLE
    }

    private fun setupRVs() {
        val spanCount = 3
        binding.recyclerViewImages.layoutManager = GridLayoutManager(this, spanCount)
        binding.recyclerViewImages.adapter = imgAdapter
        binding.recyclerViewVideos.layoutManager = GridLayoutManager(this, spanCount)
        binding.recyclerViewVideos.adapter = vidAdapter
        binding.recyclerViewSaved.layoutManager = GridLayoutManager(this, spanCount)
        binding.recyclerViewSaved.adapter = savedAdapter
    }

    private fun setupSwipe() {
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.primary, R.color.primary_dark)
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
            loadAllStatuses()
        }
    }

    private fun openPreview(doc: DocumentFile) {
        if (AdManager.canShowOnPreview()) {
            AdManager.showInterstitial(this) { doPreview(doc) }
        } else {
            doPreview(doc)
        }
    }

    private fun doPreview(doc: DocumentFile) {
        if (curTab == 2) {
            AlertDialog.Builder(this)
                .setTitle("Delete Status?")
                .setMessage("Permanently delete this saved file?")
                .setPositiveButton("Delete") { _, _ ->
                    try {
                        val deleted = doc.delete()
                        if (!deleted) contentResolver.delete(doc.uri, null, null)
                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) { Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show() }
                    loadAllStatuses()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            startActivity(Intent(this, PreviewActivity::class.java).apply {
                putExtra(PreviewActivity.EXTRA_URI_STRING, doc.uri.toString())
                putExtra(PreviewActivity.EXTRA_IS_VIDEO, doc.type?.startsWith("video/") == true)
                putExtra(EXTRA_FILE_NAME, doc.name ?: "status")
            })
        }
    }
}
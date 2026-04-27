package com.breakout.statussaver.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.breakout.statussaver.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Dark Mode Setup
        val isDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        binding.switchTheme.isChecked = isDarkMode

        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Privacy Policy
        binding.cardPrivacy.setOnClickListener {
            // REPLACE THIS URL WITH YOUR ACTUAL PRIVACY POLICY URL
            val url = "https://breakouttrade-maker.github.io/status-saver-privacy/" 
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        // Rate App
        binding.cardRate.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${packageName}")))
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${packageName}")))
            }
        }

        // Share App
        binding.cardShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Download Status Saver App now: https://play.google.com/store/apps/details?id=${packageName}")
            }
            startActivity(Intent.createChooser(shareIntent, "Share App"))
        }
    }
}

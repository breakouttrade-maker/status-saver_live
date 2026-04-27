package com.breakout.statussaver.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.breakout.statussaver.ads.AdManager
import com.breakout.statussaver.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        AdManager.preloadAppOpenAd(this)
        
        // Wait 3 seconds max for ad to load, then go to home
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, com.breakout.statussaver.ui.home.HomeActivity::class.java))
            finish()
        }, 3000L)
    }
}

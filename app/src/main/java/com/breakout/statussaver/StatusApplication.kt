package com.breakout.statussaver

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.breakout.statussaver.ads.AdManager

class StatusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        initAds()
    }

    private fun initAds() {
        MobileAds.initialize(this) { initStatus ->
            Log.d("AdInit", "AdMob Initialization Complete")
            AdManager.setInitialized()
            AdManager.preloadInterstitial(this)
            AdManager.preloadAppOpenAd(this)
        }
    }

    companion object {
        lateinit var instance: Application
            private set
    }
}

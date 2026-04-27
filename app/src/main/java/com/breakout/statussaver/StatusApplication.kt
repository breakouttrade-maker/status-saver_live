package com.breakout.statussaver

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.breakout.statussaver.ads.AdManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StatusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        initAds()
    }

    private fun initAds() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MobileAds.initialize(this@StatusApplication) {
                    AdManager.preloadInterstitial(this@StatusApplication)
                }
            } catch (e: Exception) {
                Log.e("AdInit", "Failed", e)
            }
        }
    }

    companion object {
        lateinit var instance: StatusApplication
            private set
    }
}

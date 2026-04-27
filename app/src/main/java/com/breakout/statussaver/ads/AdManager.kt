package com.breakout.statussaver.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {
    // TODO: Replace with your REAL App Open Ad Unit ID from AdMob
    private const val APP_OPEN_ID = "ca-app-pub-1607968585289432/2226111222" // TEST ID (Pays fake money)
    private const val INTERSTITIAL_ID = "ca-app-pub-1607968585289432/7681631192"
    const val BANNER_ID = "ca-app-pub-1607968585289432/8779090448"

    @Volatile private var appOpenAd: InterstitialAd? = null
    @Volatile private var interstitialAd: InterstitialAd? = null
    
    private var saveCount = 0
    private var lastAnyAdTime = 0L // Global timer for ALL ads
    private val SAVE_INTERVAL = 3
    private val GLOBAL_AD_COOLDOWN = 60_000L // STRICT 60 seconds between ANY ad
    private var isAdShowing = false // Prevents double ads

    // --- APP OPEN AD LOGIC ---
    fun preloadAppOpenAd(context: Context) {
        if (appOpenAd != null) return
        try {
            InterstitialAd.load(context, APP_OPEN_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { appOpenAd = ad }
                override fun onAdFailedToLoad(e: LoadAdError) { appOpenAd = null }
            })
        } catch (e: Exception) { appOpenAd = null }
    }

    fun showAppOpenAdIfReady(activity: Activity, onDismissed: () -> Unit) {
        if (isAdShowing) { onDismissed(); return } // Don't stack ads!
        if (SystemClock.elapsedRealtime() - lastAnyAdTime < GLOBAL_AD_COOLDOWN) { onDismissed(); return }
        
        val ad = appOpenAd
        if (ad == null) { onDismissed(); return }
        
        isAdShowing = true
        try {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { 
                    isAdShowing = false; appOpenAd = null; lastAnyAdTime = SystemClock.elapsedRealtime()
                    onDismissed(); preloadAppOpenAd(activity) 
                }
                override fun onAdFailedToShowFullScreenContent(e: AdError) { 
                    isAdShowing = false; appOpenAd = null; onDismissed() 
                }
                override fun onAdShowedFullScreenContent() { lastAnyAdTime = SystemClock.elapsedRealtime() }
            }
            ad.show(activity)
        } catch (e: Exception) { isAdShowing = false; appOpenAd = null; onDismissed() }
    }

    // --- INTERSTITIAL AD LOGIC ---
    fun preloadInterstitial(context: Context) {
        if (interstitialAd != null) return
        try {
            InterstitialAd.load(context, INTERSTITIAL_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
                override fun onAdFailedToLoad(e: LoadAdError) { interstitialAd = null }
            })
        } catch (e: Exception) { interstitialAd = null }
    }

    fun trackSave(): Boolean { 
        saveCount++; 
        return saveCount >= SAVE_INTERVAL 
    }

    fun canShowOnPreview(): Boolean = SystemClock.elapsedRealtime() - lastAnyAdTime > GLOBAL_AD_COOLDOWN

    fun showInterstitial(activity: Activity, onDismissed: () -> Unit) {
        if (isAdShowing) { onDismissed(); return } // Don't stack ads!
        if (SystemClock.elapsedRealtime() - lastAnyAdTime < GLOBAL_AD_COOLDOWN) { onDismissed(); return }
        
        val ad = interstitialAd
        if (ad == null) { onDismissed(); return }
        
        isAdShowing = true
        try {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { 
                    isAdShowing = false; interstitialAd = null; lastAnyAdTime = SystemClock.elapsedRealtime()
                    saveCount = 0; onDismissed(); preloadInterstitial(activity) 
                }
                override fun onAdFailedToShowFullScreenContent(e: AdError) { 
                    isAdShowing = false; interstitialAd = null; onDismissed(); preloadInterstitial(activity) 
                }
                override fun onAdShowedFullScreenContent() { lastAnyAdTime = SystemClock.elapsedRealtime() }
            }
            ad.show(activity)
        } catch (e: Exception) { isAdShowing = false; interstitialAd = null; onDismissed() }
    }
}

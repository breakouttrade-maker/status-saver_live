package com.breakout.statussaver.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {
    private const val TAG = "AdManager"

    // ==========================================
    // 100% VERIFIED REAL AD IDs
    // ==========================================
    private const val APP_OPEN_ID = "ca-app-pub-1607968585289432/2226111222"
    private const val INTERSTITIAL_ID = "ca-app-pub-1607968585289432/7681631192" // FIXED: Was 1195, now 1192
    const val BANNER_ID = "ca-app-pub-1607968585289432/8779090448"
    private const val REWARDED_ID = "ca-app-pub-1607968585289432/7655429661"

    var isInitialized = false
        private set

    @Volatile private var appOpenAd: InterstitialAd? = null
    @Volatile private var interstitialAd: InterstitialAd? = null
    @Volatile private var rewardedAd: RewardedAd? = null
    private var rewardedCallback: ((Boolean) -> Unit)? = null

    private var saveCount = 0
    private var shareCount = 0
    private var lastAnyAdTime = 0L
    
    private val SAVE_INTERVAL = 3
    private val SHARE_INTERVAL = 2
    private val GLOBAL_AD_COOLDOWN = 45_000L
    private var isAdShowing = false
    
    private val handler = Handler(Looper.getMainLooper())

    fun setInitialized() {
        isInitialized = true
        Log.d(TAG, "✅ SDK READY")
    }

    fun preloadAppOpenAd(context: Context) {
        if (appOpenAd != null) return
        try {
            InterstitialAd.load(context, APP_OPEN_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { appOpenAd = ad; Log.d(TAG, "✅ App Open Ready") }
                override fun onAdFailedToLoad(e: LoadAdError) { appOpenAd = null; Log.e(TAG, "❌ App Open: ${e.message}") }
            })
        } catch (e: Exception) { appOpenAd = null }
    }

    fun showAppOpenAdIfReady(activity: Activity, onDismissed: () -> Unit) {
        if (isAdShowing) { onDismissed(); return }
        val ad = appOpenAd
        if (ad != null && SystemClock.elapsedRealtime() - lastAnyAdTime > GLOBAL_AD_COOLDOWN) {
            isAdShowing = true
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { isAdShowing = false; appOpenAd = null; lastAnyAdTime = SystemClock.elapsedRealtime(); onDismissed(); preloadAppOpenAd(activity) }
                override fun onAdFailedToShowFullScreenContent(e: AdError) { isAdShowing = false; appOpenAd = null; onDismissed() }
                override fun onAdShowedFullScreenContent() { lastAnyAdTime = SystemClock.elapsedRealtime() }
            }
            try { ad.show(activity) } catch (e: Exception) { isAdShowing = false; appOpenAd = null; onDismissed() }
        } else { onDismissed() }
    }

    fun preloadInterstitial(context: Context) {
        if (interstitialAd != null) return
        try {
            InterstitialAd.load(context, INTERSTITIAL_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad; Log.d(TAG, "✅ Interstitial Ready") }
                override fun onAdFailedToLoad(e: LoadAdError) { interstitialAd = null; Log.e(TAG, "❌ Interstitial: ${e.message}") }
            })
        } catch (e: Exception) { interstitialAd = null }
    }

    fun trackSave(): Boolean { saveCount++; return saveCount >= SAVE_INTERVAL }
    fun trackShare(): Boolean { shareCount++; return shareCount >= SHARE_INTERVAL }
    fun canShowOnPreview(): Boolean = SystemClock.elapsedRealtime() - lastAnyAdTime > GLOBAL_AD_COOLDOWN

    fun showInterstitial(activity: Activity, onDismissed: () -> Unit = {}) {
        if (isAdShowing || SystemClock.elapsedRealtime() - lastAnyAdTime < GLOBAL_AD_COOLDOWN) { onDismissed(); return }
        val ad = interstitialAd
        if (ad != null) {
            isAdShowing = true
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { isAdShowing = false; interstitialAd = null; lastAnyAdTime = SystemClock.elapsedRealtime(); saveCount = 0; shareCount = 0; onDismissed(); preloadInterstitial(activity) }
                override fun onAdFailedToShowFullScreenContent(e: AdError) { isAdShowing = false; interstitialAd = null; onDismissed(); preloadInterstitial(activity) }
                override fun onAdShowedFullScreenContent() { lastAnyAdTime = SystemClock.elapsedRealtime() }
            }
            try { ad.show(activity) } catch (e: Exception) { isAdShowing = false; interstitialAd = null; onDismissed() }
        } else {
            onDismissed()
            handler.postDelayed({ preloadInterstitial(activity) }, 3000)
        }
    }

    fun preloadRewardedAd(context: Context) {
        if (rewardedAd != null) return
        try {
            RewardedAd.load(context, REWARDED_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad; Log.d(TAG, "✅ Rewarded Ready") }
                override fun onAdFailedToLoad(e: LoadAdError) { rewardedAd = null; Log.e(TAG, "❌ Rewarded: ${e.message}") }
            })
        } catch (e: Exception) { rewardedAd = null }
    }

    fun isRewardedAdReady(): Boolean = rewardedAd != null

    fun showRewardedAd(activity: Activity, onResult: (Boolean) -> Unit, onNotReady: (() -> Unit)? = null) {
        if (isAdShowing) { onNotReady?.invoke(); return }
        val ad = rewardedAd
        if (ad != null) {
            isAdShowing = true
            rewardedCallback = onResult
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { isAdShowing = false; rewardedAd = null; lastAnyAdTime = SystemClock.elapsedRealtime(); preloadRewardedAd(activity) }
                override fun onAdFailedToShowFullScreenContent(e: AdError) { isAdShowing = false; rewardedAd = null; rewardedCallback?.invoke(false); rewardedCallback = null; preloadRewardedAd(activity) }
                override fun onAdShowedFullScreenContent() { lastAnyAdTime = SystemClock.elapsedRealtime() }
            }
            try {
                ad.show(activity) { rewardItem: RewardItem -> rewardedCallback?.invoke(true); rewardedCallback = null }
            } catch (e: Exception) { isAdShowing = false; rewardedAd = null; rewardedCallback?.invoke(false); rewardedCallback = null }
        } else {
            onNotReady?.invoke()
            handler.postDelayed({ preloadRewardedAd(activity) }, 2000)
        }
    }
}

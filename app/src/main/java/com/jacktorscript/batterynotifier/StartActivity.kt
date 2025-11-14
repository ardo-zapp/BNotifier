package com.jacktorscript.batterynotifier

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.jacktorscript.batterynotifier.core.Prefs
import com.jacktorscript.batterynotifier.core.PrefsConfig


class StartActivity : AppCompatActivity() {
    private var handler: Handler? = null
    private var billingClient: BillingClient? = null
    private var prefs: Prefs? = null
    private var prefsConfig: PrefsConfig? = null
    private var mInterstitialAd: InterstitialAd? = null
    private var isAdLoading = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_wellcome)

        prefs = Prefs(this)
        prefsConfig = PrefsConfig(this)

        // Atur warna status bar untuk android lollipop
        if (Build.VERSION.SDK_INT <= 22) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.black)
        }

        //Load Preferences - Theme
        when (prefs!!.getString("theme", "light")) {
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        checkPremium()


        maybeLoadInterstitial()



        handler = Handler(Looper.getMainLooper())
        handler!!.postDelayed({
            if (!prefsConfig!!.getBoolean("first_run", true)) {
                viewMainActivity()
            }
        }, 0)

        val button = findViewById<Button>(R.id.btn_continue)
        button.setOnClickListener {
            prefsConfig!!.setBoolean("first_run", false)
            showInterstitialOrNavigate()
        }
    }

    private fun viewMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun maybeLoadInterstitial() {
        if (prefsConfig!!.getPremium() != 0 || isAdLoading || mInterstitialAd != null) {
            return
        }
        val adRequest = AdRequest.Builder().build()
        isAdLoading = true
        InterstitialAd.load(
            this,
            "ca-app-pub-9327089289176881/9325901738",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                    isAdLoading = false
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    isAdLoading = false
                }
            }
        )
    }

    private fun showInterstitialOrNavigate() {
        val interstitial = mInterstitialAd
        if (interstitial != null) {
            interstitial.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    maybeLoadInterstitial()
                    viewMainActivity()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mInterstitialAd = null
                    viewMainActivity()
                }
            }
            interstitial.show(this)
        } else {
            viewMainActivity()
        }
    }

    private fun checkPremium() {
        billingClient = BillingClient.newBuilder(this).enablePendingPurchases()
            .setListener { _: BillingResult?, _: List<Purchase?>? -> }
            .build()
        val finalBillingClient: BillingClient = billingClient as BillingClient
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {}
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    finalBillingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.INAPP).build()
                    ) { billingResult1: BillingResult, list: List<Purchase?> ->
                        if (billingResult1.responseCode == BillingClient.BillingResponseCode.OK) {
                            if (list.isNotEmpty()) {
                                prefsConfig!!.setPremium(1) // set 1 to activate premium feature
                            } else {
                                prefsConfig!!.setPremium(0) // set 0 to de-activate premium feature

                                //fitur premium dimatikan
                                if (prefs!!.getBoolean("theme", false)) {
                                    prefs!!.setBoolean("theme", false)
                                }

                                if (prefs!!.getBoolean("custom_vibrate_duration", false)) {
                                    prefs!!.setBoolean("custom_vibrate_duration", false)
                                }
                            }
                        }
                    }
                }
            }
        })
    }
}
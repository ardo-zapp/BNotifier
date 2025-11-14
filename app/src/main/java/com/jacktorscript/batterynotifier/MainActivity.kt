package com.jacktorscript.batterynotifier

import android.Manifest
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.jacktorscript.batterynotifier.core.Prefs
import com.jacktorscript.batterynotifier.core.PrefsConfig
import com.jacktorscript.batterynotifier.databinding.ActivityMainBinding
import com.jacktorscript.batterynotifier.notification.NotificationService


class MainActivity : AppCompatActivity() {


    private var prefs: Prefs? = null
    var prefsConfig: PrefsConfig? = null
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var baseTitle: String? = null


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            //jika user klik izinkan notifikasi
            notificationUtils()
            Toast.makeText(
                this,
                getString(R.string.notification_permission_granted),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            //jika user klik tolak notifikasi
            notificationBlockedSB()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        init()

        requestNotification()
        notificationUtils()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = findNavController(R.id.nav_host_fragment_activity_main)

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

        //toolbar
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(topAppBar)

        baseTitle = if (prefsConfig!!.getPremium() == 1) {
            getString(R.string.app_name) + " PRO"
        } else {
            getString(R.string.app_name)
        }
        supportActionBar?.title = baseTitle

        topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_home) {
                supportActionBar?.title = baseTitle
                topAppBar.navigationIcon = null
            } else {
                supportActionBar?.title = destination.label
                topAppBar.setNavigationIcon(R.drawable.ic_arrow_back_24)
            }
        }

        //Tombol back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!navController.popBackStack()) {
                    finish()
                }
            }
        })

        // Tampilkan iklan banner jika bukan premium
        if (prefsConfig!!.getPremium() == 0) {
            loadBannerAd()
        } else {
            binding.adView.visibility = View.GONE
        }

    }

    //Initial
    private fun init() {
        prefs = Prefs(this)
        prefsConfig = PrefsConfig(this)
    }


    /* MENU */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_app_bar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(applicationContext, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.upgrade_pro -> {
                val intent = Intent(applicationContext, PremiumActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.action_about -> {
                if (navController.currentDestination?.id != R.id.navigation_dashboard) {
                    navController.navigate(R.id.navigation_dashboard)
                }
                return true
            }
            R.id.more_apps -> {
                moreApps()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun moreApps() {
        try {
            //Jika Terdapat Google PlayStore pada Perangkat Android
            //Maka akan langsung terhubung dengan PlayStore Tersebut
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://search?q=pub:Jacktor")
                )
            )
        } catch (ex: ActivityNotFoundException) {
            //Jika tidak, maka akan terhubung dengan Browser
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/search?q=pub:Jacktor")
                )
            )
        }
    }
    /* MENU - AKHIR */


    /* NOTIFIKASI */
    // Request izin Notifikasi
    private fun requestNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    //jika izin notifikasi tersedia
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    notificationBlockedSB()
                }
                else -> {
                    // The registered ActivityResultCallback gets the result of this request
                    requestPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            }
        }

    }

    // Tampilkan snackbar jika izin notifikasi ditolak
    private fun notificationBlockedSB() {
        Snackbar.make(
            findViewById(R.id.battery_info_grid),
            getString(R.string.notification_permission_blocked_SB),
            Snackbar.LENGTH_LONG
        ).setAction(R.string.settings) {
            // Responds to click on the action
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val uri: Uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
            .setDuration(5000)
            .show()
    }

    // Layanan notifikasi
    private fun notificationUtils() {
        //Foreground Service

        if (prefs!!.getBoolean("notification_service", true)) {
            if (!isServiceRunning(NotificationService::class.java)) {
                NotificationService.startService(this)
            }
        } else {
            NotificationService.stopService(this)
        }
    }
    /* NOTIFIKASI - AKHIR */


    //tampilkan iklan banner
    private fun loadBannerAd() {
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }


    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }


}


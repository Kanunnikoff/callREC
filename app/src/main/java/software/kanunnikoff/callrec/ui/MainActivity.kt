package software.kanunnikoff.callrec.ui

import android.content.Context
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import software.kanunnikoff.callrec.core.MyMediaRecorder
import android.util.DisplayMetrics
import android.content.Intent
import android.content.IntentFilter
import software.kanunnikoff.callrec.core.PermissionManager
import software.kanunnikoff.callrec.core.PermissionManager.PERMISSIONS_CODE
import java.util.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.media.ThumbnailUtils
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.annotation.UiThread
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import com.android.billingclient.api.BillingClient
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import software.kanunnikoff.callrec.R
import software.kanunnikoff.callrec.billing.BillingManager
import software.kanunnikoff.callrec.billing.BillingManager.BILLING_MANAGER_NOT_INITIALIZED
import software.kanunnikoff.callrec.billing.BillingProvider
import software.kanunnikoff.callrec.billing.MainViewController
import software.kanunnikoff.callrec.core.Core
import software.kanunnikoff.callrec.model.Record
import software.kanunnikoff.callrec.receiver.BillingBroadcastReceiver
import software.kanunnikoff.callrec.service.MyIntentService
import java.io.ByteArrayOutputStream
import java.io.File


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, BillingProvider {
    val recorder = MyMediaRecorder()
    private val displayMetrics = DisplayMetrics()
    private var isRecording = false
    val allRecordsSubFragment = AllRecordsSubFragment()
    val favoredRecordsSubFragment = FavoredRecordsSubFragment()

    var mBillingManager: BillingManager? = null
    private var mViewController: MainViewController? = null

    private val myBroadcastReceiver = BillingBroadcastReceiver(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        val viewPager = findViewById<ViewPager>(R.id.pager)
        viewPager.adapter = MyPagerAdapter(supportFragmentManager)

        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        tabLayout.addTab(tabLayout.newTab().setText(R.string.all_records_tab))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.favored_records_tab))
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position

                if (tab.position == RecordsFragment.ALL_RECORDS_TAB) {
                    if (allRecordsSubFragment.adapter!!.records.isEmpty()) {
                        allRecordsSubFragment.loadRecords()
                    }
                } else {
                    if (favoredRecordsSubFragment.adapter!!.records.isEmpty()) {
                        favoredRecordsSubFragment.loadRecords()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })

        Core.init(this)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        if (Core.isFirstLaunch) {
            if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_HIGH)) {
                val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)

                Core.setAudioChannels(profile.audioChannels)
                Core.setAudioSamplingRate(profile.audioSampleRate)
                Core.setAudioEncodingBitRate(profile.audioBitRate)
                Core.setAudioEncoder(profile.audioCodec)
                Core.setOutputFormat(profile.fileFormat)
            }

            Core.isFirstLaunch = false
        }

        val manager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        manager.listen(MyPhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE)

// ------------------------------------------- Ads

        if (!Core.isPremiumPurchased) {
            MobileAds.initialize(applicationContext, resources.getString(R.string.admob_app_id))
            findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())
        } else {
            findViewById<AdView>(R.id.adView).visibility = View.GONE
        }

// ------------------------------------------- In-App Billing

        mViewController = MainViewController(this)
        mBillingManager = BillingManager(this, mViewController!!.updateListener)
    }

    private fun startRecording() {
        recorder.init(
                outputFormat = Core.getOutputFormat(),
                outputFile = Core.getFileNamePrefix() + Core.formatDateForFileName(Date()) + "." + (if (Core.getOutputFormat() == MediaRecorder.OutputFormat.THREE_GPP) "3gp" else "mp4"),
                audioEncoder = Core.getAudioEncoder(),
                audioEncodingBitRate = Core.getAudioEncodingBitRate(),
                audioSamplingRate = Core.getAudioSamplingRate(),
                audioChannels = Core.getAudioChannels())

        recorder.start()
        isRecording = true
        fab.setImageDrawable(resources.getDrawable(R.drawable.ic_stop_white_24px))
        startService(Intent(this@MainActivity, MyIntentService::class.java))
    }

    override fun isPremiumPurchased(): Boolean {
        return mViewController!!.isPremiumPurchased
    }

    fun onBillingManagerSetupFinished() {
        // клиент In-App Billing настроен
    }

    /**
     * Update UI to reflect model
     */
    @UiThread
    fun premiumPurchased() {  // покупка подтверждена
        Core.isPremiumPurchased = true

        if (findViewById<AdView>(R.id.adView).visibility == View.VISIBLE) {
            findViewById<AdView>(R.id.adView).visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_rate -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            }
            R.id.nav_share -> {
                val intent = Intent()
                intent.action = Intent.ACTION_SEND
                intent.putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.app_name))
                intent.putExtra(Intent.EXTRA_TEXT, "Google Play: https://play.google.com/store/apps/details?id=$packageName")
                intent.type = "text/plain"
                startActivity(Intent.createChooser(intent, resources.getString(R.string.share) + "..."))
            }
            R.id.nav_apps -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=9118553902079488918")))
            }
            R.id.nav_buy -> {
                if (!Core.isPremiumPurchased) {
                    if (mBillingManager != null && mBillingManager!!.billingClientResponseCode > BILLING_MANAGER_NOT_INITIALIZED) {
                        mBillingManager?.initiatePurchaseFlow(Core.PREMIUM_SKU_ID, BillingClient.SkuType.INAPP)
                    }
                } else {
                    Core.showToast(resources.getString(R.string.premium_purchased))
                }
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_CODE -> {
                if (grantResults.size >= 2 && grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    startRecording()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (mBillingManager != null && mBillingManager!!.billingClientResponseCode == BillingClient.BillingResponse.OK) {
            mBillingManager!!.queryPurchases()
        }

        registerReceiver(myBroadcastReceiver, IntentFilter("com.android.vending.billing.PURCHASES_UPDATED"))
    }

    override fun onPause() {
        unregisterReceiver(myBroadcastReceiver)
        super.onPause()
    }

    public override fun onDestroy() {
        if (isRecording) {
            fab.performClick()
        }

        mBillingManager?.destroy()
        super.onDestroy()
    }

    private inner class MyPagerAdapter internal constructor(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                ALL_RECORDS_TAB -> allRecordsSubFragment
                else -> favoredRecordsSubFragment
            }
        }

        override fun getCount(): Int {
            return 2
        }
    }

    internal inner class MyPhoneStateListener : PhoneStateListener() {

        override fun onCallStateChanged(state: Int, incomingNumber: String) {
            super.onCallStateChanged(state, incomingNumber)

            Log.i(Core.APP_TAG, "*** incoming number: $incomingNumber")

            when (state) {
                TelephonyManager.CALL_STATE_IDLE -> {
                    if (isRecording) {   // shouldRecord &&
                        Log.i(Core.APP_TAG, "*** stopped recording")
                        recorder.stop()
                        isRecording = false
                        stopService(Intent(this@MainActivity, MyIntentService::class.java))

                        val size: String
                        var length = File(recorder.outputFileAbsolutePath).length() / 1024 / 1024   // megabytes

                        if (length > 0) {
                            size = length.toString() + resources.getString(R.string.mb)
                        } else {
                            length = File(recorder.outputFileAbsolutePath).length() / 1024   // kilobytes

                            if (length > 0) {
                                size = length.toString() + resources.getString(R.string.kb)
                            } else {
                                length = File(recorder.outputFileAbsolutePath).length()   // bytes
                                size = length.toString() + resources.getString(R.string.b)
                            }
                        }

                        var bitmap = ThumbnailUtils.createVideoThumbnail(recorder.outputFileAbsolutePath, MediaStore.Images.Thumbnails.MINI_KIND)
                        val stream = ByteArrayOutputStream()

                        if (bitmap == null) {
                            bitmap = BitmapFactory.decodeResource(resources, R.drawable.no_thumbnail)
                        }

                        bitmap.compress(Bitmap.CompressFormat.PNG, 70, stream)

                        val record = Record(
                                -1,
                                Core.getRecordTitlePrefix() + " #${Core.recordNumber}",
                                Core.outputFormatString(),
                                recorder.outputFileAbsolutePath,
                                Core.audioEncoderString(),
                                Core.getAudioEncodingBitRate().toString(),
                                Core.getAudioSamplingRate().toString(),
                                Core.audioChannelsString(),
                                0,
                                size,
                                Core.formatDuration(recorder.duration / 1000),
                                Core.formatDate(recorder.date),
                                stream.toByteArray()
                        )

                        record.id = Core.insertRecord(record)
                        allRecordsSubFragment.adapter!!.records.add(0, record)
                        allRecordsSubFragment.adapter?.notifyDataSetChanged()
                        Core.recordNumber++
                    }
                }

                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    if (!isRecording) {  // shouldRecord &&
                        if (PermissionManager.hasPermissions(this@MainActivity)) {
                            startRecording()
                            Log.i(Core.APP_TAG, "*** started recording")
                        }
                    }
                }

                TelephonyManager.CALL_STATE_RINGING -> {
                }

                else -> {
                }
            }

        }
    }

    companion object {
        const val ALL_RECORDS_TAB = 0
    }
}

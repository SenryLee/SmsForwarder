package cn.ppps.forwarder.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import cn.ppps.forwarder.R
import cn.ppps.forwarder.core.BaseActivity
import cn.ppps.forwarder.database.ext.ioThread
import cn.ppps.forwarder.databinding.ActivityMainBinding
import cn.ppps.forwarder.fragment.CourtSettingsFragment
import cn.ppps.forwarder.fragment.LogsFragment
import cn.ppps.forwarder.fragment.RulesFragment
import cn.ppps.forwarder.service.ForegroundService
import cn.ppps.forwarder.utils.ACTION_START
import cn.ppps.forwarder.utils.Log
import cn.ppps.forwarder.utils.SharedPreference
import cn.ppps.forwarder.utils.court.CourtEmailHelper
import cn.ppps.forwarder.utils.court.CourtModeBootstrap
import com.xuexiang.xui.utils.WidgetUtils
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog

@Suppress("PrivatePropertyName", "unused", "DEPRECATION")
class MainActivity : BaseActivity<ActivityMainBinding?>() {

    private val POS_LOG = 0
    private val POS_RULE = 1
    private val REQ_RUNTIME = 0xC01

    private lateinit var mTabLayout: TabLayout
    private var setupHintShown: Boolean by SharedPreference("court_setup_hint_shown", false)

    override fun viewBindingInflate(inflater: LayoutInflater?): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            CourtModeBootstrap.applyLiteDefaultsIfNeeded()
            initViews()
            ensureCourtBootstrapAsync()
            requestRuntimePermissions()
            maybeShowSetupHint()
        } catch (e: Exception) {
            Log.e("MainActivity", "onCreate fatal: ${e.message}")
            e.printStackTrace()
        }
    }

    override val isSupportSlideBack: Boolean
        get() = false

    private fun initViews() {
        WidgetUtils.clearActivityBackground(this)
        mTabLayout = binding!!.tabs
        WidgetUtils.addTabWithoutRipple(
            mTabLayout,
            getString(R.string.menu_logs),
            R.drawable.selector_icon_tabbar_logs,
        )
        WidgetUtils.addTabWithoutRipple(
            mTabLayout,
            getString(R.string.menu_rules),
            R.drawable.selector_icon_tabbar_rules,
        )
        WidgetUtils.setTabLayoutTextFont(mTabLayout)
        switchPage(LogsFragment::class.java)
        mTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    POS_LOG -> switchPage(LogsFragment::class.java)
                    POS_RULE -> switchPage(RulesFragment::class.java)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun ensureCourtBootstrapAsync() {
        ioThread {
            try {
                CourtEmailHelper.ensurePlaceholderSender()
                CourtModeBootstrap.seedCourtRulesInternalForSettings(false)
            } catch (e: Exception) {
                Log.e("MainActivity", "bootstrap: ${e.message}")
            }
        }
    }

    private fun requestRuntimePermissions() {
        val needed = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startForwardServiceSafe()
        } else {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQ_RUNTIME)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        startForwardServiceSafe()
    }

    private fun startForwardServiceSafe() {
        try {
            if (ForegroundService.isRunning) return
            val serviceIntent = Intent(this, ForegroundService::class.java)
            serviceIntent.action = ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "start service: ${e.message}")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addCategory(Intent.CATEGORY_HOME)
        startActivity(intent)
    }

    fun openMenu() {
        openSettings()
    }

    fun closeMenu() {}

    fun isMenuOpen(): Boolean = false

    fun openSettings() {
        try {
            openNewPage(CourtSettingsFragment::class.java)
        } catch (e: Exception) {
            Log.e("MainActivity", "openSettings: ${e.message}")
        }
    }

    private fun maybeShowSetupHint() {
        if (setupHintShown) return
        val emailConfigured = try {
            CourtEmailHelper.loadEmail().isNotBlank()
        } catch (_: Exception) {
            false
        }
        if (emailConfigured && CourtModeBootstrap.isRulesSeeded()) {
            setupHintShown = true
            return
        }
        try {
            MaterialDialog.Builder(this)
                .title(R.string.court_setup_title)
                .content(R.string.court_setup_content)
                .positiveText(R.string.court_setup_go)
                .negativeText(R.string.court_setup_later)
                .onPositive { _: MaterialDialog?, _: DialogAction? ->
                    setupHintShown = true
                    openSettings()
                }
                .onNegative { _: MaterialDialog?, _: DialogAction? ->
                    setupHintShown = true
                }
                .cancelable(true)
                .show()
        } catch (e: Exception) {
            Log.e("MainActivity", "setup hint: ${e.message}")
            setupHintShown = true
        }
    }
}

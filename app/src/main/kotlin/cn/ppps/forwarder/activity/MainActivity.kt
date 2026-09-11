package cn.ppps.forwarder.activity

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import com.google.android.material.tabs.TabLayout
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.hjq.permissions.permission.base.IPermission
import cn.ppps.forwarder.App
import cn.ppps.forwarder.R
import cn.ppps.forwarder.core.BaseActivity
import cn.ppps.forwarder.databinding.ActivityMainBinding
import cn.ppps.forwarder.fragment.CourtSettingsFragment
import cn.ppps.forwarder.fragment.LogsFragment
import cn.ppps.forwarder.fragment.RulesFragment
import cn.ppps.forwarder.service.ForegroundService
import cn.ppps.forwarder.utils.ACTION_START
import cn.ppps.forwarder.utils.SettingUtils
import cn.ppps.forwarder.utils.SharedPreference
import cn.ppps.forwarder.utils.XToastUtils
import cn.ppps.forwarder.utils.court.CourtEmailHelper
import cn.ppps.forwarder.utils.court.CourtModeBootstrap
import com.xuexiang.xui.utils.WidgetUtils
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog

@Suppress("PrivatePropertyName", "unused", "DEPRECATION")
class MainActivity : BaseActivity<ActivityMainBinding?>() {

    private val POS_LOG = 0
    private val POS_RULE = 1

    private lateinit var mTabLayout: TabLayout
    private var setupHintShown: Boolean by SharedPreference("court_setup_hint_shown", false)

    override fun viewBindingInflate(inflater: LayoutInflater?): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CourtModeBootstrap.applyLiteDefaultsIfNeeded()
        initViews()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && SettingUtils.enableExcludeFromRecents) {
            val am = App.context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val tasks = am.appTasks
            if (!tasks.isNullOrEmpty()) {
                tasks[0].setExcludeFromRecents(true)
            }
        }

        XXPermissions.with(this)
            .permission(PermissionLists.getNotificationServicePermission())
            .permission(PermissionLists.getPostNotificationsPermission())
            .request(object : OnPermissionCallback {
                override fun onResult(
                    grantedList: MutableList<IPermission>,
                    deniedList: MutableList<IPermission>,
                ) {
                    if (deniedList.isNotEmpty()) {
                        XToastUtils.error(R.string.tips_notification)
                        return
                    }
                    if (!ForegroundService.isRunning) {
                        val serviceIntent = Intent(getTopActivity(), ForegroundService::class.java)
                        serviceIntent.action = ACTION_START
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent)
                        } else {
                            startService(serviceIntent)
                        }
                    }
                }
            })

        maybeShowSetupHint()
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
        openNewPage(CourtSettingsFragment::class.java)
    }

    private fun maybeShowSetupHint() {
        if (setupHintShown) return
        if (CourtEmailHelper.findBuiltinSender() != null && CourtModeBootstrap.isRulesSeeded()) {
            setupHintShown = true
            return
        }
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
            .cancelable(false)
            .show()
    }
}

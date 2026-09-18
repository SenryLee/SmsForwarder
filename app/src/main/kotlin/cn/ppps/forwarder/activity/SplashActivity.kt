package cn.ppps.forwarder.activity

import android.annotation.SuppressLint
import android.view.KeyEvent
import cn.ppps.forwarder.R
import cn.ppps.forwarder.utils.SettingUtils.Companion.isAgreePrivacy
import com.xuexiang.xui.utils.KeyboardUtils
import com.xuexiang.xui.widget.activity.BaseSplashActivity
import com.xuexiang.xutil.app.ActivityUtils
import me.jessyan.autosize.internal.CancelAdapt

/**
 * 精简启动页：去掉复杂隐私弹窗链路，首次进入自动标记同意并进入主界面，降低闪退风险。
 */
@Suppress("PropertyName")
@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseSplashActivity(), CancelAdapt {

    val TAG: String = SplashActivity::class.java.simpleName

    override fun getSplashDurationMillis(): Long = 400

    override fun onCreateActivity() {
        initSplashView(R.drawable.xui_config_bg_splash)
        startSplash(false)
    }

    override fun onSplashFinished() {
        try {
            if (!isAgreePrivacy) {
                isAgreePrivacy = true
            }
        } catch (_: Exception) {
            // ignore
        }
        try {
            ActivityUtils.startActivity(MainActivity::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return KeyboardUtils.onDisableBackKeyDown(keyCode) && super.onKeyDown(keyCode, event)
    }
}

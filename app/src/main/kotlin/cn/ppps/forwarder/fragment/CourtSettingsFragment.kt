package cn.ppps.forwarder.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import cn.ppps.forwarder.BuildConfig
import cn.ppps.forwarder.R
import cn.ppps.forwarder.core.BaseFragment
import cn.ppps.forwarder.database.ext.ioThread
import cn.ppps.forwarder.databinding.FragmentCourtSettingsBinding
import cn.ppps.forwarder.utils.SettingUtils
import cn.ppps.forwarder.utils.XToastUtils
import cn.ppps.forwarder.utils.court.CourtEmailHelper
import cn.ppps.forwarder.utils.court.CourtModeBootstrap
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xui.widget.actionbar.TitleBar

@Page(name = "设置")
class CourtSettingsFragment : BaseFragment<FragmentCourtSettingsBinding?>() {

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentCourtSettingsBinding {
        return FragmentCourtSettingsBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        return super.initTitle()!!.setImmersive(false).apply {
            setTitle(R.string.menu_settings)
            setLeftClickListener { popToBack() }
        }
    }

    override fun initViews() {
        binding!!.etEmail.setText(CourtEmailHelper.loadEmail())
        binding!!.sbEnableSms.isChecked = SettingUtils.enableSms
        binding!!.tvVersion.text = getString(
            R.string.court_settings_version,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
        )
    }

    override fun initListeners() {
        binding!!.sbEnableSms.setOnCheckedChangeListener { _, checked ->
            SettingUtils.enableSms = checked
        }
        binding!!.btnSaveEmail.setOnClickListener { onSaveEmail() }
        binding!!.btnApplyRules.setOnClickListener { onApplyRules() }
    }

    @SingleClick
    private fun onSaveEmail() {
        val email = binding!!.etEmail.text.toString().trim()
        val code = binding!!.etAuthCode.text.toString().trim()
        ioThread {
            try {
                CourtEmailHelper.saveEmail(email, code)
                SettingUtils.enableSms = true
                CourtModeBootstrap.seedCourtRulesInternalForSettings(true)
                requireActivity().runOnUiThread {
                    binding!!.sbEnableSms.isChecked = true
                    XToastUtils.success(R.string.court_settings_save_ok)
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    XToastUtils.error(e.message ?: getString(R.string.court_settings_save_fail))
                }
            }
        }
    }

    @SingleClick
    private fun onApplyRules() {
        CourtModeBootstrap.seedCourtRulesAsync(true)
    }
}

package cn.ppps.forwarder.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import cn.ppps.forwarder.BuildConfig
import cn.ppps.forwarder.R
import cn.ppps.forwarder.core.BaseFragment
import cn.ppps.forwarder.database.ext.ioThread
import cn.ppps.forwarder.databinding.FragmentCourtSettingsBinding
import cn.ppps.forwarder.utils.Log
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
        return try {
            super.initTitle()!!.setImmersive(false).apply {
                setTitle(R.string.menu_settings)
                setLeftClickListener { popToBack() }
            }
        } catch (e: Exception) {
            Log.e("CourtSettings", "initTitle: ${e.message}")
            null
        }
    }

    override fun initViews() {
        try {
            binding!!.etEmail.setText(CourtEmailHelper.loadEmail())
            binding!!.sbEnableSms.isChecked = SettingUtils.enableSms
            binding!!.tvVersion.text = getString(
                R.string.court_settings_version,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
            )
        } catch (e: Exception) {
            Log.e("CourtSettings", "initViews: ${e.message}")
        }
    }

    override fun initListeners() {
        try {
            binding!!.sbEnableSms.setOnCheckedChangeListener { _, checked ->
                SettingUtils.enableSms = checked
            }
            binding!!.btnSaveEmail.setOnClickListener { onSaveEmail() }
            binding!!.btnApplyRules.setOnClickListener { onApplyRules() }
        } catch (e: Exception) {
            Log.e("CourtSettings", "initListeners: ${e.message}")
        }
    }

    @SingleClick
    private fun onSaveEmail() {
        val email = binding!!.etEmail.text?.toString()?.trim().orEmpty()
        val code = binding!!.etAuthCode.text?.toString()?.trim().orEmpty()
        ioThread {
            try {
                CourtEmailHelper.saveEmail(email, code)
                SettingUtils.enableSms = true
                CourtModeBootstrap.seedCourtRulesInternalForSettings(true)
                requireActivity().runOnUiThread {
                    try {
                        binding!!.sbEnableSms.isChecked = true
                    } catch (_: Exception) {
                    }
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
        try {
            CourtModeBootstrap.seedCourtRulesAsync(true)
        } catch (e: Exception) {
            XToastUtils.error(e.message ?: getString(R.string.court_mode_failed))
        }
    }
}

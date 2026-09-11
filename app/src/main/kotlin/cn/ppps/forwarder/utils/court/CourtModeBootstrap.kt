package cn.ppps.forwarder.utils.court

import android.os.Handler
import android.os.Looper
import cn.ppps.forwarder.R
import cn.ppps.forwarder.core.Core
import cn.ppps.forwarder.database.entity.Rule
import cn.ppps.forwarder.database.entity.Sender
import cn.ppps.forwarder.database.ext.ioThread
import cn.ppps.forwarder.utils.CHECK_CONTAIN
import cn.ppps.forwarder.utils.CHECK_SIM_SLOT_ALL
import cn.ppps.forwarder.utils.FILED_MULTI_MATCH
import cn.ppps.forwarder.utils.FILED_PHONE_NUM
import cn.ppps.forwarder.utils.Log
import cn.ppps.forwarder.utils.SettingUtils
import cn.ppps.forwarder.utils.SharedPreference
import cn.ppps.forwarder.utils.TYPE_EMAIL
import cn.ppps.forwarder.utils.XToastUtils
import com.xuexiang.xutil.resource.ResUtils

object CourtModeBootstrap {
    private const val TAG = "CourtModeBootstrap"
    private const val SP_DEFAULTS = "court_defaults_applied"
    private const val SP_RULES = "court_rules_seeded"
    const val RULE_TITLE_NUMBER = "法院号码·12368"
    const val RULE_TITLE_KEYWORD = "法院关键词·案号送达"

    private var defaultsApplied: Boolean by SharedPreference(SP_DEFAULTS, false)
    private var rulesSeeded: Boolean by SharedPreference(SP_RULES, false)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun applyLiteDefaultsIfNeeded() {
        if (defaultsApplied) return
        try {
            SettingUtils.enableSms = true
            SettingUtils.enablePhone = false
            SettingUtils.enableAppNotify = false
            SettingUtils.enableSmsCommand = false
            SettingUtils.enableSmsTemplate = true
            SettingUtils.smsTemplate = CourtSmsEnricher.defaultSmsTemplate()
            SettingUtils.enablePureClientMode = false
            SettingUtils.enablePureTaskMode = false
            defaultsApplied = true
            Log.i(TAG, "defaults applied")
        } catch (e: Exception) {
            Log.e(TAG, "applyLiteDefaultsIfNeeded: ${e.message}")
        }
    }

    fun isRulesSeeded(): Boolean = rulesSeeded

    fun seedCourtRulesAsync(force: Boolean = false) {
        ioThread {
            val n = seedCourtRulesInternal(force)
            mainHandler.post {
                when (n) {
                    0 -> XToastUtils.warning(R.string.court_mode_no_sender)
                    -1 -> XToastUtils.error(R.string.court_mode_failed)
                    else -> XToastUtils.success(
                        String.format(ResUtils.getString(R.string.court_mode_success), n)
                    )
                }
            }
        }
    }

    fun seedCourtRulesInternalForSettings(force: Boolean = true): Int =
        seedCourtRulesInternal(force)

    private fun seedCourtRulesInternal(force: Boolean): Int {
        if (rulesSeeded && !force) return 0
        return try {
            val senders = Core.sender.getAllNonCache()
                .filter { it.status == 1 && it.type == TYPE_EMAIL }
            if (senders.isEmpty()) return 0
            val primary = CourtEmailHelper.findBuiltinSender() ?: senders.first()
            val existing = Core.rule.getAllNonCache()
            if (force) {
                existing.filter {
                    it.title == RULE_TITLE_NUMBER || it.title == RULE_TITLE_KEYWORD
                }.forEach { Core.rule.delete(it.id) }
            } else if (existing.any {
                    it.title == RULE_TITLE_NUMBER || it.title == RULE_TITLE_KEYWORD
                }
            ) {
                rulesSeeded = true
                return 0
            }
            val template = CourtSmsEnricher.defaultSmsTemplate()
            insertRule(RULE_TITLE_NUMBER, FILED_PHONE_NUM, "12368", primary, template)
            insertRule(
                RULE_TITLE_KEYWORD,
                FILED_MULTI_MATCH,
                CourtSmsEnricher.defaultMultiMatchRule(),
                primary,
                template,
            )
            SettingUtils.enableSms = true
            SettingUtils.enableSmsTemplate = true
            SettingUtils.smsTemplate = template
            rulesSeeded = true
            2
        } catch (e: Exception) {
            Log.e(TAG, "seedCourtRulesInternal: ${e.message}")
            -1
        }
    }

    private fun insertRule(
        title: String,
        filed: String,
        value: String,
        sender: Sender,
        smsTemplate: String,
    ) {
        Core.rule.insert(
            Rule(
                id = 0,
                type = "sms",
                filed = filed,
                check = CHECK_CONTAIN,
                value = value,
                senderId = sender.id,
                smsTemplate = smsTemplate,
                regexReplace = "",
                simSlot = CHECK_SIM_SLOT_ALL,
                status = 1,
                senderList = listOf(sender),
                senderLogic = "ALL",
                title = title,
            )
        )
    }
}

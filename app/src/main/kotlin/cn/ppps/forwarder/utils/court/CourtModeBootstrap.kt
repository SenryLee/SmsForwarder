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
import cn.ppps.forwarder.utils.SharedPreference
import cn.ppps.forwarder.utils.SettingUtils
import cn.ppps.forwarder.utils.TYPE_EMAIL
import cn.ppps.forwarder.utils.TYPE_WEBHOOK
import cn.ppps.forwarder.utils.XToastUtils
import com.xuexiang.xutil.resource.ResUtils.getString

/**
 * 法院送达专用版：默认开关、邮件模板、一键写入转发规则。
 */
object CourtModeBootstrap {
    private const val TAG = "CourtModeBootstrap"
    private const val SP_COURT_DEFAULTS_APPLIED = "court_defaults_applied"
    private const val SP_COURT_RULES_SEEDED = "court_rules_seeded"
    private const val RULE_TITLE_NUMBER = "法院号码·12368"
    private const val RULE_TITLE_KEYWORD = "法院关键词·案号送达"

    private var defaultsApplied: Boolean by SharedPreference(SP_COURT_DEFAULTS_APPLIED, false)
    private var rulesSeeded: Boolean by SharedPreference(SP_COURT_RULES_SEEDED, false)
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
            Log.i(TAG, "court lite defaults applied")
        } catch (e: Exception) {
            Log.e(TAG, "applyLiteDefaultsIfNeeded: ${e.message}")
        }
    }

    fun isRulesSeeded(): Boolean = rulesSeeded

    /**
     * 异步写入法院专用规则，结果用 Toast 提示。
     */
    fun seedCourtRulesAsync(force: Boolean = false) {
        ioThread {
            val n = seedCourtRulesInternal(force)
            mainHandler.post {
                when (n) {
                    0 -> XToastUtils.warning(R.string.court_mode_no_sender)
                    -1 -> XToastUtils.error(R.string.court_mode_failed)
                    else -> XToastUtils.success(getString(R.string.court_mode_success, n))
                }
            }
        }
    }

    private fun seedCourtRulesInternal(force: Boolean): Int {
        if (rulesSeeded && !force) return 0
        return try {
            val senders = Core.sender.getAllNonCache()
                .filter { it.status == 1 && (it.type == TYPE_EMAIL || it.type == TYPE_WEBHOOK) }
            if (senders.isEmpty()) {
                Log.w(TAG, "no email/webhook sender; skip seeding rules")
                return 0
            }
            val primary = senders.first()
            val existing = Core.rule.getAllNonCache()
            if (force) {
                existing.filter { it.title == RULE_TITLE_NUMBER || it.title == RULE_TITLE_KEYWORD }
                    .forEach { Core.rule.delete(it.id) }
            } else if (existing.any { it.title == RULE_TITLE_NUMBER || it.title == RULE_TITLE_KEYWORD }) {
                rulesSeeded = true
                return 0
            }

            val template = CourtSmsEnricher.defaultSmsTemplate()
            insertRule(
                title = RULE_TITLE_NUMBER,
                filed = FILED_PHONE_NUM,
                check = CHECK_CONTAIN,
                value = "12368",
                sender = primary,
                smsTemplate = template,
            )
            insertRule(
                title = RULE_TITLE_KEYWORD,
                filed = FILED_MULTI_MATCH,
                check = CHECK_CONTAIN,
                value = CourtSmsEnricher.defaultMultiMatchRule(),
                sender = primary,
                smsTemplate = template,
            )
            SettingUtils.enableSms = true
            SettingUtils.enableSmsTemplate = true
            SettingUtils.smsTemplate = template
            rulesSeeded = true
            Log.i(TAG, "court rules seeded with sender=${primary.name}")
            2
        } catch (e: Exception) {
            Log.e(TAG, "seedCourtRules: ${e.message}")
            -1
        }
    }

    private fun insertRule(
        title: String,
        filed: String,
        check: String,
        value: String,
        sender: Sender,
        smsTemplate: String,
    ) {
        val rule = Rule(
            id = 0,
            type = "sms",
            filed = filed,
            check = check,
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
        Core.rule.insert(rule)
    }
}

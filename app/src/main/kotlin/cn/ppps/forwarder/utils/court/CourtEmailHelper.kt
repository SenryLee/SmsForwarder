package cn.ppps.forwarder.utils.court

import com.google.gson.Gson
import cn.ppps.forwarder.core.Core
import cn.ppps.forwarder.database.entity.Sender
import cn.ppps.forwarder.entity.setting.EmailSetting
import cn.ppps.forwarder.utils.Log
import cn.ppps.forwarder.utils.TYPE_EMAIL

object CourtEmailHelper {
    private const val TAG = "CourtEmailHelper"
    const val SENDER_NAME = "法院邮箱"

    data class SmtpProfile(
        val mailType: String,
        val host: String,
        val port: String,
        val ssl: Boolean,
        val startTls: Boolean,
        val splitLocalPart: Boolean,
    )

    fun resolveSmtp(email: String): SmtpProfile {
        val domain = email.trim().lowercase().substringAfter("@", "")
        return when (domain) {
            "qq.com", "foxmail.com" ->
                SmtpProfile("@$domain", "smtp.qq.com", "465", true, false, true)
            "163.com" -> SmtpProfile("@163.com", "smtp.163.com", "465", true, false, true)
            "126.com" -> SmtpProfile("@126.com", "smtp.126.com", "465", true, false, true)
            "yeah.net" -> SmtpProfile("@yeah.net", "smtp.yeah.net", "465", true, false, true)
            "gmail.com" -> SmtpProfile("@gmail.com", "smtp.gmail.com", "465", true, false, true)
            "139.com" -> SmtpProfile("@139.com", "smtp.139.com", "465", true, false, true)
            "189.cn" -> SmtpProfile("@189.cn", "smtp.189.cn", "465", true, false, true)
            "sina.com" -> SmtpProfile("@sina.com", "smtp.sina.com", "465", true, false, true)
            "sina.cn" -> SmtpProfile("@sina.cn", "smtp.sina.cn", "465", true, false, true)
            "icloud.com", "me.com" ->
                SmtpProfile("", "smtp.mail.me.com", "587", false, true, false)
            else -> SmtpProfile("", "smtp.$domain", "465", true, false, false)
        }
    }

    fun findBuiltinSender(): Sender? {
        val all = Core.sender.getAllNonCache()
        return all.firstOrNull { it.type == TYPE_EMAIL && it.name == SENDER_NAME }
            ?: all.firstOrNull { it.type == TYPE_EMAIL && it.status == 1 }
    }

    fun loadEmail(): String {
        val sender = findBuiltinSender() ?: return ""
        return try {
            val s = Gson().fromJson(sender.jsonSetting, EmailSetting::class.java) ?: return ""
            when {
                s.toEmail.isNotBlank() -> s.toEmail
                s.mailType.isNotBlank() && !s.fromEmail.contains("@") -> s.fromEmail + s.mailType
                else -> s.fromEmail
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadEmail: ${e.message}")
            ""
        }
    }

    fun saveEmail(emailRaw: String, authCode: String): Sender {
        val email = emailRaw.trim()
        require(email.contains("@") && email.substringAfter("@").contains(".")) {
            "邮箱格式不正确"
        }
        require(authCode.isNotBlank()) {
            "请填写邮箱授权码（不是登录密码）"
        }
        val profile = resolveSmtp(email)
        val local = email.substringBefore("@")
        val setting = EmailSetting(
            mailType = profile.mailType,
            fromEmail = if (profile.splitLocalPart) local else email,
            pwd = authCode.trim(),
            nickname = "法院短信转发器",
            host = profile.host,
            port = profile.port,
            ssl = profile.ssl,
            startTls = profile.startTls,
            title = CourtSmsEnricher.defaultTitleTemplate(),
            recipients = mutableMapOf(email to Pair("", "")),
            toEmail = email,
        )
        val json = Gson().toJson(setting)
        val existing = findBuiltinSender()
        return if (existing == null) {
            Core.sender.insert(
                Sender(
                    id = 0,
                    type = TYPE_EMAIL,
                    name = SENDER_NAME,
                    jsonSetting = json,
                    status = 1,
                )
            )
            findBuiltinSender()!!
        } else {
            existing.name = SENDER_NAME
            existing.jsonSetting = json
            existing.status = 1
            Core.sender.update(existing)
            existing
        }
    }
}

package cn.ppps.forwarder.utils.court

/**
 * 法院送达短信轻量解析：提取案号、链接、日期，供邮件模板与 WorkBuddy Skill 使用。
 * 不做完整 NLP，只做高召回正则。
 */
object CourtSmsEnricher {

    private val CASE_NO_REGEX = Regex(
        """[（(]\s*20\d{2}\s*[）)][\u4e00-\u9fa5A-Za-z0-9]{0,12}[民刑行执赔他知][初终再申执管督确催破]?第?\d+号"""
    )

    private val URL_REGEX = Regex(
        """https?://[^\s<>"'，。；、）)\]]+|www\.[^\s<>"'，。；、）)\]]+""",
        RegexOption.IGNORE_CASE
    )

    private val DATE_REGEX = Regex(
        """20\d{2}\s*年\s*\d{1,2}\s*月\s*\d{1,2}\s*日(?:\s*\d{1,2}\s*[点时:：]\s*\d{0,2}\s*分?)?|\d{4}[-/.]\d{1,2}[-/.]\d{1,2}(?:\s+\d{1,2}:\d{2})?"""
    )

    private val DOWNLOAD_HINTS = listOf(
        "下载", "查阅", "电子送达", "点击链接", "登录", "文书", "pdf", "附件", "查看详情", "送达平台"
    )

    private val CALENDAR_HINTS = listOf(
        "开庭", "到庭", "出庭", "听证", "举证期限", "答辩期限", "履行期限", "截止日期",
        "前到", "前完成", "前递交", "传票", "调解", "宣判"
    )

    data class Enrichment(
        val caseNo: String,
        val links: String,
        val dates: String,
        val needDownload: String,
        val needCalendar: String,
        val summary: String,
    )

    fun enrich(from: String, content: String): Enrichment {
        val caseNos = CASE_NO_REGEX.findAll(content).map { it.value.replace("\\s".toRegex(), "") }.distinct().toList()
        val links = URL_REGEX.findAll(content).map { it.value.trimEnd('.', ',', '，', '。', '；', ';') }.distinct().toList()
        val dates = DATE_REGEX.findAll(content).map { it.value.replace("\\s+".toRegex(), "") }.distinct().toList()

        val lower = content.lowercase()
        val needDownload = if (links.isNotEmpty() || DOWNLOAD_HINTS.any { content.contains(it) || lower.contains(it) }) {
            "yes"
        } else {
            "no"
        }
        val needCalendar = if (dates.isNotEmpty() || CALENDAR_HINTS.any { content.contains(it) }) {
            "yes"
        } else {
            "no"
        }

        val summaryParts = mutableListOf<String>()
        if (caseNos.isNotEmpty()) summaryParts.add(caseNos.first())
        when {
            content.contains("开庭") -> summaryParts.add("开庭")
            content.contains("送达") -> summaryParts.add("送达")
            content.contains("执行") -> summaryParts.add("执行")
            content.contains("传票") -> summaryParts.add("传票")
            else -> if (from.contains("12368")) summaryParts.add("法院短信")
        }

        return Enrichment(
            caseNo = caseNos.joinToString(" | ").ifBlank { "无" },
            links = if (links.isEmpty()) "无" else links.joinToString("\n"),
            dates = if (dates.isEmpty()) "无" else dates.joinToString(" | "),
            needDownload = needDownload,
            needCalendar = needCalendar,
            summary = summaryParts.joinToString(" · ").ifBlank { content.take(24) },
        )
    }

    /** 法院模式默认邮件正文模板（含 WorkBuddy 可解析字段） */
    fun defaultSmsTemplate(): String = """
来源号码：{{FROM}}
卡槽：{{CARD_SLOT}}
接收时间：{{RECEIVE_TIME}}
案号：{{COURT_CASE_NO}}
疑似期限/开庭：{{COURT_DATES}}
NEED_DOWNLOAD={{NEED_DOWNLOAD}}
NEED_CALENDAR={{NEED_CALENDAR}}
检测到的链接：
{{COURT_LINKS}}
—— 原文 ——
{{SMS}}
""".trimIndent()

    /** 法院模式默认邮件主题模板 */
    fun defaultTitleTemplate(): String =
        "[法院送达][{{CARD_SLOT}}] {{FROM}} | {{COURT_SUMMARY}}"

    /** 多重匹配：号码 12368，或（法院相关词 + 案号） */
    fun defaultMultiMatchRule(): String = """
并且 是 手机号 相等 12368
或者 是 手机号 包含 12368
或者 是 短信内容 包含 人民法院
 并且 是 短信内容 正则匹配 [（(]20\d{2}[）)][\u4e00-\u9fa5A-Za-z0-9]{0,12}[民刑行执赔他知][初终再申执管督确催破]?第?\d+号
或者 是 短信内容 包含 电子送达
 并且 是 短信内容 包含 法院
或者 是 短信内容 包含 开庭
 并且 是 短信内容 包含 传票
""".trimIndent()
}

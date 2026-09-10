# 送达短信助手（Court SMS Helper）

> **SenryLee fork** · 基于 [pppscn/SmsForwarder](https://github.com/pppscn/SmsForwarder) 的法院送达专用轻量版  
> 仓库：https://github.com/SenryLee/SmsForwarder  
> 应用包名：`cn.senrylee.courtsms`（与上游 SmsForwarder 可并存安装）

把法院 **12368 / 送达类短信** 精准转发到邮箱，再由 **WorkBuddy** 每天傍晚汇总：下载提醒、日程提醒、固定文件夹落盘。

---

## 和上游 SmsForwarder 的区别

| 项目 | 上游 SmsForwarder | 本 fork「送达短信助手」 |
|---|---|---|
| 定位 | 通用短信/来电/通知转发 | **仅法院相关短信** |
| 监听 | 短信 + 来电 + App 通知 | **仅短信** |
| 发送通道 | 钉钉/企微/飞书/Bark/…十余种 | **邮箱（主）+ Webhook（可选）** |
| 远程控制 / Frpc / 自动任务 | 有 | **已从入口与 Manifest 下线** |
| 默认规则 | 需自行配置 | **一键写入 12368 + 关键词/案号规则** |
| 邮件格式 | 自由模板 | **结构化字段**，方便 WorkBuddy 解析 |
| APK 文件名 | `SmsF_…` | `CourtSms_…` |

---

## 推荐链路

```text
法院短信 → 送达短信助手(Android) → QQ/Foxmail 邮箱
                                      ↓
                         WorkBuddy 每天 18:00 读信
                                      ↓
                    汇总清单 + 下载提醒 + 日历/滴答 + 固定文件夹
```

详细 Skill 与自动化提示词见：[workbuddy-skill/court-sms-digest/](workbuddy-skill/court-sms-digest/)

---

## 手机端快速上手

1. 安装本 fork 的 APK（Release 或自行 `assembleRelease`）。
2. **通用设置**：开启「转发短信」，授予短信/通知相关权限，并忽略电池优化。
3. **发送通道**：添加「邮箱」，推荐 QQ/Foxmail；主题模板会预填  
   `[法院送达][{{CARD_SLOT}}] {{FROM}} | {{COURT_SUMMARY}}`
4. **关于页** 点「一键法院规则」（或首次启动引导），写入默认规则。
5. 用法院测试短信或手动「重试/测试」确认邮箱收到带 `NEED_DOWNLOAD` / `NEED_CALENDAR` 字段的邮件。

### 默认会匹配什么？

- 号码包含 **12368**
- 或「人民法院 + 案号」/「电子送达 + 法院」/「开庭 + 传票」等组合（可在规则页微调）

### 邮件正文关键字段（给 WorkBuddy）

- `案号` / `疑似期限/开庭` / `检测到的链接`
- `NEED_DOWNLOAD=yes|no`
- `NEED_CALENDAR=yes|no`
- 原文全文

---

## WorkBuddy 端

1. 连接 **QQ 邮箱** 连接器（与手机发件/收件邮箱一致或同一收件箱）。
2. 导入或复制 [workbuddy-skill/court-sms-digest/SKILL.md](workbuddy-skill/court-sms-digest/SKILL.md)。
3. 用 [automation-prompt.md](workbuddy-skill/court-sms-digest/automation-prompt.md) 创建每天 **18:00** 的自动化。
4. 指定落盘目录，例如 `~/法院送达/`。

---

## 下载与构建

### 下载

- GitHub Releases：https://github.com/SenryLee/SmsForwarder/releases  
- 识别文件名：`CourtSms_<version>_…apk`  
- 勿与上游 `SmsF_…` / 官方渠道包混淆。

### 自行编译

```bash
./gradlew :app:assembleDebug
# 或
./gradlew :app:assembleRelease
```

产物在 `app/build/outputs/apk/`，文件名以 `CourtSms_` 开头。

---

## 声明

- 本仓库代码基于开源 SmsForwarder，遵循原项目 LICENSE；改造仅用于个人学习与办案辅助，禁止商业贩卖。
- 不收集隐私数据；邮件内容仅发往你配置的邮箱。
- 法院短信、链接、文书下载请遵守当地法院电子送达规定；本工具不代替正式送达确认。

---

## 致谢

- 上游项目：[pppscn/SmsForwarder](https://github.com/pppscn/SmsForwarder)

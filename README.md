# 法院短信转发器

自动识别并转发法院相关短信（如 12368 送达）到邮箱的 Android 应用。

包名：`cn.senrylee.courtsms`  
仓库：https://github.com/SenryLee/CourtSmsForwarder

---

## 做什么

法院短信 → 本机监听 → 按预置规则匹配 → 发到你的邮箱。

界面只保留：

- **转发日志**：查看转发记录
- **转发规则**：查看/微调预置规则
- **设置**（左上角齿轮）：填写邮箱与授权码、开关、关于

不提供多通道、来电转发、应用通知转发等通用能力。

---

## 快速开始

1. 安装 APK（见 [Releases](https://github.com/SenryLee/CourtSmsForwarder/releases)）
2. 授予短信与通知相关权限，并尽量关闭电池优化
3. 打开设置，填写邮箱与 SMTP 授权码（推荐 QQ / Foxmail）
4. 保存后会自动写入法院规则：
   - `法院号码·12368`
   - `法院关键词·案号送达`
5. 收到法院短信后，在「转发日志」确认是否成功

授权码不是登录密码：在邮箱网页「设置 → 账户 → POP3/SMTP」中开启并生成。

---

## 邮件内容

转发邮件带有结构化字段，便于后续脚本或助手解析，例如：

- 案号、链接、日期
- `NEED_DOWNLOAD` / `NEED_CALENDAR` 标记
- 原文摘要

可选：配合 [workbuddy-skill/court-sms-digest](./workbuddy-skill/court-sms-digest) 做每日汇总（下载提醒、日程、落盘）。

---

## 构建

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
./gradlew :app:assembleDebug
```

产物文件名形如：`CourtSms_1.0.0.yymmdd_…_debug.apk`

---

## 说明

本项目是独立产品「法院短信转发器」，界面与能力已按法院短信场景重做。实现上参考过开源短信转发方案的技术路径，但产品定位、交互与默认规则均为本项目自有设计。

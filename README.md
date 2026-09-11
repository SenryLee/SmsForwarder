# 法院短信转发器

独立 Android 应用：自动识别法院相关短信（如 12368 送达），并转发到你的邮箱。

包名：`cn.senrylee.courtsms`  
仓库：https://github.com/SenryLee/CourtSmsForwarder

## 功能

- 主界面仅保留：**转发日志**、**转发规则**
- 设置页：填写邮箱 + 授权码、短信开关、重新写入法院规则、关于
- 仅邮箱发送；按邮箱域名自动匹配 SMTP
- 首启自动预置规则：
  - `法院号码·12368`
  - `法院关键词·案号送达`

## 使用

1. 安装 APK，授予短信 / 通知权限
2. 打开设置，填写邮箱与 SMTP 授权码（推荐 QQ / Foxmail）
3. 保存后规则会绑定到该邮箱
4. 在「转发日志」核对结果

## 构建

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew :app:assembleDebug
```

## 说明

本项目是独立产品「法院短信转发器」。实现上参考过开源短信转发方案的技术路径，但产品定位、交互与默认规则均为本项目自有设计。

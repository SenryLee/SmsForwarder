# WorkBuddy Skill 导入说明

## 目录

```text
workbuddy-skill/court-sms-digest/
  SKILL.md
  automation-prompt.md
  schemas/digest.md
  examples/sample-email.md
  README.md
```

## 导入步骤

1. 打开 WorkBuddy → 连接器 → 连接 **QQ 邮箱**（或你实际收信的邮箱连接器）。
2. 将本目录复制到 WorkBuddy skills 目录（常见为 `~/.workbuddy/skills/court-sms-digest/`），或在对话中粘贴 `SKILL.md` 让其创建同名 Skill。
3. 手动执行一次：把 `examples/sample-email.md` 当作输入，确认清单含「待下载」「待建日程」。
4. 打开 `automation-prompt.md`，整段发给 WorkBuddy，创建每天 18:00 自动化。
5. 确认落盘路径 `~/法院送达/` 存在且可写。

## 与 Android 端的约定

- 邮件主题前缀：`[法院送达]`
- 布尔标记：`NEED_DOWNLOAD=` / `NEED_CALENDAR=`
- 详见仓库根目录 README。

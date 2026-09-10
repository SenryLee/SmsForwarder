# 汇总清单字段定义

每条短信/邮件对应一行事项：

| 字段 | 说明 |
|---|---|
| case_no | 案号；无则填「无」 |
| from | 来源号码 |
| sim | 卡槽备注 |
| received_at | 接收时间 |
| topic | 一句话事由（送达/开庭/执行/传票…） |
| action_needed | 要不要动：无需 / 需下载 / 需出庭 / 需答辩 / 需关注 |
| need_download | yes/no |
| links | URL 列表 |
| need_calendar | yes/no |
| schedule | 开庭或截止时间原文 + 建议提醒时间 |
| raw_excerpt | 原文摘要（≤200 字） |

## Markdown 推荐结构

```markdown
# 法院送达日汇总 YYYY-MM-DD

## 一眼清单
- [案号] 事由 · 要不要动 · 截止

## 待下载（必看）
- [案号] 链接 …

## 待建日程
- [案号] 时间 … · 建议提醒 …

## 明细
…
```

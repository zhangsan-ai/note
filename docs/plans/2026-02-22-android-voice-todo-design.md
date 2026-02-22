# Android 语音待办提醒应用设计文档

日期：2026-02-22

## 1. 目标与约束
- 目标：实现一个极简安卓待办提醒应用，支持语音命令录入、手动输入、快捷时长选择、通知栏关闭/延后，以及未关闭时每 5 分钟持续提醒。
- 约束：仅本地离线存储，不依赖后端；优先稳定可用；UI 极简。

## 2. 技术方案（已确认）
- 开发语言与框架：Kotlin + Jetpack Compose
- 本地数据：Room
- 提醒调度：AlarmManager + BroadcastReceiver
- 通知动作：Notification Action（关闭、延后 5 分钟）
- 开机恢复：BOOT_COMPLETED Receiver
- 语音能力：MediaRecorder 保存原音 + SpeechRecognizer 转写

推荐此方案原因：提醒触发更准，通知动作直连，重启恢复稳定，满足“未关闭就每 5 分钟继续提醒”的场景。

## 3. 模块划分
- app（单模块 Android 应用）
- ui/：Compose 页面与状态展示
- voice/：录音、语音识别、原音回放
- parser/：中文时间语句解析（相对 + 绝对）
- data/：Room 实体、DAO、Repository
- alarm/：AlarmManager 调度、Receiver 处理
- notification/：通知构建与动作分发
- boot/：开机恢复提醒
- worker/：可选一致性兜底

## 4. 数据模型
- todo_item
  - id, content_text, status(ACTIVE/DONE), created_at, updated_at
- reminder
  - id, todo_id, trigger_at_epoch_ms, repeat_every_minutes(固定5), is_enabled, last_fired_at
- voice_note
  - id, todo_id, audio_path, duration_ms, stt_text, parse_status
- pending_action_log（可选）
  - action_type, reminder_id, created_at

## 5. 关键业务流程
1) 语音创建
- 用户按住说话 -> 录音保存原音
- 语音转文字（STT）
- 解析文本中的时间表达
- 解析成功：创建 Todo + Reminder + VoiceNote 并调度闹钟
- 解析失败：创建普通待办（无提醒）并引导手动选快捷时间

2) 手动创建
- 输入文本 + 点击快捷时长（5m/10m/15m/30m/1h/2h/3h/6h/12h/24h）
- 写入本地并调度提醒

3) 提醒触发
- Receiver 收到闹钟 -> 发通知（关闭/延后5分钟）
- 关闭：is_enabled=false，取消后续
- 延后：trigger_at=now+5min 并重设闹钟
- 未操作：自动安排 5 分钟后再次提醒，直到用户关闭

4) 重启恢复
- 开机后扫描 is_enabled=true 的提醒并重新调度

## 6. 时间解析规则
支持表达：
- 相对时间：5分钟后、10分钟后、1小时后、2小时后
- 绝对时间：今晚8点、今天20:00、明天早上9点、后天14点

策略：
- 先匹配相对时间，再匹配绝对时间
- 失败时降级为“仅待办不定时提醒”

## 7. 异常处理
- 麦克风权限拒绝：保留手动流程
- STT 失败：保留原音并可回放，支持手动修正
- 时间解析失败：不阻塞创建，提示手动选快捷时间
- 精确闹铃权限受限：提示开启权限，未开启则降级

## 8. UI（极简）
- 顶部：手动输入框
- 中部：快捷时长按钮组
- 语音区：按住说话、识别文本预览、回放原音、一键创建
- 列表区：待办标题、下次提醒时间、状态

## 9. 验收标准
- 可语音创建待办并保存原始录音且可回放
- 支持相对/绝对时间语音命令解析
- 通知栏包含关闭和延后 5 分钟
- 未关闭时每 5 分钟持续提醒（无限）
- 手动输入与快捷创建可用
- 设备重启后可恢复未完成提醒

## 10. 说明
当前工作目录不是 Git 仓库，无法在本步骤执行“提交设计文档”动作。后续可在初始化 Git 后补提交流程。

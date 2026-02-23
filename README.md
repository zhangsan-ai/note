# 待办清单 (Android)

极简待办提醒 App（Android / Kotlin / Compose），支持：

- 语音主流程创建待办（文本输入为辅助）
- 快捷定时（无提醒、5分钟、10分钟、15分钟、30分钟、1小时、2小时、3小时、6小时、12小时、24小时、2天、3天）
- 保存原始语音文件并可直接回放
- 通知栏动作：`关闭`、`延后5分钟`、`播放语音`
- 通知展示真实待办正文（含展开大文本）
- 提醒采用闹钟策略（优先 `setAlarmClock`）并使用系统闹铃声
- 提供“测试铃声”按钮验证设备铃声链路
- 未关闭时自动每 5 分钟继续提醒（无限）
- 常驻通知：有未完成提醒时通知栏保持驻留
- 一键清除已完成（含二次确认）
- 提供保活入口：精确闹钟、电池优化豁免、后台运行设置
- 开机后恢复未关闭提醒

## 目录结构

- `app/src/main/java/com/example/voicetodo/ui`：界面与状态
- `app/src/main/java/com/example/voicetodo/parser`：中文时间解析
- `app/src/main/java/com/example/voicetodo/data`：Room 本地数据
- `app/src/main/java/com/example/voicetodo/alarm`：闹钟调度与通知动作
- `app/src/main/java/com/example/voicetodo/boot`：开机恢复
- `app/src/main/java/com/example/voicetodo/voice`：录音与原音存储

## 权限

- `RECORD_AUDIO`
- `POST_NOTIFICATIONS`
- `SCHEDULE_EXACT_ALARM`
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- `RECEIVE_BOOT_COMPLETED`
- `USE_FULL_SCREEN_INTENT`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`

## 语音录音说明

- 已取消语音识别，录音仅作为原音附件保存
- 标题优先使用输入框文本；若为空则自动生成“语音待办 + 时间”
- 提醒时间仍使用快捷时长按钮设置
- 待办列表中可直接播放每条语音待办的原音

## 说明

- 你要求“先实现功能不本地编译”，本仓库代码已完整落地。
- 已补齐 `gradle-wrapper.jar` 与 `gradlew` 脚本。
- 当前执行环境未安装 Java，所以这里无法直接跑 `./gradlew`；在本地装好 JDK 17+ 即可构建。
- 语音仅走 `MediaRecorder` 录制 `m4a`，避免机型差异导致的识别不可用问题。

## 上传到 GitHub

```bash
git init
git add .
git commit -m "feat: android voice todo reminder app"
# 替换为你的仓库地址
git remote add origin https://github.com/<your-account>/<repo>.git
git branch -M main
git push -u origin main
```

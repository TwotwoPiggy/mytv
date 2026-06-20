# Phase 1: Verification Bypass & Playback Architecture - Context

**Gathered:** 2026-06-21
**Status:** Ready for planning

<domain>
## Phase Boundary

移除一切测试码和绑定验证框，让应用开机即播；解耦 `PlayerFragment` 的 ExoPlayer 和 Webview 播放逻辑，保障切换稳定（健壮性）。

**In scope:**
- 完全删除 UserVerificationHandler、验证对话框、测试码相关代码
- 删除 SettingFragment 中的验证入口
- 保留源文件下载能力（importFromUrl），移除验证/绑定/过期限制
- 删除验证相关广播和 24 小时定时检查
- ExoPlayer 逻辑独立封装为 ExoPlayerEngine 类
- 通过回调接口与 PlayerFragment 通信
- 引擎切换时立即完全销毁前一个引擎
- 引擎内部自清理防止内存泄漏
- 启动时显示简短 loading 过渡，然后自动播放上次频道或默认源

**Out of scope:**
- 遥控器按键防抖与 UI 美化（Phase 2）
- 繁体→简体中文转换（Phase 3）
- Jetpack Compose 重构（v2 Deferred）
- 自动化 UI 测试（Out of Scope per PROJECT.md）

</domain>

<decisions>
## Implementation Decisions

### 验证移除策略
- **D-01:** 完全删除验证代码 — UserVerificationHandler.kt、验证对话框布局 (userconfirm.xml)、UserInfo 中的验证/绑定/过期逻辑全部删除
- **D-02:** 删除 SettingFragment 中的验证入口 — 移除 `binding.verifyUser` 相关代码和 `showVerificationDialog()` 方法
- **D-03:** 保留源文件下载能力 — 保留 `viewModel.importFromUrl()` 等源导入功能，移除验证/绑定/过期检查
- **D-04:** 删除验证相关广播和定时检查 — 移除 `test_code_expired` 广播、`checkExpiredTestCodes()` 24小时检查、SettingFragment 中的验证监听
- **D-05:** 清理 SharedPreferences — 移除 `KEY_TEST_CODES`、`KEY_ACTIVE_USER_ID`、`KEY_LAST_CHECK_TIME` 等验证相关键值

### 播放引擎拆分方式
- **D-06:** ExoPlayer 独立封装为 `ExoPlayerEngine` 类 — 封装 ExoPlayer 的创建/播放/释放/切换逻辑
- **D-07:** ExoPlayerEngine 封装全部能力 — 生命周期管理、播放监控与自动切源、源切换与编解码、错误处理与恢复
- **D-08:** 通过回调接口通信 — ExoPlayerEngine 通过 `ExoPlayerCallback` 接口通知 PlayerFragment 播放状态变化

### 引擎切换与内存管理
- **D-09:** 立即完全销毁 — 切换引擎时立即释放前一个引擎全部资源（ExoPlayer.release、WebView.destroy），确保零残留
- **D-10:** 引擎内部自清理 — ExoPlayerEngine 在 release() 中解绑 PlayerView、释放 MediaSource、清理 Handler；WebFragment 在 destroy 中清理 WebView 和回调
- **D-11:** 保存频道状态 — 切换引擎时 MainViewModel 通过 LiveData 保持频道状态，切换回来时自动恢复

### 开机即播的启动流程
- **D-12:** 有 loading 过渡 — 启动时显示简短 branded loading 画面（1-2秒），源加载完成后自动播放
- **D-13:** 上次频道或默认源 — 播放上次关闭前正在看的频道（如果有），否则播放默认源的第一个频道
- **D-14:** 首次安装无源提示 — 显示友好提示「请通过局域网管理后台添加源文件」，并显示 IP 地址和端口

### Claude's Discretion
- ExoPlayerEngine 的具体类结构和方法签名由 Claude 设计
- 回调接口的具体方法由 Claude 根据实际需要定义
- Loading 过渡的具体时长和样式由 Claude 决定
- 无源提示的具体 UI 布局由 Claude 设计

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 项目文档
- `.planning/PROJECT.md` — 项目定义、核心价值、约束条件、关键决策
- `.planning/REQUIREMENTS.md` — v1 需求列表，Phase 1 对应 LIMITS-01, LIMITS-02, PERF-02
- `.planning/ROADMAP.md` — 路线图，Phase 1 目标和成功标准

### 架构与代码
- `.planning/codebase/ARCHITECTURE.md` — 应用架构（Single-Activity MVVM、数据流、关键抽象）
- `.planning/codebase/STACK.md` — 技术栈（ExoPlayer 1.5.1、TBS X5 WebView 44286、Coroutines）
- `.planning/codebase/CONVENTIONS.md` — 编码规范（命名、异步模式、错误处理）

### 源代码（关键文件）
- `app/src/main/java/com/horsenma/yourtv/UserVerificationHandler.kt` — 待删除：验证处理器
- `app/src/main/java/com/horsenma/yourtv/UserInfo.kt` — 待清理：UserInfoManager 中的验证逻辑
- `app/src/main/java/com/horsenma/yourtv/PlayerFragment.kt` — 待重构：播放引擎拆分
- `app/src/main/java/com/horsenma/yourtv/MainActivity.kt` — 待清理：验证初始化和按键路由
- `app/src/main/java/com/horsenma/yourtv/SettingFragment.kt` — 待清理：验证入口
- `app/src/main/java/com/horsenma/mytv1/WebFragment.kt` — 参考：WebView 播放实现
- `app/src/main/res/layout/userconfirm.xml` — 待删除：验证对话框布局

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **WebFragment** (`com.horsenma.mytv1.WebFragment`): WebView 播放已独立封装，可直接复用
- **MainViewModel**: LiveData/StateFlow 状态管理已就绪，频道状态可直接通过 ViewModel 传递
- **SimpleServer**: LAN 管理后台已实现，首次安装无源时可提示用户访问
- **SourceDecoder/SourceEncoder**: 源文件编解码工具可保留复用

### Established Patterns
- **Single-Activity MVVM**: 所有 Fragment 通过 MainViewModel 通信，新 Engine 类应遵循此模式
- **Coroutine-based async**: 所有异步操作使用 Kotlin Coroutines，ExoPlayerEngine 应遵循
- **Global exception handler**: YourTVExceptionHandler 全局捕获崩溃，新代码无需额外处理

### Integration Points
- **PlayerFragment.play(tvModel)**: 切换引擎的入口点，ExoPlayerEngine 将在此被调用
- **MainActivity.onCreate()**: 验证初始化将被移除，替换为直接启动播放
- **SettingFragment**: 验证入口将被移除
- **MenuFragment**: test_code_expired 广播监听将被移除

</code_context>

<specifics>
## Specific Ideas

- ExoPlayerEngine 回调接口设计参考 WebFragment 的 `WebFragmentCallback` 模式
- Loading 过渡可复用现有 `LoadingFragment` 的布局样式
- 首次安装提示需要显示设备的实际 IP 地址（从 SimpleServer 获取）

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 1-Verification Bypass & Playback Architecture*
*Context gathered: 2026-06-21*

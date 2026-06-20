# Requirements

## v1 Requirements

### 1. Remove Limits (移除使用限制)
- [ ] **LIMITS-01**: 彻底移除 TV 端的测试码与激活验证对话框，确保用户开机即用，不受验证弹窗阻碍。
- [ ] **LIMITS-02**: 剥离后台对云端 D1 数据库的用户状态及设备绑定网络请求，从根本上杜绝超时卡顿。

### 2. UI Aesthetics & Feedback (界面美化与动效)
- [ ] **UI-01**: 为 Menu 和 Setting 菜单引入拟物微磨砂（毛玻璃）与现代色彩渐变效果，提升大屏视觉高级感。
- [ ] **UI-02**: 为加载画面、错误弹窗及流切换过程添加流畅的动画过渡与交互状态反馈。

### 3. Remote Controller & Gesture (遥控按键与适配)
- [ ] **REMOTE-01**: 实现基于时间闸的物理按键过滤与防抖，解决遥控器频繁误触、死锁及选台漂移问题。
- [ ] **REMOTE-02**: 优化菜单、设置和播放视窗在电视、投影仪、手机等多种分辨率屏幕下的焦点对齐与自适应排版。

### 4. Performance & Architecture (性能、架构与健壮性)
- [ ] **PERF-01**: 解耦 Monolithic `MainActivity.kt`，将按键捕获与事件分发拆分至独立的控制组件，增强核心链路的稳定性。
- [ ] **PERF-02**: 对 `PlayerFragment.kt` 进行播放引擎重构，将 ExoPlayer 逻辑和 Webview 逻辑隔离，提高播放器扩展性。
- [ ] **PERF-03**: 优化 EPG (Electronic Program Guide) 节目表解析性能，由大内存 DOM 模式逐步向 Streaming 读取演进，防止低配设备 OOM 崩溃。

## v2 Requirements (Deferred)

- [ ] **CLOUDSYNC-01**: 允许用户通过扫描本地二维码远程上传自定义源到自定义云存储（安全且无阻碍）。
- [ ] **COMP-01**: 完全使用 Jetpack Compose 实现全新的大屏 TV UI 样式。

## Out of Scope

- **Cloud licensing check**: 不再重新加入任何云端授权校验，Core Value 强调纯净无限制。
- **High-overhead automated UI tests**: 由于不同品牌电视遥控驱动差异极大，放弃高成本的 Espresso 自动化 UI 测试，采用精准的物理按键模拟和真机手动验证。

## Traceability

<!-- Filled during roadmap creation -->

---
*Last updated: 2026-06-20*

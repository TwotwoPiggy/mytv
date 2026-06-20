# YourTV (你的電視)

## What This Is

一个专为安卓电视、电视盒子和手机设计的 IPTV/网页视频流媒体播放应用程序。它集成了 ExoPlayer 直播流解码与腾讯 TBS X5 WebView 网页视频源渲染能力，并提供局域网后台（NanoHTTPD）供用户轻松进行视频源和配置管理。

## Core Value

为用户提供一个纯净、无任何激活与授权使用限制、在电视遥控器和手势下拥有流畅交互的高性能直播播放体验。

## Requirements

### Validated

<!-- 现有 codebase 中已实现的成熟功能 -->
- ✓ IPTV 播放引擎 — 整合 Jetpack Media3 (ExoPlayer) 支持 HLS/RTSP/RTMP 直播源
- ✓ 网页视频源渲染 — 支持 `webview://` 协议，采用腾讯 TBS X5 浏览器内核渲染网页视频
- ✓ 局域网管理后台 — 通过内置 NanoHTTPD 提供本地 8080 端口，可由浏览器管理设置及源文件
- ✓ 开机自启动 — 提供 `BootReceiver` 可选配置开机启动
- ✓ 容错重启系统 — 全局异常捕获 `YourTVExceptionHandler` 捕获崩溃并重试启动

### Active

<!-- 当前规划中待实现的要求 -->
- [ ] 彻底移除使用限制 — 彻底剥离/跳过 `UserVerificationHandler` 的测试码与设备绑定验证逻辑，让用户免验证直接顺畅播放
- [ ] 优化遥控器与手势操作响应 — 改进 `MainActivity` 和 `PlayerFragment` 的遥控器按键连击判断与手势滑动响应，解决卡顿与漂移问题
- [ ] 增强菜单与设置的界面布局适配 — 重新调整并适配 `MenuFragment`、`SettingFragment` 在不同屏幕尺寸下的布局排版
- [ ] 升级视觉美感与过渡反馈 — 改进主题色彩、圆角、背景渐变以及加载/错误画面的交互反馈动画

### Out of Scope

- [ ] 完全重构为 Jetpack Compose — 现存的基于 Fragment 的体系能够较好地与 ExoPlayer 和 Tencent TBS Webview 配合，暂时无重构为 Compose 的必要
- [ ] 全套自动化 UI 测试 — 电视端应用在不同硬件/系统的适配上高度依赖物理遥控器的按键表现，高覆盖率的 UI 自动化测试性价比较低，故以手动真机验证为主

## Context

该项目是对 my-tv / my-tv-1 等产品的演进，支持更加灵活的网页源，但也加入了复杂的激活与测试码验证流程，导致用户体验受限。同时，老旧 Android TV 设备上的内存占用和遥控按键响应也是导致界面卡顿的核心痛点。

## Constraints

- **Compatibility**: Android 6.0+ (API 23+) — 需确保新增的 UI 特效和代码在该最低系统版本下正常工作。
- **Interaction Constraint**: 遥控器 DPAD 键为核心导航方式 — 所有前端优化和布局调整均需保证焦点态可被遥控器正确识别。
- **Build Requirement**: 必须保留 TBS 和本地 AES 资源解密所需的依赖项及 Proguard 混淆规则。

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 彻底移除验证弹窗与校验 | 用户要求去除所有限制，跳过激活码直接免验证播放 | — Pending |
| 全面升级遥控交互与UI美化 | 解决老电视盒子上响应迟钝、界面模糊的缺陷 | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-06-20 after project initialization*

# Project Research Summary

**Project:** YourTV (你的電視)
**Domain:** Android TV Media Player
**Researched:** 2026-06-20
**Confidence:** HIGH

## Executive Summary

本项目研究旨在解决现存的 YourTV 播放器在 Android 智能电视端的使用限制和操作交互痛点。通过对代码库以及 TV 播放架构的研究，我们制定了移除激活码验证、优化按键防抖与界面美化的改造方案。

主要推荐方案为彻底剥离 `UserVerificationHandler.kt` 对话框，并在 `MainActivity.kt` 遥控按键分发逻辑中加入时间闸防抖过滤器，同时为菜单和设置界面注入更加符合现代电视美感的视觉特效。

## Key Findings

### Recommended Stack
- **核心：** Jetpack Media3 ExoPlayer 用于快速直播流解码，Tencent TBS Webview 提供网页流解析。
- **辅助：** `androidx.security` 用于保密解密，物理弹性动画用于提升 TV 端焦点反馈。

### Expected Features
- **Table Stakes (必有)：** 彻底去除验证，直接播放；优化遥控按键防抖。
- **Differentiators (优势)：** TV 界面玻璃拟态（RenderEffect）优化，多屏幕分辨率自适应排版。

### Architecture Approach
在数据流动上，通过 `MainActivity` 捕获事件后，先经过防抖及手势处理器，再推送到 `MainViewModel`，从而解耦单 Activity 中的状态杂糅。

### Critical Pitfalls
- **按键重复派发：** 导致按一次方向键漂移数格，需在分发侧设定 minimum click interval。
- **WebView 内存泄露：** 切换播放源时不释放导致闪退，必须彻底销毁播放组件。

## Implications for Roadmap

### Phase 1: Verification Removal & Playback Safety
- **Rationale:** 移除校验是用户的第一优先级，且必须保证去校验后切换频道时不会发生播放器崩溃。
- **Delivers:** 移除 `UserVerificationHandler` 的显示与网络接口校验。
- **Avoids:** Webview 与 ExoPlayer 切换时的 Context 泄露陷阱。

### Phase 2: Key & Gesture Optimization & UI Aesthetics
- **Rationale:** 优化底层按键捕获与视觉体验，在用户使用上能带来最直观的体验提升。
- **Delivers:** 重构按键防抖，引入弹性焦点过渡动画，美化 Fragment 布局。

---
*Research completed: 2026-06-20*
*Ready for roadmap: yes*

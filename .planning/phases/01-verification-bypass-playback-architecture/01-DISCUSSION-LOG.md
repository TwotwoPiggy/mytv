# Phase 1: Verification Bypass & Playback Architecture - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-21
**Phase:** 1-Verification Bypass & Playback Architecture
**Areas discussed:** 验证移除策略, 播放引擎拆分方式, 引擎切换与内存管理, 开机即播的启动流程

---

## 验证移除策略

### 代码删除方式

| Option | Description | Selected |
|--------|-------------|----------|
| 完全删除代码 | 删除 UserVerificationHandler.kt、UserInfo.kt、验证对话框布局等全部相关代码 | ✓ |
| 注释掉但保留文件 | 注释掉关键调用点，保留文件作为参考 | |
| 你决定 | Claude 决定最合理的方案 | |

**User's choice:** 完全删除代码
**Notes:** 用户明确要求纯净无限制体验

### SettingFragment 验证入口

| Option | Description | Selected |
|--------|-------------|----------|
| 删除验证入口 | 完全移除验证相关的菜单项 | ✓ |
| 保留但改为无操作 | 保留菜单项但点击后显示「已激活」或直接跳过 | |
| 你决定 | Claude 决定 | |

**User's choice:** 删除验证入口

### UserInfo 云端数据库

| Option | Description | Selected |
|--------|-------------|----------|
| 全部删除 | 删除所有 UserInfo 相关逻辑，包括云端数据库请求 | |
| 保留数据结构 | 保留 UserInfo 数据结构但移除验证逻辑 | |
| 保留源下载，移除限制 | 保留从云端下载源文件的能力，移除验证/绑定/过期检查 | ✓ |

**User's choice:** 保留源下载，移除限制
**Notes:** 用户先询问了云端数据库内容，了解后选择了保留源下载能力

### 验证广播和定时检查

| Option | Description | Selected |
|--------|-------------|----------|
| 删除验证广播和检查 | 删除 test_code_expired 广播、24小时检查等 | ✓ |
| 保留广播框架 | 保留广播机制但移除验证内容 | |
| 你决定 | Claude 决定 | |

**User's choice:** 删除验证广播和检查
**Notes:** 用户先询问了具体作用和删除影响，确认无负面影响后选择删除

---

## 播放引擎拆分方式

### 抽象程度

| Option | Description | Selected |
|--------|-------------|----------|
| 接口抽象层 | 创建 PlayerEngine 接口，ExoPlayer 和 WebView 分别实现 | |
| ExoPlayer 独立封装 | ExoPlayer 逻辑提取为独立类，WebView 保留在 WebFragment | ✓ |
| 原地重构 | 只清理 PlayerFragment 中的混乱逻辑 | |
| 你决定 | Claude 决定 | |

**User's choice:** ExoPlayer 独立封装（推荐）
**Notes:** Claude 推荐此方案，理由是 WebView 已在独立包中，统一接口投入产出比低

### 具体形式

| Option | Description | Selected |
|--------|-------------|----------|
| ExoPlayerEngine 类 | 封装 ExoPlayer 实例管理、播放控制、错误处理 | ✓ |
| 统一 PlayerEngine 接口 | 完全抽象接口，两种引擎都实现 | |
| 你决定 | Claude 决定 | |

**User's choice:** ExoPlayerEngine 类（推荐）

### 封装能力

| Option | Description | Selected |
|--------|-------------|----------|
| 生命周期管理 | create(), play(url), release(), PlayerView 绑定 | ✓ |
| 播放监控与自动切源 | checkPlaybackRunnable、缓冲检测 | ✓ |
| 源切换与编解码 | switchSource()、HLS 特殊处理、软硬解切换 | ✓ |
| 错误处理与恢复 | ErrorFragment 显示、错误恢复逻辑 | ✓ |

**User's choice:** 全部 4 项

### 通信方式

| Option | Description | Selected |
|--------|-------------|----------|
| 回调接口 | ExoPlayerEngine 通过回调接口通知 PlayerFragment | ✓ |
| LiveData/StateFlow | ExoPlayerEngine 通过 LiveData 暴露状态 | |
| 你决定 | Claude 决定 | |

**User's choice:** 回调接口（推荐）

---

## 引擎切换与内存管理

### 切换时前一个引擎处理

| Option | Description | Selected |
|--------|-------------|----------|
| 立即完全销毁 | 切换时立即释放前一个引擎全部资源 | ✓ |
| 延迟销毁/缓存 | 保留前一个引擎实例一段时间 | |
| 你决定 | Claude 决定 | |

**User's choice:** 立即完全销毁（推荐）
**Notes:** 用户先询问了引擎切换的作用，了解后选择立即销毁

### 内存泄漏防护

| Option | Description | Selected |
|--------|-------------|----------|
| 引擎内部自清理 | ExoPlayerEngine 在 release() 中自清理 | ✓ |
| 外部统一清理 | PlayerFragment 在 onDestroy 中统一清理 | |
| 你决定 | Claude 决定 | |

**User's choice:** 引擎内部自清理（推荐）

### 播放状态保存

| Option | Description | Selected |
|--------|-------------|----------|
| 保存频道状态 | 切换引擎时保存当前频道信息 | ✓ |
| 不保存状态 | 每次切换都从头播放 | |
| 你决定 | Claude 决定 | |

**User's choice:** 你决定 → Claude 推荐保存频道状态

---

## 开机即播的启动流程

### 启动后第一个画面

| Option | Description | Selected |
|--------|-------------|----------|
| 有 loading 过渡 | 启动时显示简短 loading 动画，然后直接进入播放 | ✓ |
| 直接进播放界面 | 直接显示播放界面，后台加载完成后自动播放 | |
| 你决定 | Claude 决定 | |

**User's choice:** 你决定 → Claude 推荐有 loading 过渡

### 默认播放频道

| Option | Description | Selected |
|--------|-------------|----------|
| 上次频道或默认源 | 播放上次关闭前正在看的频道，否则播放默认源 | ✓ |
| 总是默认源 | 每次启动都播放默认源的第一个频道 | |
| 你决定 | Claude 决定 | |

**User's choice:** 上次频道或默认源（推荐）

### 无源时显示

| Option | Description | Selected |
|--------|-------------|----------|
| 提示添加源 | 显示友好提示「请通过局域网管理后台添加源文件」 | ✓ |
| 空白播放界面 | 显示一个空白播放界面 | |
| 你决定 | Claude 决定 | |

**User's choice:** 你决定 → Claude 推荐提示添加源

---

## Claude's Discretion

- ExoPlayerEngine 的具体类结构和方法签名由 Claude 设计
- 回调接口的具体方法由 Claude 根据实际需要定义
- Loading 过渡的具体时长和样式由 Claude 决定
- 无源提示的具体 UI 布局由 Claude 设计

## Deferred Ideas

None — discussion stayed within phase scope.

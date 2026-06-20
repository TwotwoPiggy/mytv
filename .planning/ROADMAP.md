# Roadmap

## Phase 1: Verification Bypass & Playback Architecture (使用限制清除与播放重构)
**Goal:** 移除一切测试码和绑定验证框，让应用开机即播；解耦 `PlayerFragment` 的 ExoPlayer 和 Webview 播放逻辑，保障切换稳定（健壮性）。
**Mode:** mvp
**Success Criteria:**
1. 应用在无激活码、离线状态下冷启动不会弹出任何激活/验证提示框，默认直达播放区。
2. ExoPlayer 视频源与 TBS X5 Webview 网页源切换时能完美销毁前一个实例，内存占用不发生持续膨胀。
3. 完成播放引擎重构，将 ExoPlayer 独立封装。

**Requirements:**
- `LIMITS-01`
- `LIMITS-02`
- `PERF-02`

Plans:
- [ ] TBD (run /gsd-plan-phase 1 to break down)

---

## Phase 2: Remote Interaction & UI Aesthetics (按键防抖与界面美化适配)
**Goal:** 解耦 MainActivity 遥控按键逻辑，解决按键误触漂移，实现菜单与设置界面在多分辨率大屏下的微磨砂毛玻璃新 UI 及平滑交互动效，并重构 EPG 解析以极大降低内存开销。
**Mode:** mvp
**Success Criteria:**
1. 在各种电视模拟器/真机上快速连击遥控方向键和确认键时，不会跳格，更不会导致界面死锁，按键响应维持在 100ms 内。
2. MenuFragment 与 SettingFragment 呈现出毛玻璃与配色渐变的现代 TV UI 质感，且在多屏幕比例下完美对齐。
3. 加载、错误和状态切换带有过渡反馈。
4. 重构按键分发，MainActivity 复杂度下降，代码可读性与可维护性提升。
5. 性能优化：EPG 表单解析从 DOM 转向 Streaming，读超大节目单不再引起帧率抖动或 OOM。

**Requirements:**
- `UI-01`
- `UI-02`
- `REMOTE-01`
- `REMOTE-02`
- `PERF-01`
- `PERF-03`

Plans:
- [ ] TBD (run /gsd-plan-phase 2 to break down)

---

## Phase 3: 前端所有文字改成简体中文, 项目中所有繁体也都改成简体中文
**Goal:** [To be planned]
**Requirements**: TBD
**Depends on:** Phase 2
**Plans:** 0 plans

Plans:
- [ ] TBD (run /gsd-plan-phase 3 to break down)

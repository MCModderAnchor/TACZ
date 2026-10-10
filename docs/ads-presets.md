# 共享开镜预设

枪包可以把开镜路径和 FOV 过渡存为共享预设。预设位于
`assets/<namespace>/ads_presets/<name>.json`。

例如 `assets/tacz/ads_presets/renew.json` 的资源 ID 是 `tacz:renew`。
枪械展示配置只需添加：

```json
"ads_preset": "tacz:renew"
```

预设包含 `ads_path` 和 `ads_fov`，其字段与此前的单枪配置一致。
修改预设并重载枪包，会更新所有引用它的枪械。
引用不会改变模型观察节点、开镜时间、最终 FOV 或瞄具放大倍率。

## 单枪覆盖

枪械可以同时引用预设并填写自己的 `ads_path` 或 `ads_fov`。
单枪字段优先，按整组覆盖，不逐轴或逐参数合并。例如：

```json
"ads_preset": "tacz:renew",
"ads_fov": {
  "model": { "start": 0.2, "end": 1, "ease": "ease_in_out_sine", "smooth": true },
  "world": { "start": 0, "end": 1, "ease": "ease_in_sine", "smooth": true }
}
```

这个例子使用共享路径和独立 FOV 配置。
单枪 `ads_fov` 只填写 `model` 时，`world` 使用原版行为，不继承预设的 world。
未填写 `ads_preset` 时自动引用 `tacz:legacy`，显式自定义预设仍有效。
引用丢失时会记录警告，并回退到 `tacz:legacy`；单枪配置仍优先。
`legacy.json` 保存旧版参数：定位各轴及旋转为 0～1 的 linear，
`clamp: false` 保留原定位平滑器的越界进度；模型和世界 FOV 为
0～1 的 ease_in_expo，`smooth: true` 保留各自原有平滑器。
未填写或无效的 FOV 通道，以及 legacy 文件缺失时，仍保留代码中的旧版回退。

## 调参

`start` 和 `end` 是 0～1 的开镜进度，需满足 `start < end`。
`ease` 支持 `linear`、`smoothstep`，以及以下八个系列各自的
`ease_in_*`、`ease_out_*`、`ease_in_out_*`：
`sine`、`quad`、`cubic`、`quart`、`quint`、`expo`、`circ`、`back`，共 26 种。
也支持图示 camelCase 名称，例如 `easeInOutCubic` 等同于 `ease_in_out_cubic`。
曲线 `clamp` 默认是 `true`，将窗口进度限制在 0～1；旧版线性定位使用 `false`。
`clamp` 限制的是输入，Back 曲线仍会回拉或超出终点；用于 FOV 时也会暂时越过目标 FOV。
Circ 曲线为避免平方根产生 NaN，始终限制输入到 0～1；Expo 保留起终点限制。
FOV 通道的 `smooth` 默认是 `true`，保留原来的独立 FOV 平滑器。
定位使用平滑开镜进度；FOV 曲线使用原始开镜进度，再由自己的平滑器追随目标。

测试时编辑客户端 `tacz` 目录里的预设，然后使用 `/tacz reload`（需管理员权限）。
调好后同步回源代码枪包。首次使用新增加载代码需要重启客户端，之后修改 JSON 无需重启。

默认枪包的 67 个常规枪械展示配置引用 `tacz:renew`；M320、RPG7 和 Minigun 自动使用 `tacz:legacy`。

Editing the TACZ default gun pack

To preserve local edits, set DefaultPackDebug=true in the [gunpack] section of
<game directory>/tacz/tacz-pre.toml. When false, changes to the TACZ version or
bundled resources trigger replacement at startup.

To regenerate, set DefaultPackDebug=false, delete the exported folder
<game directory>/tacz/tacz_default_gun, then restart.
Backups are stored in <game directory>/tacz_backup.

Dual-version gun pack / 双版本枪包示例

This pack keeps the 1.20.1 base files and shares models, textures, sounds and
scripts between versions. gunpack.meta.json enables compat_1_21_1 only on
Minecraft 1.21.1. The old 1.20.1 loader reads the base files as before.

目录结构：
  gunpack.meta.json                         根元数据，声明版本与覆盖目录
  assets/tacz/...                           两个版本共用的客户端资源
  data/tacz/recipes/...                     1.20.1 基础配方（Forge 标签、NBT）
  data/tacz/data/blocks/...                 1.20.1 分类图标
  compat_1_21_1/data/tacz/recipe/...          1.21.1 配方（NeoForge 标签、组件）
  compat_1_21_1/data/tacz/data/blocks/...     同路径覆盖分类图标
  compat_1_21_1/data/tacz/tacz_loot_injectors/...  覆盖旧版战利品函数

供内容包作者参考：
1. 共用资源只保留在基础层。仅有版本差异的文件才放入覆盖目录，
   不要把整个包复制进去。覆盖目录内仍从 assets/ 或 data/ 开始。
2. 同路径文件整体替换，不合并 JSON；未提供的文件回落到基础层。
   例如修改工作台分类时，需要在覆盖层保留完整 tabs 数组。
3. 配方目录从 recipes 改为 recipe，因此覆盖层中的新路径是新增资源，
   旧目录保留给 1.20.1。TACZ 自定义的 data/blocks、tacz_loot_injectors
   等目录不改名。同一个资源 ID 在两个版本中保持一致。
4. 可对照 gun_smith_table.json 的图标、gun/taurus943.json 的配方和
   spawn_bonus_chest_taurus943.json 的战利品查看真实差异。
5. 多个匹配层按声明顺序叠加，后声明者优先；不支持删除基础文件或
   嵌套子包。覆盖层不能有自己的 gunpack.meta.json。
6. 文件夹和 ZIP 使用相同结构；ZIP 根目录直接放 gunpack.meta.json。
   发布前分别在两个游戏版本中验证，尤其是脚本和第三方模组依赖。

修改本包用于调试时，请先开启上述 DefaultPackDebug，并修改当前版本实际
读取的文件。1.21.1 配方应修改 compat_1_21_1/data/tacz/recipe/ 下的副本。
仅含 1.21.1 差异的包可继续供旧 1.20.1 加载器使用；旧加载器不支持启用
1.20.1 专用覆盖层。目录结构示例不改变本包原有的资源许可证。

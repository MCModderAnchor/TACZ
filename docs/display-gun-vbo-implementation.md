# 展示枪 VBO 接入实施方案

更新日期：2026-09-24。

范围与顺序：挂板／展示柜的固定枪械 → 第三人称手持与掉落物。工作台暂不接入，物品不复用世界 section 绘制入口。

本项目当前只收集 QUADS。poly mesh（三角面）不参与捕获，也不为其增加专门的回退判定；保留 `skipNormalVisibilityCull=true`。

本文区分已完成的改动与待实现部分。当前已建立 SBM 独立模型／LOD 懒加载、SBM 逐对象帧级接管 API、展示槽位调用，以及第三人称／掉落物即时 VBO 入口。两个 VBO 开关默认开启；光影开启的展示路径已验证可用，新增物品即时路径的坐标、材质、阴影和性能仍需游戏内验收。

## 1. 当前代码中的接入边界

| 位置 | 已核对的行为 | 实施决定 |
|---|---|---|
| `GunPegboardRenderer` / `GunDisplayCaseRenderer` | 槽位变换后以 FIXED 调用 ItemRenderer；另有半透明放置预览 | 接管单个正式槽位中的枪械，预览继续原路径 |
| `GunItemRendererWrapper.renderByItem` | 枪体 LOD、贴图、定位节点反变换、display 缩放 | 旧路径矩阵保留作参照；SBM 表示独立验证坐标规则，模型选择纳入外观快照 |
| `BedrockGunModel.render` | 更新配件状态、渲染瞄具、模板操作、功能回调、AR 分支 | 不能用完整 render 作为纯几何捕获入口 |
| `AttachmentRender` | 内置配件、replace / overlay 变体、slot adapter、mount offset、配件独立 LOD | 外观解析必须覆盖整个配件链，不能只有 attachment ID |
| `FunctionalBedrockPart` | 即使 visible=false 也可能运行功能回调 | 不通过旧树完整 render 来生成静态副本 |
| `TextShowRender` | 仅第一人称执行，替换该节点的几何输出 | 外部副本应跳过对应功能节点，不能把其原始 cubes 当作普通几何 |
| `BeamRenderer` | FIXED 不画激光，第三人称右手会画 | 展示捕获不含激光；第三人称另保留动态效果 |
| `GunDisplayInstance.checkTextureAndModel` | 模型由 GunModelTypeManager 的工厂创建，可为自定义子类 | 首版只接显式支持的模型类型；未知模型完整回退 |
| `ClientIndexManager.reload` | 枪包同步后重建全部客户端索引 | 增加资源代际，不能只依赖 F3+T 的 reload listener |

当前挂板与展示柜是 4×4 槽位。展示柜物品固定满亮；普通挂板采样方块光照，发光挂板满亮。容器的方块模型及玻璃不属于本次枪械网格。

## 2. 数据与职责

TACZ 只维护业务数据：枪包资源到 SBM 模型的转换、外观解析和槽位对象；GPU 缓存、引用计数、上传、剔除、世界卸载交给 SBM。外部视角的几何生成与骨骼状态使用 SBM，不通过 TACZ 的 BedrockGunModel 或 FunctionalBedrockPart 完成。

### 2.1 资源表示

在资源加载／首次使用时，从同一份 TACZ BedrockModelPOJO 转换为 SBM 的 BedrockModelPOJO，再用 `BakedBedrockModel.bake(pojo, BakerOptions)` 创建共享外部模型；完整外观捕获时使用 `BakedModelInstance` 设置骨骼显隐，绘制实例不持有或更新骨骼姿势。生命周期绑定当前 GunDisplayInstance 或资源代际。

`GunDisplayInstance.getExternalGunModel/getExternalLodModel` 已提供基础入口：按当前枪包模型资源懒烘焙；自定义旧模型子类或烘焙失败时返回 null 供接入方回退。首批展示槽位已调用该入口。

- 不从第一人称模型当前姿势拷贝：它的位移、旋转、显隐、功能回调有可变状态。也不在外部路径建立一套 TACZ BedrockPart 树。
- TACZ 与 SBM 的 POJO 是不同 Java 类型；`ExternalGunModel.bake` 在加载时转换标准 Bedrock JSON 语义，特别处理 cube 的数组／逐面 UV、镜像、旋转和枢轴，再调用 SBM 烘焙。转换后的模型仍需游戏内与旧路径逐模型对照。
- 首版保留全部命名骨骼，避免把显隐组、配件挂点、fixed、thirdperson_hand、枪口及激光挂点提前折叠。稳定后根据实际需要缩小 preservedBones，使固定枪身合并。
- 独立的 BakedModelInstance 从不可变 BakedBedrockModel 创建，外观仅修改这一实例的骨骼状态。
- 几何回放使用 SBM 的烘焙 chunk／顶点出口，不调用 TACZ 的旧 render、flush、stencil、粒子、AR 注册或全局 bufferSource。
- 可变骨骼状态属于此次捕获，不写回第一人称共享模型。资源暂缺返回失败，不能把缺资源缓存成成功的空模型。

配件模型也从对应的枪包 POJO 烘焙为 SBM 模型，首批已支持无变体／无 slot adapter 的普通配件，包括 FIXED 视角的瞄具；分划板节点不进入静态几何。带 replace／overlay 变体或 slot adapter 的组合仍让整枪回退。枪体 LOD 模型缺少配件挂点时跳过该配件，与旧渲染路径一致。自定义模型类型若依赖非标准旧子类行为，应由提供者适配 SBM 表示；尚无适配时该枪完整回退。

### 2.2 外观快照

建议一个不可变 GunAppearance，保存实际解析后的资源选择及显隐状态，不保存可被业务方修改的 ItemStack。

必须覆盖：

- 枪械 display 身份、皮肤实际选择、枪体模型／贴图和 LOD；资源 generation。
- 每类有效配件（安装件优先，否则内置件），实际模型／贴图及独立 LOD。
- 配件变体的 replace / overlay 模式及挂载偏移。
- SlotAdapterHelper 解析的转接件及其模型、贴图、偏移。
- 弹膛是否有弹、弹匣是否有弹、扩容等级。
- mount / carry / sight / sight_folded、默认／战术护木、默认枪口与配件 adapter 子节点显隐。
- 外部视角不显示的手臂、额外换弹弹匣及第一人称功能节点。

把显隐规则从 BedrockGunModel 中提取为可复用的纯解析逻辑，将结果写入 SBM BakedModelInstance。旧模型的 FunctionalBedrockPart 只用于核对现有语义，不作为新表示的渲染机制。不能无条件输出所有保留骨骼，其中相当一部分受弹药和配件状态控制。

`Snapshot` 按槽位内容版本及枪包资源版本缓存近距、远距两种捕获参数；每帧只按相机距离选择已有快照。缓存以弱引用槽位为键，不另存长期槽位表。geometryKey 用展示资源 ID、按类型排列的有效配件 ID、枪包资源版本、弹膛与弹匣有无子弹及实际 LOD 选择描述共享网格。GunDisplayInstance、ClientAttachmentIndex、ExternalGunModel、世界位置、光照、朝向、槽号、旋转和无关物品 tag 都不进入 key。

### 2.3 槽位对象

每个悬挂物独立登记。GunPegboardBlockEntity 和 GunDisplayCaseBlockEntity 懒创建并持有对应槽号的稳定 DisplayGunSlot，它只保存所属 BE、槽号和变化版本。TACZ 不再维护额外的 SLOTS、弱引用槽位表或 BE→槽位映射。

- isValid：BE 属于当前世界且未移除，槽位仍放着枪。
- origin：所属容器方块的世界原点。
- packedLight：展示柜满亮；挂板按方块状态选择满亮或环境光。
- geometryKey：该悬挂物的共享外观；槽位变换不入 key。
- localTransform：方块内的槽位位移、朝向、八档旋转和统一缩放。
- collectGeometry：只捕获该悬挂物的 SBM 模型。
- BER：受支持的槽位由 VBO 路径接管；网格未就绪时跳过本次枪体绘制，明确不受支持的槽位才走旧路径。

setItem、removeItem、dropItem、dropAllItems 及 load 中实际改变的物品会更新槽位外观版本；rotateItem 和 load 中单纯旋转的变化只更新通知版本。BER 登记时把已知变化交给 `markDirty`；纯光照变化仍由 SBM 的每 tick 属性读取发现。资源重载也使外观缓存失效。相同外观的悬挂物可跨槽位共享 VBO，实例矩阵在绘制时分别应用。

## 3. 坐标与光照

完整链路为：容器槽位变换 → ItemRenderer 的模型／上下文变换 → 枪械原点翻转及定位组反变换 → display 缩放 → 骨骼／配件变换。

本轮已抽出：

- `DisplayItemTransforms.apply`：两类容器和放置预览共用，输出仍是进入 ItemRenderer 前的空间。
- `GunItemTransforms.apply`：目前仅整理了 TACZ 旧模型的 FIXED / GROUND / 第三人称变换，作为视觉对照。它依赖旧 BedrockGunModel，不能直接给 SBM 副本使用。

两者不能直接拼接就宣称等价：SBM 烘焙已有自己的坐标约定，旧模型的 `scale(-1,-1,1)` 不能直接叠加。静态捕获绕开 ItemRenderer 时，还必须补齐中间的 baked item transform、居中平移及 Forge hook 语义。应先通过旧／新模型顶点与挂点对照求出单一转换，再在展示和第三人称复用。无法复现的自定义物品／模型 hook 应回退。用带明显偏心 fixed 节点和非均匀缩放的枪验收。

普通法线留在共享网格的局部空间，不能含相机旋转。SBM 的 INSTANCE 绘制把世界方向光变换到当前槽位的局部空间；普通顶点捕获 UV2=0，自发光保留固定满亮。枪械 display 的非均匀缩放仍在捕获时处理。

自发光明确采用特殊 RenderType 单独绘制，不采用伪造法线方案。旧 UnshadedVertexConsumer.wrap 从 RenderSystem 当前光源方向生成法线；新外部副本不调用它，也不使用固定 +Y 法线。

### 3.1 自发光材质契约

- 普通部件写入普通 cutout RenderType，自发光部件写入独立 emissive cutout RenderType，均通过 collector.buffer 分流。继承父骨骼的 illuminated 语义必须保留。
- 自发光 pass 使用原版 `rendertype_energy_swirl` shader 的无方向光、alpha 裁剪和雾衰减行为，由 TACZ 的 RenderType 单独设置静态 UV、纹理、深度、剔除及不混合状态。保留真实几何法线，供后续模型变换和明确支持的后端使用。顶点 UV2 保持固定满亮，不参与环境光更新。
- 顶点颜色固定白色，不设计染色或按颜色拆缓存；保留纹理采样、alpha 裁剪、overlay、雾、深度测试、深度写入和对应的剔除规则。发光不等于 bloom。
- 同一部件只输出到一种材质，不把自发光顶点同时复制到普通 pass。不同材质分别合批，同一个普通 RenderType 不因某个骨骼发光而整体改成无光照。
- 新 RenderType 的职责是材质状态和 shader；实例光照更新、GPU 上传仍归 SBM。几何 key 必须区分普通／自发光输出及材质选择。
- TACZ 只创建 RenderType，不注册新的 core shader。SBM 接收实际 RenderType，无需理解 TACZ 的骨骼命名。原版 shader 的雾衰减与普通 entity cutout 的线性雾混色存在差异，实机对照时单独检查。
- 光影开启的普通世界 pass 已确认可用，不因启用光影而回退。VBO 默认优先于 AR；AR 仅在明确不支持该枪或用户关闭 VBO 时通过旧路径接管。网格未就绪时不交给 AR。

展示枪和后续第三人称共用这一材质分类；第三人称后端仍负责真实法线矩阵。旧第一人称的 UnshadedVertexConsumer 本轮不改，避免扩大接入范围。

## 4. 首版后端与原路径交接

展示枪先声明只支持 INSTANCE。取放和 LOD 切换要求按槽位交接，SECTION 的 active 旧快照暂留会让已取走的枪短暂残留。槽位朝向与旋转现在只更新实例矩阵，不触发共享几何重新捕获。

WorldMeshGroup 已新增 `claimForWorldDraw(object, expectedGeometryKey)`，在当前主世界帧检查实际上传完成的 INSTANCE 网格。调用方应先判断该枪是否受支持；受支持后即由 VBO 路径接管，不能把 claim 的“尚未可画”当作调用 BER 的理由。具体契约：

1. BER 登记／更新对象后，请求本帧是否由世界层接管，传入本次期望的 geometryKey。
2. 只有实际可绘制版本匹配且全部 pass 已上传时，claim 返回成功；返回失败表示本帧没有可画的 VBO。
3. 受支持但 claim 失败时，BER 跳过枪体绘制，让捕获／上传队列继续推进；明确不受支持时才走完整旧路径。
4. AFTER_BLOCK_ENTITIES 可以捕获、上传、提升新版本，但不能在 BER 已决定跳过的同一帧擅自画新版本。
5. 版本和决策作用于当前主绘制帧；阴影 pass 不得消费或覆盖主帧决策。未被 BER 访问的对象按约定不绘制，下一主帧重新决策。
6. 取走物品、改变配装或资源失效时，旧几何不能与本帧期望 key 不一致却仍被接管；单纯朝向或旋转变化由实例矩阵更新。

该接管机制已采用显式帧序号与期望 key，claim 失败时不移除缓存引用。当前 `DisplayGunMeshGroup.tryRender` 忽略 claim 返回值并在已识别的展示枪上返回 true，正是未就绪跳过旧绘制的语义；不能将它误改成回退。SECTION 没有逐对象接管能力，因此展示枪渲染组只声明支持 INSTANCE。

非枪物品与明确不受支持的自定义模型或复杂配件组合使用旧路径。受支持的外观在捕获／上传未就绪时跳过本次绘制；可重试错误保持待处理并记录原因，不能因旧路径耗时耗尽准备预算。光影与 AR 本身不阻止 VBO；真正回退时须同步抑制 VBO，避免重复绘制。

## 5. 生命周期和更新频率

- 登记：先由 BER 发现槽位。无需扫全世界；容器目前 shouldRenderOffScreen=true，但仍保留距离约束。
- 内容：load / 网络同步、取放、旋转使 BE 更新对应槽位版本；BER 将通知交给 markDirty。外观变化换几何版本，槽位变换只更新实例矩阵。
- LOD：独立检查相机距离，不依赖首次捕获时的局部 PoseStack。枪体和配件均参与选择；相机移动能触发 key 改变。避免沿用 GUI 的 100ms 时间戳影响世界捕获。
- 光照：无可靠区块光照通知前保留每 tick 采样；needsUpdate=false 不适合直接用于普通挂板。
- 资源：ClientIndexManager.clear/reload 和普通资源重载都推动代际，重建完成后才能发布新快照。GPU 操作在渲染线程；不在网络线程直接改渲染组。
- 卸载／退出：SBM 清理登记和 VBO；TACZ 同时清理槽位状态和外部资源表示，不保留旧世界强引用。
- 配置关闭／兼容环境切换：当帧回退并停止 VBO 输出，重新启用按未就绪路径预热。

不新增 TACZ 的 GPU map、上传队列或 VertexBuffer 池。

## 6. 第三人称与掉落物复用的边界

两者与展示枪使用相同的静态完整外观组合：资源表示、配件和弹药状态解析、SBM 捕获流程及材质分流一致。没有染色需求，不引入瞄具模板特殊处理或枪体骨骼动画，也不预先拆成可见组。区别在捕获时的固定视角定位、运行时实例矩阵、光照、overlay 与绘制时机。

- 展示枪继续在捕获时烘入 `applyFixed`；第三人称右手和 `GROUND` 在同一捕获流程里先补偿 ItemRenderer 的 `(-0.5,-0.5,-0.5)` 位移，再烘入自己的 SBM 定位及 display 缩放。物品 VBO 键包含视角，场景运动与掉落旋转不入键。
- 在 `GunItemRendererWrapper.renderByItem` 的原调用位置即时绘制，不能延后到世界 `AFTER_BLOCK_ENTITIES` 阶段。继续遵守第三人称左手跳过规则；第一人称保留原链路。
- 物品后端按外观键和视角在所有对应实例间共享 VBO。展示枪、手持和掉落的静态定位变换不同，当前分别上传对应视角的网格。
- 局部模型旋转和非均匀缩放要求法线与 shader 光源处于一致空间。主 pass、阴影 pass 和缓冲绘制顺序分别验证；不能仅因启用光影或 AR 而回退。
- `DRAWN` 与 `PENDING` 都由 VBO 路径接管，后者本帧跳过绘制但推进准备工作。VBO 默认优先于 AR，只有明确 `UNSUPPORTED` 或用户关闭功能才进入旧路径。
- 若现有第三人称枪口世界位置上报、粒子和激光仍需执行，将它们保留在静态网格外的渲染器流程，不通过旧枪体 render 来取得结果。

具体类、缓存状态、交接和验证步骤见 [第三人称 VBO 实施细节](D:/Minecraft/Dev/SimpleBedrockModel/docs/third-person-item-vbo-design.md)。

## 7. 实施批次与验收

| 批次 | 交付物 | 完成条件 |
|---|---|---|
| A：准备 | 旧路径变换整理、SBM 模型转换入口、源码审计和本文 | Java 编译通过；旧路径矩阵运算顺序保持一致 |
| B：纯几何 | SBM 枪体／配件资源、GunAppearance、SBM 实例状态及捕获 | 首批固定枪身已可编译；配件和坐标对照仍待完成 |
| C：接管 | SBM 帧级接管契约、槽位登记、INSTANCE、失效／回退 | API 与首批调用已可编译；首次加载、旋转、取放、LOD、重载仍待游戏内验收 |
| D：验收 | 开关、等待／不支持原因统计、A/B 记录 | 原版／Embeddium／已验证光影的正确性与性能实测，确认 VBO 高于 AR 的优先级 |
| E：第三人称／掉落物 | 独立矩阵绘制入口与动态效果衔接 | 旋转、非均匀缩放、光影、未就绪跳过、激光及枪口粒子验证 |

B/C 的本地依赖已准备：带即时物品 VBO 和双材质几何收集 API 的 SBM `2.5.17-forge-mc1.20.1` 已发布至 Maven Local，TACZ 优先解析 Maven Local，依赖与 jarJar 范围同步到 2.5.17。当前原型仍需游戏内确认，未来修改 SBM 公共 API 时再次提升版本，不覆盖已使用的同名制品。

展示枪开关位于 TACZ 客户端配置 `render.EnableDisplayGunVbo`，物品即时 VBO 独立使用 `render.EnableExternalItemGunVbo`，两者默认 true。受支持的外观优先走 VBO，包括光影开启且 AR 同时可用的情况；未就绪时跳过本次枪体绘制。预览、复杂配件组合和自定义旧模型子类继续原路径。展示枪现有世界阶段仅负责主场景绘制；物品即时后端在当前 pass 内提交。自发光 pass 复用原版 `rendertype_energy_swirl` shader，配置独立纹理、静态 UV、cutout 深度与不混合状态；不注册新 core shader。

第三人称右手与掉落物的即时 VBO 入口、未就绪跳过、独立默认开启配置和动态激光／枪口外围调用已实现并通过 Java 编译；游戏内绘制行为仍需在实际接入场景中确认。

游戏内验收至少覆盖：四个朝向、八档槽位旋转、高低模边界、内置／安装配件、replace／overlay 变体、slot adapter、满／空弹匣、非均匀缩放、自发光、普通／发光挂板与展示柜、预览、锁定展示柜、取放、破坏容器、切世界、枪包重载和 F3+T。

性能场景分别测相同枪械、不同配装和不同贴图，记录稳定期捕获／上传增量、draw、CPU 时间、帧时间波动与显存。INSTANCE 只消除重复几何处理，不承诺合成一次 draw；SECTION 复制几何且受材质数约束。没有实机数据前不宣称提升比例。

## 8. 参考文档

- [当前 SBM 渲染组 API](D:/Minecraft/Dev/SimpleBedrockModel/docs/world-mesh-rendering.md)
- [调用链与缓存归属](D:/Minecraft/Dev/SimpleBedrockModel/docs/world-mesh-call-chain.md)
- [Section 实验与边界](D:/Minecraft/Dev/SimpleBedrockModel/docs/world-mesh-section-prototype.md)
- [第三人称 VBO 设计](D:/Minecraft/Dev/SimpleBedrockModel/docs/third-person-item-vbo-design.md)

`static-block-entity-rendering-plan.md` 保留为早期讨论记录；Source、手动句柄、后台重建等旧方案描述不作为本次实际 API 契约。

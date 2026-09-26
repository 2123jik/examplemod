# Tooltip 功能调研与集合

## 1. 调研口径

- 目标平台：Minecraft 1.21.1，优先考虑 NeoForge 客户端实现。
- 截止时间：以 2026-09-19 为调研日，纳入最后更新时间不早于 2024-09-19 的项目。
- 主题范围：改变物品、方块、实体、容器、界面控件或 HUD 提示信息的模组。
- 统计方式：以功能为单位合并同类项目；一个项目可以贡献多个能力。
- 项目来源：CurseForge 作为用户指定的发现入口，Modrinth API 作为可公开访问的交叉检索和项目正文来源。

CurseForge 页面在本次环境中始终返回 Cloudflare JavaScript challenge/403，无法可靠取得其搜索结果。因此本文不声称是 CurseForge 的可验证全量结果；来源项目链接优先指向 Modrinth 项目页，项目正文中出现的 CurseForge 链接仅作为交叉线索。

检索关键词包括 `tooltip`、`tooltips`、`item tooltip`、`enchantment tooltip`、`container tooltip`、`advanced tooltip` 和 `hover information`。多关键词结果去重后得到 134 个支持 Minecraft 1.21.1 的候选项目，其中 129 个满足时间和文本相关性初筛。初筛结果包含外围整合包插件，最终按功能相关性分层。

## 2. 功能总集合

### A. 信息内容扩展

| 原子能力 | 具体内容 | 代表来源 |
|---|---|---|
| 耐久度 | 当前耐久、最大耐久、耐久百分比、耐久条或耐久状态 | [Durability Tooltip](https://modrinth.com/mod/durability-tooltip)、[Tooltips TXF](https://modrinth.com/mod/tooltips-txf)、[Inline Tooltips](https://modrinth.com/mod/inline-tooltips) |
| 工具属性 | 攻击伤害、攻击速度、挖掘速度、攻击范围、护甲及护甲韧性 | [Cleaner Tooltips](https://modrinth.com/mod/cleaner-tooltips)、[Item Tooltips](https://modrinth.com/mod/item-tooltips)、[Mining Speed Indicators](https://modrinth.com/mod/mining-speed-indicators) |
| 属性图标化 | 用图标替代或辅助属性文字，支持资源包驱动图标 | [Cleaner Tooltips](https://modrinth.com/mod/cleaner-tooltips)、[Inline Tooltips](https://modrinth.com/mod/inline-tooltips) |
| 属性比较 | 将悬停物品与已装备物品比较，显示增加、减少和差值 | [Cleaner Tooltips](https://modrinth.com/mod/cleaner-tooltips)、[Equipment Compare](https://modrinth.com/mod/equipment-compare) |
| 附魔说明 | 显示附魔的自然语言说明、准确数值、伤害加成、概率和每级成长 | [Enchantment Insights](https://modrinth.com/mod/enchantment-insights)、[Exact Enchantments](https://modrinth.com/mod/exact-enchantments) |
| 附魔 HUD | 物品栏或快捷栏选中物品时，在 HUD 显示附魔列表 | [Enchantment Tooltips](https://modrinth.com/mod/enchantment-tooltips)、[Held Item Tooltips](https://modrinth.com/mod/held-item-tooltips) |
| 附魔状态标记 | 标记附魔是否达到等级上限，或显示工作惩罚等附加状态 | [Enchantment Level Cap Indicator](https://modrinth.com/mod/enchantment-level-cap-indicator)、[Work Penalty](https://modrinth.com/mod/work-penalty) |
| 食物信息 | 营养值、饱和度、食用后效果、效果时长和效果说明 | [Tooltips TXF](https://modrinth.com/mod/tooltips-txf)、[Food Effect Tooltips](https://modrinth.com/mod/foodeffecttooltips)、[Effect Insights](https://modrinth.com/mod/effect-insights) |
| 药水与状态效果 | 在物品提示中展示效果名称、效果描述、等级和持续时间 | [Effect Insights](https://modrinth.com/mod/effect-insights)、[Held Item Tooltips](https://modrinth.com/mod/held-item-tooltips) |
| 燃料信息 | 是否可燃、燃烧时间，可按 tick 或秒显示 | [Tooltips TXF](https://modrinth.com/mod/tooltips-txf)、[Inline Tooltips](https://modrinth.com/mod/inline-tooltips) |
| 堆肥信息 | 是否可堆肥及进入堆肥桶的概率 | [Tooltips TXF](https://modrinth.com/mod/tooltips-txf) |
| 冷却信息 | 物品使用冷却时间 | [Tooltips TXF](https://modrinth.com/mod/tooltips-txf) |
| 标签和组件 | 显示物品标签、Data Component、注册名或调试信息 | [Better Advanced Tooltips](https://modrinth.com/mod/better-advanced-tooltips)、[Tag & Component Tooltips](https://modrinth.com/mod/tag-and-component-tooltips)、[NBT Viewer](https://modrinth.com/mod/items-nbt-viewer) |
| NBT/数据查看 | 在提示中直接展开或查看物品 NBT、组件数据 | [NBT Viewer](https://modrinth.com/mod/items-nbt-viewer) |
| 版本信息 | 显示物品首次加入的 Minecraft 版本或快照版本 | [Version Tooltips](https://modrinth.com/mod/version-tooltips)、[Added In](https://modrinth.com/mod/added-in) |
| 自定义文本 | 通过 JSON、配置文件或数据驱动规则给物品增加说明文字 | [Custom Tooltip API](https://modrinth.com/mod/custom-tooltip-api)、[Misia's Simple ToolTips](https://modrinth.com/mod/misias-simple-tooltips)、[Tooltip Creator](https://modrinth.com/mod/tooltip-creator) |
| 翻译和双语 | 显示第二语言名称、翻译完整提示文本或按键切换语言 | [Bilingual Tooltips](https://modrinth.com/mod/bilingual-tooltips)、[SimpleTranslate](https://modrinth.com/mod/simpletranslation)、[xkball's Auto Translate](https://modrinth.com/mod/xkballs-auto-translate) |
| 专项领域数据 | 矿石生成条件、蜜蜂数量和蜂蜜等级、盔甲纹饰、物品来源等 | [TFC Ore Tooltips](https://modrinth.com/mod/tfc-ore-tooltips)、[Swin's Beehive Counter](https://modrinth.com/mod/swins-beehive-counter)、[Better Trim Tooltips Reborn](https://modrinth.com/mod/better-trim-tooltips-reborn) |

### B. 容器与复合内容预览

| 原子能力 | 具体内容 | 代表来源 |
|---|---|---|
| 潜影盒预览 | 悬停物品时预览潜影盒内容 | [Shulker Box Tooltip](https://modrinth.com/mod/shulkerboxtooltip) |
| 容器通用预览 | 预览箱子、木桶、熔炉及多数模组容器 | [Shulker Box Tooltip](https://modrinth.com/mod/shulkerboxtooltip)、[Container Tooltips](https://modrinth.com/mod/container-tooltips) |
| 紧凑/完整预览 | 紧凑列表预览和按键触发的完整槽位布局预览 | [Shulker Box Tooltip](https://modrinth.com/mod/shulkerboxtooltip) |
| 束口袋预览 | 展示 Bundle 内的物品及容量 | [Shulker Box Tooltip](https://modrinth.com/mod/shulker-box-tooltip-mod)、[Frostbyte's Improved Inventory](https://modrinth.com/mod/frostbytes-improved-inventory) |
| 地图预览 | 在背包、箱子或地面物品提示中显示地图内容 | [Map Tooltip](https://modrinth.com/mod/map-tooltip)、[Show My Maps](https://modrinth.com/mod/show-my-maps) |
| Create 容器预览 | 预览 Package、Toolbox 等 Create 容器内容 | [Create Container Tooltip](https://modrinth.com/mod/create-container-tooltip)、[Create Toolbox Tooltip](https://modrinth.com/mod/create-toolbox-tooltip) |
| 外部背包预览 | 为服务器插件或模组背包增加类似潜影盒的预览 | [HavenBags Preview](https://modrinth.com/mod/havenbagspreview) |

### C. 显示位置与交互

| 原子能力 | 具体内容 | 代表来源 |
|---|---|---|
| 快捷栏上方提示 | 选中快捷栏物品时显示附魔、药水、潜影盒等信息 | [Held Item Tooltips](https://modrinth.com/mod/held-item-tooltips)、[Not Enough Tooltips](https://modrinth.com/mod/notenoughtooltips) |
| 世界物品提示 | 看向掉落物时显示其原生物品提示 | [Hysk-Tooltips](https://modrinth.com/mod/hysk-tooltips) |
| 方块/实体注视提示 | 在 HUD 显示正在看的方块或实体名称、生命值、护甲等 | [BET - Block'n'Entity Tooltip](https://modrinth.com/mod/block-entity-tooltip)、[Polydex](https://modrinth.com/mod/polydex) |
| 悬停位置移动 | 将提示移动到鼠标周围指定位置，避免遮挡目标 | [Tooltip Scroll](https://modrinth.com/mod/tooltip-scroll) |
| 提示滚动 | 超长提示可滚动，支持鼠标滚轮或按键 | [Tooltip Scroll](https://modrinth.com/mod/tooltip-scroll)、[Smart Tooltip Scroll](https://modrinth.com/mod/smart-tooltip-scroll) |
| 提示固定 | 将当前提示钉在屏幕上，同时继续查看其他物品 | [Pin Tooltips](https://modrinth.com/mod/pin-tooltips) |
| 动态显示/隐藏 | 按键、Shift、Alt 等修饰键切换详细信息或隐藏提示 | [Inline Tooltips](https://modrinth.com/mod/inline-tooltips)、[HideTooltip](https://modrinth.com/mod/hidetooltip)、[Don't Hide The Tooltip](https://modrinth.com/mod/donthidethetooltip) |
| 持物仍显示 | 即使鼠标或玩家正持有其他物品，也不自动隐藏悬停提示 | [Don't Hide The Tooltip](https://modrinth.com/mod/donthidethetooltip) |
| 提示开关集合 | 分别开关耐久、附魔、标签、HUD 提示等功能 | [TooltipToggles](https://modrinth.com/mod/tooltiptoggles) |
| 槽位调试 | 显示当前库存槽位 ID 或空槽位用途 | [SlotID Tooltip](https://modrinth.com/mod/slotid-tooltip)、[Slot Customization API](https://modrinth.com/mod/slot-customization-api) |

### D. 布局、样式和可读性

| 原子能力 | 具体内容 | 代表来源 |
|---|---|---|
| 自适应尺寸 | 根据内容动态扩大或限制提示框宽度和高度 | [Smart Tooltip Scroll](https://modrinth.com/mod/smart-tooltip-scroll)、[Dynamic Tooltips](https://modrinth.com/mod/dynamic-tooltips) |
| 标题固定 | 滚动长提示时固定物品图标和标题区域 | [Smart Tooltip Scroll](https://modrinth.com/mod/smart-tooltip-scroll) |
| 内容折叠 | 合并属性、压缩空行、折叠重复或次要信息 | [Dynamic Tooltips](https://modrinth.com/mod/dynamic-tooltips)、[Apothic Tooltip Cleanup](https://modrinth.com/mod/apothic-tooltip-cleanup) |
| Shift 展开详情 | 默认显示简洁版本，按住 Shift 展示完整统计 | [Shrink It](https://modrinth.com/mod/shrinkit)、[Inline Tooltips](https://modrinth.com/mod/inline-tooltips) |
| 颜色配置 | 配置背景、上下边框、渐变和透明度 | [Recoloured Tooltips](https://modrinth.com/mod/recoloured-tooltips)、[ColorTooltips](https://modrinth.com/mod/colortooltips) |
| 稀有度样式 | 按物品稀有度使用边框、颜色、发光、动画和粒子效果 | [Legendary Tooltips](https://modrinth.com/mod/legendary-tooltips)、[Obscure Tooltips](https://modrinth.com/mod/obscure-tooltips) |
| 主题化渲染 | 自定义背景、边框、字体、分隔线和图标排版 | [Vanilla Tooltips](https://modrinth.com/mod/vanilla-tooltips)、[Tooltip Overhaul](https://modrinth.com/mod/tooltip-overhaul) |
| 资源包驱动 | 用资源包替换图标和渲染纹理，而不是硬编码贴图 | [Cleaner Tooltips](https://modrinth.com/mod/cleaner-tooltips) |
| 图标前置 | 将属性图标放到属性文字之前 | [Icon Leading Tooltip](https://modrinth.com/mod/icon-leading-tooltip) |
| 渲染性能 | 缓存提示布局和文本宽度测量，减少 GUI 悬停卡顿 | [Sodium Relief](https://modrinth.com/mod/sodium-relief) |

### E. API、配置与兼容性

| 原子能力 | 具体内容 | 代表来源 |
|---|---|---|
| 数据驱动 API | 由数据包、JSON 或其他模组注册提示文本和规则 | [Custom Tooltip API](https://modrinth.com/mod/custom-tooltip-api)、[LibTooltips](https://modrinth.com/mod/libtooltips) |
| 分类开关 | 按工具、护甲、食物、附魔、方块、容器和提示类型单独启用 | [Item Tooltips](https://modrinth.com/mod/item-tooltips)、[Enhanced Tooltips](https://modrinth.com/mod/tooltips-enhanced) |
| 按键绑定 | 为详细模式、标签、固定、滚动或显示位置提供按键 | [Tag Tooltips](https://modrinth.com/mod/tag-tooltips)、[Pin Tooltips](https://modrinth.com/mod/pin-tooltips) |
| 客户端独立 | 不要求服务器安装，仅依据客户端可见的物品和组件渲染 | [Durability Tooltip](https://modrinth.com/mod/durability-tooltip)、[Mining Speed Indicators](https://modrinth.com/mod/mining-speed-indicators) |
| Mod 集成 | 兼容模组物品、模组容器、Jade/WTHIT、Create、Apotheosis 等 | [Shulker Box Tooltip](https://modrinth.com/mod/shulkerboxtooltip)、[Villager Inventory Plugin](https://modrinth.com/mod/villager-inventory-hwyla-plugin)、[Legendary Tooltips x Equipment Compare](https://modrinth.com/mod/lt-x-ec) |
| 渲染冲突处理 | 处理多个提示模组叠加时的堆叠顺序、计数、遮挡和 z-order | [Legendary Tooltips x Equipment Compare](https://modrinth.com/mod/lt-x-ec) |
| 可访问性 | 通过文本、颜色、图标、紧凑模式降低信息识别成本 | [Cleaner Tooltips](https://modrinth.com/mod/cleaner-tooltips)、[Bilingual Tooltips](https://modrinth.com/mod/bilingual-tooltips) |

## 3. 分层结论

### 核心通用层

首版应覆盖：

1. 物品提示统一拦截和安全重建。
2. 耐久、属性、挖掘速度、食物、药水效果、燃料、堆肥和冷却信息。
3. 附魔说明、准确值、等级上限和 Shift 详情模式。
4. 标签、Data Component、NBT 的可选调试显示。
5. 潜影盒、Bundle、普通容器和地图预览。
6. 提示框颜色、边框、宽高、换行、滚动和超长内容处理。
7. 分类配置、按键绑定、开关和客户端独立运行。

### 第二阶段增强层

1. 属性对比。
2. 提示固定和快捷栏上方 HUD。
3. 方块/实体注视提示。
4. 自定义 JSON/数据驱动文本。
5. 资源包驱动图标和主题。
6. Mod 容器、Create、Jade/WTHIT 等集成。
7. 提示布局缓存和渲染冲突处理。

### 专项或外围层

以下能力可作为扩展插件，不应阻塞通用 Tooltip 核心：翻译服务、Cobblemon 战斗数据、TFC 矿石、蜂巢统计、Apotheosis 词条、Create 包装物、MineColonies/Jade 插件，以及 AI 生成方块/实体描述。

## 4. 当前工程对接结论

当前模板是 NeoForge 1.21.1、Java 21 工程。客户端入口为 [`ExampleModClient.java`](../src/main/java/com/example/examplemod/ExampleModClient.java:22)，通用入口为 [`ExampleMod.java`](../src/main/java/com/example/examplemod/ExampleMod.java:8)，配置注册位于 [`ExampleMod.java`](../src/main/java/com/example/examplemod/ExampleMod.java:12)，配置定义位于 [`Config.java`](../src/main/java/com/example/examplemod/Config.java:5)。实现代码位于 [`tooltip/`](../src/main/java/com/example/examplemod/tooltip/)。

### 当前实现状态

已实现并接入运行链：

1. 通过 `ItemTooltipEvent` 保留原版内容，增量输出耐久值/百分比、属性及装备比较、工具规则、食物营养与食物效果、药水效果名称/等级/持续时间、燃料、堆肥、冷却、附魔等级与上限、Data Component、Custom Data、注册名、Tag、地图 ID 和容器总堆数/总物品数。
2. 通过 [`TooltipContribution.java`](../src/main/java/com/example/examplemod/tooltip/TooltipContribution.java:6) 与 [`TooltipLayout.java`](../src/main/java/com/example/examplemod/tooltip/TooltipLayout.java:8) 完成分类、排序、优先级、去重、最大行数和扩展贡献汇总。
3. 通过 [`TooltipInteraction.java`](../src/main/java/com/example/examplemod/tooltip/TooltipInteraction.java:13) 实现 P 键固定、鼠标滚轮分页和原版 TooltipComponent 兼容的固定提示。
4. 通过 [`ExampleModClient.java`](../src/main/java/com/example/examplemod/ExampleModClient.java:33) 实现持物 HUD、掉落物/方块/实体注视 HUD、生命/护甲信息、最大宽度、配置颜色和稀有度边框主题。
5. 通过 [`TooltipRules.java`](../src/main/java/com/example/examplemod/tooltip/TooltipRules.java:20) 支持资源包 `tooltips/*.json` 的自定义文本、版本字段和第二语言字段，并使用客户端资源重载生命周期热更新。
6. 通过 [`TooltipContributors.java`](../src/main/java/com/example/examplemod/tooltip/TooltipContributors.java:12) 提供第三方模组注入结构化 Tooltip 贡献和原始事件处理器的扩展入口。
7. 已执行 `gradlew.bat clean build`，构建通过。

### 尚未完整实现的集合项

以下内容不能由当前模组单独可靠完成，当前只保留扩展入口或原版降级行为：Create/外部背包/Jade/WTHIT/Apotheosis 的协议级集成、翻译服务、Cobblemon/TFC/蜂巢等专项数据、服务端隐藏数据、真正的图形化潜影盒/通用容器预览、资源包图标前置、完整标题固定布局和多模组 z-order 协调。它们需要对应模组 API、服务端同步协议或更深的 GUI 渲染改造，不能用客户端猜测数据替代。容器屏幕槽位 ID 与空槽状态调试已通过 NeoForge 前景事件接入，并默认关闭。

当前不使用本地整目录 JAR 作为 Tooltip 实现依赖；构建只依赖模板声明的 NeoForge 开发环境。

## 5. 功能验收矩阵

| 集合 | 状态 | 实际落点 |
|---|---|---|
| 信息扩展 | 部分完成 | 原生组件、数据映射、Custom Data、JSON 规则已完成；第三方专项数据和在线翻译未接入 |
| 容器预览 | 文本完成/图形部分 | `CONTAINER`、`BUNDLE_CONTENTS`、地图 ID 与原版图形组件兼容；第三方容器未接入 |
| 显示交互 | 核心完成 | 持物 HUD、注视 HUD、固定、滚动、修饰键、属性比较和槽位 ID/空槽状态覆盖层已接入 |
| 布局样式 | 部分完成 | 宽度、换行、行数、折叠式详情、颜色、稀有度主题已完成；标题固定和完整主题渲染未完成 |
| API/兼容 | 核心完成 | 配置、JSON 重载、贡献注册、原始事件扩展已完成；第三方协议适配器需由对应模组提供 |

## 6. 建议的内部数据模型

Tooltip 功能应拆成三个阶段：

1. `TooltipContext`：记录物品栈、来源屏幕、鼠标位置、指针实体/方块、修饰键和配置快照。
2. `TooltipContribution`：每个功能模块只提交带分类、优先级、可见条件和组件内容的贡献项。
3. `TooltipLayout`：统一进行排序、分组、换行、折叠、滚动区域计算和最终渲染。

建议的贡献分类：`IDENTITY`、`DURABILITY`、`ATTRIBUTES`、`ENCHANTMENTS`、`EFFECTS`、`FOOD`、`FUEL`、`TAGS`、`COMPONENTS`、`CONTAINER_PREVIEW`、`CUSTOM` 和 `DEBUG`。

统一布局层必须保留原版 Tooltip 的默认内容和渲染入口，功能模块只增量添加或替换明确分类，避免多个模块直接互相修改同一个 `List<Component>` 导致顺序和兼容性不可控。

## 7. 需要明确的取舍

- “所有模组”无法在 CurseForge 反爬挑战下被严格证明；本文将公开可访问平台的候选作为发现集，并保留来源链接。
- 搜索结果中的项目更新时间晚于边界不代表其在边界日前不存在；本文使用项目当前最近更新时间作为纳入条件。
- 某些项目虽然支持 1.21.1，但仅支持 Fabric 或仅支持 Forge/NeoForge，不能直接作为本项目实现依赖。
- 1.21.1 的 NBT 相关能力应优先抽象为 Data Component 读取，同时保留兼容旧 NBT 的适配层。
- 服务端真实数据不可由纯客户端凭空推断；容器内容、实体属性和模组自定义信息需要依据客户端同步数据或对应整合 API。

## 8. 来源数据文件

- [`modrinth-tooltip-search.json`](../modrinth-tooltip-search.json)：首次 `tooltip` 搜索返回的原始项目数据。
- [`tooltip-candidates.json`](../tooltip-candidates.json)：多关键词去重后的候选项目集合。
- [`tooltip-deep-projects.json`](../tooltip-deep-projects.json)：代表项目的正文、版本和加载器元数据。
- [`tooltip-core-candidates.tsv`](../tooltip-core-candidates.tsv)：按名称、描述和支持版本整理的候选摘要。

以上文件是调研缓存，不是运行时依赖；后续实现阶段只保留必要的代码和配置。

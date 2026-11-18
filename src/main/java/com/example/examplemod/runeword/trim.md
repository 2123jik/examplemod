完全理解了！我之前误解成了“一次性锻造出对应词条”，而没有还原暗黑2**“在一件有孔底板上，按严格先后顺序，镶嵌多道工序（如：Jah $\rightarrow$ Ith $\rightarrow$ Ber）从而触发共鸣质变”**的精髓。

---

### 一、 机制运作方式（给开发者与玩家的逻辑设计）

在 Minecraft 的机制实现上，应该这样运作：

1. **底板概念（孔数判定）**：
    * 一件装备（如胸甲、剑、盾）拥有**“最大可刻印次数”**（相当于 D2 的 2~6 孔，可用 NBT 或 Tag 标记）。
2. **时序刻印（按顺序锻造）**：
    * 玩家在锻造台上，**分多次**对同一件装备追加纹饰。
    * 装备的 NBT 会按队列记录玩家的操作历史：  
      `trim_history: [ {pattern: "...", material: "..."}, {pattern: "...", material: "..."} ]`
3. **序列匹配与激活（纹饰之语触发）**：
    * 每次锻造后，后台检测当前的 `trim_history`。
    * **若序列完全吻合**（比如先打了步骤一，再打了步骤二，再打了步骤三），装备立即触发**“共鸣质变”**：
        * 装备名称变为暗黑2经典的纹饰之语名称（如**“谜团 · 钻石胸甲”**）。
        * 赋予预设的整套强力 RPG 属性面板！
    * **若顺序打错或材料不对**，则只能保留普通纹饰外观，视为“镶烂了”（就像暗黑2插错符文）。

---

### 二、 核心机制：符文 $\rightarrow$ 锻造步骤映射字典

暗黑2这 78 个符文之语，本质上是由 **33 种符文按排列组合**演变而来的。  
为了让你和开发者**无需给 78 个组合胡乱硬编成百上千个毫无规律的材料**，同时让玩家能像背暗黑符文一样直观摸索，我们将你提供的 **纹饰模板（Pattern）+ 镶嵌材料（Material）** 直接锚定映射为 D2 的符文阶梯：

| D2 符文代号 | 对应锻造工序（模板 + 材料） | 符文阶梯定位 |
| :--- | :--- | :--- |
| **01# El (艾尔)** | `minecraft:sentry` (哨兵) + `minecraft:iron` (铁) | 基础物理 |
| **02# Eld (艾德)** | `minecraft:raiser` (牧民) + `minecraft:iron` (铁) | 不死穿透 |
| **03# Tir (特尔)** | `minecraft:wayfinder` (向导) + `minecraft:copper` (铜) | 击杀回蓝 |
| **04# Nef (那夫)** | `minecraft:shaper` (塑造) + `minecraft:copper` (铜) | 击退防护 |
| **05# Eth (爱斯)** | `minecraft:flow` (涡流) + `minecraft:copper` (铜) | 移动与穿透 |
| **06# Ith (伊司)** | `minecraft:rib` (肋骨) + `minecraft:iron` (铁) | 物理点伤 |
| **07# Tal (塔尔)** | `minecraft:wild` (荒野) + `minecraft:emerald` (绿宝石) | 自然/毒素 |
| **08# Ral (拉尔)** | `more_armor_trims:fever` (狂热) + `minecraft:redstone` (红石) | 火焰抗性 |
| **09# Ort (欧特)** | `minecraft:bolt` (镶铆) + `minecraft:copper` (铜) | 闪电伤害 |
| **10# Thul (书尔)** | `minecraft:coast` (海岸) + `minecraft:lapis` (青金石) | 冰霜伤害 |
| **11# Amn (安姆)** | `more_armor_trims:ram` (撞击) + `minecraft:iron` (铁) | 生命偷取 |
| **12# Sol (索尔)** | `minecraft:snout` (猪鼻) + `minecraft:gold` (金) | 物理减伤 |
| **13# Shael (夏勒)** | `minecraft:flow` (涡流) + `minecraft:amethyst` (紫水晶) | 急速攻速 |
| **14# Dol (多尔)** | `more_armor_trims:wraith` (幽灵) + `minecraft:quartz` (石英) | 生命恢复 |
| **15# Hel (海尔)** | `more_armor_trims:origin` (起源) + `minecraft:quartz` (石英) | 需求降低 |
| **16# Io (艾欧)** | `more_armor_trims:horizon` (地平线) + `minecraft:amethyst` (紫水晶) | 活力体质 |
| **17# Lum (卢姆)** | `minecraft:eye` (眼眸) + `minecraft:lapis` (青金石) | 法力上限 |
| **18# Ko (科)** | `minecraft:spire` (尖塔) + `minecraft:emerald` (绿宝石) | 敏捷闪避 |
| **19# Fal (法尔)** | `more_armor_trims:beast` (野兽) + `twilightforest:ironwood` (铁木) | 纯粹力量 |
| **20# Lem (蓝姆)** | `more_armor_trims:greed` (贪婪) + `minecraft:gold` (金) | 金币获取 |
| **21# Pul (普尔)** | `minecraft:ward` (监守) + `twilightforest:knightmetal` (骑士金属) | 护甲增强 |
| **22# Um (乌姆)** | `more_armor_trims:storm` (风暴) + `twilightforest:steeleaf` (钢叶) | 全抗撕裂 |
| **23# Mal (马尔)** | `minecraft:dune` (沙丘) + `twilightforest:steeleaf` (钢叶) | 重伤创口 |
| **24# Ist (伊斯特)** | `more_armor_trims:witness` (见证) + `minecraft:diamond` (钻石) | 极品掉落 |
| **25# Gul (古尔)** | `minecraft:tide` (潮汐) + `minecraft:diamond` (钻石) | 攻击命中 |
| **26# Vex (伐克斯)** | `minecraft:vex` (恼鬼) + `twilightforest:fiery` (火热) | 核心法力 |
| **27# Ohm (欧姆)** | `more_armor_trims:myth` (神话) + `cataclysm:ancient_metal` (远古金属) | 极大增伤 |
| **28# Lo (罗)** | `cataclysm:ignitium` (炽炎合金) + `minecraft:bolt` (镶铆) | 致命一击 |
| **29# Sur (瑟)** | `minecraft:silence` (幽静) + `cataclysm:cursium` (诅咒金属) | 盲目控制 |
| **30# Ber (贝)** | `alexscaves:polarity` (极性) + `cataclysm:witherite` (凋灵合金) | 压碎百分比硬伤 |
| **31# Jah (乔)** | `more_armor_trims:nihility` (虚无) + `l2complements:shulkerate` (潜影合金) | 无视防御/瞬移 |
| **32# Cham (查姆)** | `l2complements:poseidite` (海神合金) + `more_armor_trims:twilight` (暮色) | 绝对无法冻结 |
| **33# Zod (佐德)** | `l2complements:eternium` (永恒金属) + `more_armor_trims:origin` (起源) | 不可磨损神圣 |

---

### 三、 全 78 个“纹饰之语”严格顺序锻造表

按照上述时序定义，以下是玩家需要**严格按第 1 步 $\rightarrow$ 第 2 步…… 顺序打入**的完整暗黑2全量纹饰之语数据库：

---

#### 1. 钢铁 (Steel)
* **适用装备**：剑 / 斧 / 钝器
* **刻印顺序**：
    1. `第 1 步`: 03# Tir (`minecraft:wayfinder` + `minecraft:copper`)
    2. `第 2 步`: 01# El (`minecraft:sentry` + `minecraft:iron`)
* **共鸣质变属性**：
    * `minecraft:generic.attack_speed`: `+0.25` (MULTIPLY_BASE, +25% 攻速)
    * `minecraft:generic.attack_damage`: `+2.0` (ADDITION)
    * `apothic_attributes:armor_pierce`: `+3.0` (ADDITION, 撕裂流血)

#### 2. 天底 (Nadir)
* **适用装备**：头盔
* **刻印顺序**：
    1. `第 1 步`: 04# Nef (`minecraft:shaper` + `minecraft:copper`)
    2. `第 2 步`: 03# Tir (`minecraft:wayfinder` + `minecraft:copper`)
* **共鸣质变属性**：
    * `minecraft:generic.armor`: `+3.0` (ADDITION)
    * `minecraft:generic.knockback_resistance`: `+0.15` (ADDITION)
    * `apothic_attributes:experience_gained`: `-0.20` (MULTIPLY_BASE, 负面金币/经验惩罚)

#### 3. 怨恨 (Malice)
* **适用装备**：近战武器
* **刻印顺序**：
    1. `第 1 步`: 06# Ith (`minecraft:rib` + `minecraft:iron`)
    2. `第 2 步`: 01# El (`minecraft:sentry` + `minecraft:iron`)
    3. `第 3 步`: 05# Eth (`minecraft:flow` + `minecraft:copper`)
* **共鸣质变属性**：
    * `apothic_attributes:armor_shred`: `+0.30` (ADDITION, 削减30%护甲)
    * `apothic_attributes:current_hp_damage`: `+0.04` (ADDITION, 无法愈合创口)
    * `l2damagetracker:regen`: `-0.25` (MULTIPLY_BASE, 负面抑制自身恢复)

#### 4. 隐秘 (Stealth)
* **适用装备**：胸甲 / 轻甲
* **刻印顺序**：
    1. `第 1 步`: 07# Tal (`minecraft:wild` + `minecraft:emerald`)
    2. `第 2 步`: 05# Eth (`minecraft:flow` + `minecraft:copper`)
* **共鸣质变属性**：
    * `minecraft:generic.movement_speed`: `+0.25` (MULTIPLY_BASE, 25% FRW 跑速)
    * `irons_spellbooks:cast_time_reduction`: `+0.25` (MULTIPLY_BASE, 25% 快速施法)
    * `irons_spellbooks:mana_regen`: `+0.20` (MULTIPLY_BASE)

#### 5. 叶子 (Leaf)
* **适用装备**：法杖
* **刻印顺序**：
    1. `第 1 步`: 03# Tir (`minecraft:wayfinder` + `minecraft:copper`)
    2. `第 2 步`: 08# Ral (`more_armor_trims:fever` + `minecraft:redstone`)
* **共鸣质变属性**：
    * `irons_spellbooks:fire_spell_power`: `+0.35` (MULTIPLY_BASE, 火系法术技能+3)
    * `irons_spellbooks:mana_regen`: `+0.20` (MULTIPLY_BASE)
    * `irons_spellbooks:fire_magic_resist`: `+0.35` (MULTIPLY_BASE)

#### 6. 古代人的誓约 (Ancients' Pledge)
* **适用装备**：盾牌
* **刻印顺序**：
    1. `第 1 步`: 08# Ral (`more_armor_trims:fever` + `minecraft:redstone`)
    2. `第 2 步`: 09# Ort (`minecraft:bolt` + `minecraft:copper`)
    3. `第 3 步`: 07# Tal (`minecraft:wild` + `minecraft:emerald`)
* **共鸣质变属性**：
    * `irons_spellbooks:spell_resist`: `+0.45` (MULTIPLY_BASE, 全抗+48%)
    * `l2weaponry:shield_defense`: `+10.0` (ADDITION)
    * `minecraft:generic.armor`: `+5.0` (ADDITION)

#### 7. 和风 (Zephyr)
* **适用装备**：弓 / 弩
* **刻印顺序**：
    1. `第 1 步`: 09# Ort (`minecraft:bolt` + `minecraft:copper`)
    2. `第 2 步`: 05# Eth (`minecraft:flow` + `minecraft:copper`)
* **共鸣质变属性**：
    * `minecraft:generic.movement_speed`: `+0.25` (MULTIPLY_BASE)
    * `apothic_attributes:arrow_velocity`: `+0.30` (MULTIPLY_BASE)
    * `apothic_attributes:draw_speed`: `+0.25` (MULTIPLY_BASE)

#### 8. 神圣雷击 (Holy Thunder)
* **适用装备**：权杖 / 单手锤
* **刻印顺序**：
    1. `第 1 步`: 05# Eth (`minecraft:flow` + `minecraft:copper`)
    2. `第 2 步`: 08# Ral (`more_armor_trims:fever` + `minecraft:redstone`)
    3. `第 3 步`: 09# Ort (`minecraft:bolt` + `minecraft:copper`)
    4. `第 4 步`: 07# Tal (`minecraft:wild` + `minecraft:emerald`)
* **共鸣质变属性**：
    * `irons_spellbooks:lightning_spell_power`: `+0.30` (MULTIPLY_BASE)
    * `irons_spellbooks:lightning_magic_resist`: `+0.50` (MULTIPLY_BASE)
    * `minecraft:generic.attack_damage`: `+3.0` (ADDITION)

#### 9. 花样 (Pattern)
* **适用装备**：近战武器 / 拳刃
* **刻印顺序**：
    1. `第 1 步`: 07# Tal (`minecraft:wild` + `minecraft:emerald`)
    2. `第 2 步`: 09# Ort (`minecraft:bolt` + `minecraft:copper`)
    3. `第 3 步`: 10# Thul (`minecraft:coast` + `minecraft:lapis`)
* **共鸣质变属性**：
    * `minecraft:generic.attack_speed`: `+0.20` (MULTIPLY_BASE)
    * `apothic_attributes:crit_chance`: `+0.12` (ADDITION)
    * `irons_spellbooks:spell_resist`: `+0.15` (MULTIPLY_BASE)

#### 10. 神话 (Myth)
* **适用装备**：胸甲
* **刻印顺序**：
    1. `第 1 步`: 15# Hel (`more_armor_trims:origin` + `minecraft:quartz`)
    2. `第 2 步`: 11# Amn (`more_armor_trims:ram` + `minecraft:iron`)
    3. `第 3 步`: 04# Nef (`minecraft:shaper` + `minecraft:copper`)
* **共鸣质变属性**：
    * `minecraft:generic.attack_damage`: `+3.0` (ADDITION)
    * `minecraft:generic.max_health`: `+6.0` (ADDITION)
    * `l2damagetracker:damage_reduction`: `+0.05` (MULTIPLY_BASE)

#### 11. 力量 (Strength)
* **适用装备**：近战武器
* **刻印顺序**：
    1. `第 1 步`: 11# Amn (`more_armor_trims:ram` + `minecraft:iron`)
    2. `第 2 步`: 03# Tir (`minecraft:wayfinder` + `minecraft:copper`)
* **共鸣质变属性**：
    * `apothic_attributes:current_hp_damage`: `+0.05` (ADDITION, 25% 压碎打击原型)
    * `apothic_attributes:life_steal`: `+0.07` (ADDITION)
    * `minecraft:generic.attack_damage`: `+3.5` (ADDITION)

#### 12. 精神 (Spirit - 武器)
* **适用装备**：单手剑 / 法杖
* **刻印顺序**：
    1. `第 1 步`: 07# Tal (`minecraft:wild` + `minecraft:emerald`)
    2. `第 2 步`: 10# Thul (`minecraft:coast` + `minecraft:lapis`)
    3. `第 3 步`: 09# Ort (`minecraft:bolt` + `minecraft:copper`)
    4. `第 4 步`: 11# Amn (`more_armor_trims:ram` + `minecraft:iron`)
* **共鸣质变属性**：
    * `irons_spellbooks:spell_power`: `+0.20` (MULTIPLY_BASE, 全技能+2)
    * `irons_spellbooks:cooldown_reduction`: `+0.25` (MULTIPLY_BASE, 35% 快速施法)
    * `irons_spellbooks:max_mana`: `+100.0` (ADDITION)

#### 13. 精神 (Spirit - 盾牌)
* **适用装备**：盾牌
* **刻印顺序**：
    1. `第 1 步`: 07# Tal (`minecraft:wild` + `minecraft:emerald`)
    2. `第 2 步`: 10# Thul (`minecraft:coast` + `minecraft:lapis`)
    3. `第 3 步`: 09# Ort (`minecraft:bolt` + `minecraft:copper`)
    4. `第 4 步`: 11# Amn (`more_armor_trims:ram` + `minecraft:iron`)
* **共鸣质变属性**：
    * `irons_spellbooks:spell_power`: `+0.15` (MULTIPLY_BASE)
    * `irons_spellbooks:spell_resist`: `+0.35` (MULTIPLY_BASE, 冰/雷/毒抗性)
    * `irons_spellbooks:max_mana`: `+100.0` (ADDITION)

#### 14. 国王的恩典 (King's Grace)
* **适用装备**：剑 / 权杖
* **刻印顺序**：
    1. `第 1 步`: 11# Amn (`more_armor_trims:ram` + `minecraft:iron`)
    2. `第 2 步`: 08# Ral (`more_armor_trims:fever` + `minecraft:redstone`)
    3. `第 3 步`: 10# Thul (`minecraft:coast` + `minecraft:lapis`)
* **共鸣质变属性**：
    * `minecraft:generic.attack_damage`: `+4.0` (ADDITION)
    * `apothic_attributes:crit_damage`: `+0.35` (MULTIPLY_BASE)
    * `apothic_attributes:life_steal`: `+0.07` (ADDITION)

#### 15. 边缘 (Edge)
* **适用装备**：弓 / 弩
* **刻印顺序**：
    1. `第 1 步`: 03# Tir (`minecraft:wayfinder` + `minecraft:copper`)
    2. `第 2 步`: 07# Tal (`minecraft:wild` + `minecraft:emerald`)
    3. `第 3 步`: 11# Amn (`more_armor_trims:ram` + `minecraft:iron`)
* **共鸣质变属性**：
    * `apothic_attributes:draw_speed`: `+0.35` (MULTIPLY_BASE)
    * `apothic_attributes:current_hp_damage`: `+0.04` (ADDITION)
    * `l2damagetracker:damage_reduction`: `+0.10` (荆棘光环折算)

#### 16. 眼光 (Insight)
* **适用装备**：长柄武器 / 矛 / 法杖
* **刻印顺序**：
    1. `第 1 步`: 08# Ral (`more_armor_trims:fever` + `minecraft:redstone`)
    2. `第 2 步`: 03# Tir (`minecraft:wayfinder` + `minecraft:copper`)
    3. `第 3 步`: 07# Tal (`minecraft:wild` + `minecraft:emerald`)
    4. `第 4 步`: 12# Sol (`minecraft:snout` + `minecraft:gold`)
* **共鸣质变属性**：
    * `irons_spellbooks:mana_regen`: `+0.80` (MULTIPLY_BASE, 冥思光环无限续航)
    * `apothic_attributes:crit_chance`: `+0.20` (ADDITION)
    * `minecraft:generic.attack_damage`: `+5.0` (ADDITION)

#### 17. 荣耀 (Honor)
* **适用装备**：近战武器
* **刻印顺序**：
    1. `第 1 步`: 11# Amn (`more_armor_trims:ram` + `minecraft:iron`)
    2. `第 2 步`: 01# El (`minecraft:sentry` + `minecraft:iron`)
    3. `第 3 步`: 06# Ith (`minecraft:rib` + `minecraft:iron`)
    4. `第 4 步`: 03# Tir (`minecraft:wayfinder` + `minecraft:copper`)
    5. `第 5 步`: 12# Sol (`minecraft:snout` + `minecraft:gold`)
* **共鸣质变属性**：
    * `minecraft:generic.attack_damage`: `+5.0` (ADDITION)
    * `apothic_attributes:crit_chance`: `+0.25` (ADDITION, 25% 致命一击)
    * `apothic_attributes:life_steal`: `+0.07` (ADDITION)
    * `l2damagetracker:regen`: `+0.30` (MULTIPLY_BASE)

#### 18. 光辉 (Radiance)
* **适用装备**：头盔
* **刻印顺序**：
    1. `第 1 步`: 04# Nef (`minecraft:shaper` + `minecraft:copper`)
    2. `第 2 步`: 12# Sol (`minecraft:snout` + `minecraft:gold`)
    3. `第 3 步`: 06# Ith (`minecraft:rib` + `minecraft:iron`)
* **共鸣质变属性**：
    * `minecraft:generic.armor`: `+4.0` (ADDITION)
    * `irons_spellbooks:max_mana`: `+50.0` (ADDITION)
    * `minecraft:generic.max_absorption`: `+4.0` (ADDITION)

#### 19. 学识 (Lore)
* **适用装备**：头盔
* **刻印顺序**：
    1. `第 1 步`: 09# Ort (`minecraft:bolt` + `minecraft:copper`)
    2. `第 2 步`: 12# Sol (`minecraft:snout` + `minecraft:gold`)
* **共鸣质变属性**：
    * `irons_spellbooks:spell_power`: `+0.12` (MULTIPLY_BASE, 全技能+1)
    * `irons_spellbooks:lightning_magic_resist`: `+0.30` (MULTIPLY_BASE)
    * `irons_spellbooks:mana_regen`: `+0.15` (MULTIPLY_BASE)

#### 20. 和平 (Peace)
* **适用装备**：胸甲
* **刻印顺序**：
    1. `第 1 步`: 13# Shael (`minecraft:flow` + `minecraft:amethyst`)
    2. `第 2 步`: 10# Thul (`minecraft:coast` + `minecraft:lapis`)
    3. `第 3 步`: 11# Amn (`more_armor_trims:ram` + `minecraft:iron`)
* **共鸣质变属性**：
    * `apothic_attributes:crit_chance`: `+0.15` (ADDITION)
    * `irons_spellbooks:summon_damage`: `+0.25` (MULTIPLY_BASE, 召唤女武神加成)
    * `minecraft:generic.armor`: `+4.0` (ADDITION)

#### 21. 韵律 (Rhyme)
* **适用装备**：盾牌
* **刻印顺序**：
    1. `第 1 步`: 13# Shael (`minecraft:flow` + `minecraft:amethyst`)
    2. `第 2 步`: 05# Eth (`minecraft:flow` + `minecraft:copper`)
* **共鸣质变属性**：
    * `l2weaponry:reflect_time`: `+4.0` (ADDITION, 40% 格挡提速)
    * `irons_spellbooks:spell_resist`: `+0.25` (MULTIPLY_BASE)
    * `minecraft:generic.movement_speed`: `+0.10` (MULTIPLY_BASE, 无法冻结)

#### 22. 权威 (Authority)
* **适用装备**：胸甲
* **刻印顺序**：
    1. `第 1 步`: 15# Hel (`more_armor_trims:origin` + `minecraft:quartz`)
    2. `第 2 步`: 13# Shael (`minecraft:flow` + `minecraft:amethyst`)
    3. `第 3 步`: 08# Ral (`more_armor_trims:fever` + `minecraft:redstone`)
* **共鸣质变属性**：
    * `irons_spellbooks:blood_spell_power`: `+0.25` (MULTIPLY_BASE)
    * `minecraft:generic.armor`: `+5.0` (ADDITION)
    * `irons_spellbooks:fire_magic_resist`: `+0.20` (MULTIPLY_BASE)

#### 23. 白色 (White)
* **适用装备**：法杖 / 细剑
* **刻印顺序**：
    1. `第 1 步`: 14# Dol (`more_armor_trims:wraith` + `minecraft:quartz`)
    2. `第 2 步`: 16# Io (`more_armor_trims:horizon` + `minecraft:amethyst`)
* **共鸣质变属性**：
    * `irons_spellbooks:eldritch_spell_power`: `+0.40` (MULTIPLY_BASE, 毒素与骨骼+3)
    * `irons_spellbooks:cast_time_reduction`: `+0.20` (MULTIPLY_BASE)
    * `irons_spellbooks:max_mana`: `+80.0` (ADDITION)

#### 24. 脾气 (Temper)
* **适用装备**：头盔
* **刻印顺序**：
    1. `第 1 步`: 13# Shael (`minecraft:flow` + `minecraft:amethyst`)
    2. `第 2 步`: 16# Io (`more_armor_trims:horizon` + `minecraft:amethyst`)
    3. `第 3 步`: 08# Ral (`more_armor_trims:fever` + `minecraft:redstone`)
* **共鸣质变属性**：
    * `irons_spellbooks:fire_magic_resist`: `+0.50` (MULTIPLY_BASE)
    * `minecraft:generic.max_health`: `+5.0` (ADDITION)
    * `l2damagetracker:damage_reduction`: `+0.08` (MULTIPLY_BASE)

#### 25. 心灵 (Heart)
* **适用装备**：头盔
* **刻印顺序**：
    1. `第 1 步`: 13# Shael (`minecraft:flow` + `minecraft:amethyst`)
    2. `第 2 步`: 16# Io (`more_armor_trims:horizon` + `minecraft:amethyst`)
    3. `第 3 步`: 10# Thul (`minecraft:coast` + `minecraft:lapis`)
* **共鸣质变属性**：
    * `irons_spellbooks:ice_magic_resist`: `+0.50` (MULTIPLY_BASE)
    * `minecraft:generic.max_health`: `+5.0` (ADDITION)
    * `minecraft:generic.movement_speed`: `+0.10` (MULTIPLY_BASE, 无法冻结)

#### 26. 大地 (Ground)
* **适用装备**：头盔
* **刻印顺序**：
    1. `第 1 步`: 13# Shael (`minecraft:flow` + `minecraft:amethyst`)
    2. `第 2 步`: 16# Io (`more_armor_trims:horizon` + `minecraft:amethyst`)
    3. `第 3 步`: 09# Ort (`minecraft:bolt` + `minecraft:copper`)
* **共鸣质变属性**：
    * `irons_spellbooks:lightning_magic_resist`: `+0.50` (MULTIPLY_BASE)
    * `minecraft:generic.max_health`: `+5.0` (ADDITION)
    * `minecraft:generic.knockback_resistance`: `+0.2` (ADDITION)

#### 27. 治愈 (Cure)
* **适用装备**：头盔
* **刻印顺序**：
    1. `第 1 步`: 13# Shael (`minecraft:flow` + `minecraft:amethyst`)
    2. `第 2 步`: 16# Io (`more_armor_trims:horizon` + `minecraft:amethyst`)
    3. `第 3 步`: 07# Tal (`minecraft:wild` + `minecraft:emerald`)
* **共鸣质变属性**：
    * `apothic_attributes:healing_received`: `+0.35` (MULTIPLY_BASE, 净化光环)
    * `l2damagetracker:regen`: `+0.40` (MULTIPLY_BASE)
    * `minecraft:generic.max_health`: `+5.0` (ADDITION)

#### 28. 壁垒 (Bulwark)
* **适用装备**：头盔
* **刻印顺序**：
    1. `第 1 步`: 13# Shael (`minecraft:flow` + `minecraft:amethyst`)
    2. `第 2 步`: 16# Io (`more_armor_trims:horizon` + `minecraft:amethyst`)
    3. `第 3 步`: 12# Sol (`minecraft:snout` + `minecraft:gold`)
* **共鸣质变属性**：
    * `l2damagetracker:damage_reduction`: `+0.15` (MULTIPLY_BASE, 物理硬免伤15%)
    * `apothic_attributes:life_steal`: `+0.06` (ADDITION)
    * `minecraft:generic.max_health`: `+6.0` (ADDITION)

#### 29. 黑色 (Black)
* **适用装备**：钝器 / 锤 / 链枷
* **刻印顺序**：
    1. `第 1 步`: 10# Thul (`minecraft:coast` + `minecraft:lapis`)
    2. `第 2 步`: 16# Io (`more_armor_trims:horizon` + `minecraft:amethyst`)
    3. `第 3 步`: 04# Nef (`minecraft:shaper` + `minecraft:copper`)
* **共鸣质变属性**：
    * `apothic_attributes:current_hp_damage`: `+0.08` (ADDITION, 40% 压碎打击)
    * `minecraft:generic.attack_speed`: `+0.15` (MULTIPLY_BASE)
    * `minecraft:generic.attack_knockback`: `+1.5` (ADDITION)

#### 30. 烟雾 (Smoke)
* **适用装备**：胸甲
* **刻印顺序**：
    1. `第 1 步`: 04# Nef (`minecraft:shaper` + `minecraft:copper`)
    2. `第 2 步`: 17# Lum (`minecraft:eye` + `minecraft:lapis`)
* **共鸣质变属性**：
    * `irons_spellbooks:spell_resist`: `+0.45` (MULTIPLY_BASE, 全抗+50)
    * `minecraft:generic.armor`: `+6.0` (ADDITION)
    * `apothic_attributes:dodge_chance`: `+0.10` (ADDITION)

#### 31. 灿烂 (Splendor)
* **适用装备**：盾牌
* **刻印顺序**：
    1. `第 1 步`: 05# Eth (`minecraft:flow` + `minecraft:copper`)
    2. `第 2 步`: 17# Lum (`minecraft:eye` + `minecraft:lapis`)
* **共鸣质变属性**：
    * `irons_spellbooks:spell_power`: `+0.10` (MULTIPLY_BASE)
    * `irons_spellbooks:mana_regen`: `+0.30` (MULTIPLY_BASE)
    * `apothic_attributes:experience_gained`: `+0.30` (MULTIPLY_BASE)

#### 32. 回忆 (Memory)
* **适用装备**：法杖
* **刻印顺序**：
    1. `第 1 步`: 17# Lum (`minecraft:eye` + `minecraft:lapis`)
    2. `第 2 步`: 16# Io (`more_armor_trims:horizon` + `minecraft:amethyst`)
    3. `第 3 步`: 12# Sol (`minecraft:snout` + `minecraft:gold`)
    4. `第 4 步`: 05# Eth (`minecraft:flow` + `minecraft:copper`)
* **共鸣质变属性**：
    * `irons_spellbooks:spell_power`: `+0.25` (MULTIPLY_BASE, 法师技能+3)
    * `irons_spellbooks:max_mana`: `+150.0` (ADDITION)
    * `irons_spellbooks:cast_time_reduction`: `+0.30` (MULTIPLY_BASE)

#### 33. 旋律 (Melody)
* **适用装备**：弓 / 弩
* **刻印顺序**：
    1. `第 1 步`: 13# Shael (`minecraft:flow` + `minecraft:amethyst`)
    2. `第 2 步`: 18# Ko (`minecraft:spire` + `minecraft:emerald`)
    3. `第 3 步`: 04# Nef (`minecraft:shaper` + `minecraft:copper`)
* **共鸣质变属性**：
    * `apothic_attributes:arrow_damage`: `+0.35` (MULTIPLY_BASE)
    * `apothic_attributes:draw_speed`: `+0.25` (MULTIPLY_BASE)
    * `apothic_attributes:dodge_chance`: `+0.10` (ADDITION)

#### 34. 歇斯底里 (Hysteria)
* **适用装备**：胸甲
* **刻印顺序**：
    1. `第 1 步`: 13# Shael (`minecraft:flow` + `minecraft:amethyst`)
    2. `第 2 步`: 18# Ko (`minecraft:spire` + `minecraft:emerald`)
    3. `第 3 步`: 02# Eld (`minecraft:raiser` + `minecraft:iron`)
* **共鸣质变属性**：
    * `minecraft:generic.movement_speed`: `+0.35` (MULTIPLY_BASE, 65% 跑速爆发)
    * `minecraft:generic.attack_speed`: `+0.25` (MULTIPLY_BASE)
    * `apothic_attributes:dodge_chance`: `+0.15` (ADDITION)

#### 35. 狂躁 (Mania)
* **适用装备**：近战武器
* **刻印顺序**：
    1. `第 1 步`: 13# Shael (`minecraft:flow` + `minecraft:amethyst`)
    2. `第 2 步`: 18# Ko (`minecraft:spire` + `minecraft:emerald`)
    3. `第 3 步`: 02# Eld (`minecraft:raiser` + `minecraft:iron`)
* **共鸣质变属性**：
    * `minecraft:generic.attack_speed`: `+0.35` (MULTIPLY_BASE, 狂热光环)
    * `minecraft:generic.attack_damage`: `+4.0` (ADDITION)
    * `apothic_attributes:crit_chance`: `+0.10` (ADDITION)

#### 36. 和谐 (Harmony)
* **适用装备**：弓 / 弩
* **刻印顺序**：
    1. `第 1 步`: 03# Tir (`minecraft:wayfinder` + `minecraft:copper`)
    2. `第 2 步`: 06# Ith (`minecraft:rib` + `minecraft:iron`)
    3. `第 3 步`: 12# Sol (`minecraft:snout` + `minecraft:gold`)
    4. `第 4 步`: 18# Ko (`minecraft:spire` + `minecraft:emerald`)
* **共鸣质变属性**：
    * `minecraft:generic.movement_speed`: `+0.30` (MULTIPLY_BASE, 10级活力光环)
    * `apothic_attributes:arrow_damage`: `+0.40` (MULTIPLY_BASE)
    * `apothic_attributes:projectile_damage`: `+0.25` (MULTIPLY_BASE)

#### 37. 狮心 (Lionheart)
* **适用装备**：胸甲
* **刻印顺序**：
    1. `第 1 步`: 15# Hel (`more_armor_trims:origin` + `minecraft:quartz`)
    2. `第 2 步`: 17# Lum (`minecraft:eye` + `minecraft:lapis`)
    3. `第 3 步`: 19# Fal (`more_armor_trims:beast` + `twilightforest:ironwood`)
* **共鸣质变属性**：
    * `minecraft:generic.max_health`: `+10.0` (ADDITION)
    * `minecraft:generic.attack_damage`: `+4.0` (ADDITION)
    * `irons_spellbooks:spell_resist`: `+0.30` (MULTIPLY_BASE)
    * `minecraft:generic.armor`: `+5.0` (ADDITION)

#### 38. 不屈意志 (Unbending Will)
* **适用装备**：重剑 / 双手剑
* **刻印顺序**：
    1. `第 1 步`: 19# Fal (`more_armor_trims:beast` + `twilightforest:ironwood`)
    2. `第 2 步`: 16# Io (`more_armor_trims:horizon` + `minecraft:amethyst`)
    3. `第 3 步`: 06# Ith (`minecraft:rib` + `minecraft:iron`)
    4. `第 4 步`: 02# Eld (`minecraft:raiser` + `minecraft:iron`)
    5. `第 5 步`: 01# El (`minecraft:sentry` + `minecraft:iron`)
    6. `第 6 步`: 15# Hel (`more_armor_trims:origin` + `minecraft:quartz`)
* **共鸣质变属性**：
    * `minecraft:generic.attack_damage`: `+7.0` (ADDITION)
    * `apothic_attributes:life_steal`: `+0.10` (ADDITION)
    * `l2damagetracker:damage_reduction`: `+0.10` (MULTIPLY_BASE)
    * `minecraft:generic.attack_speed`: `+0.25` (MULTIPLY_BASE)

#### 39. 顺从 (Obedience)
* **适用装备**：长柄武器 / 矛
* **刻印顺序**：
    1. `第 1 步`: 15# Hel (`more_armor_trims:origin` + `minecraft:quartz`)
    2. `第 2 步`: 18# Ko (`minecraft:spire` + `minecraft:emerald`)
    3. `第 3 步`: 10# Thul (`minecraft:coast` + `minecraft:lapis`)
    4. `第 4 步`: 05# Eth (`minecraft:flow` + `minecraft:copper`)
    5. `第 5 步`: 19# Fal (`more_armor_trims:beast` + `twilightforest:ironwood`)
* **共鸣质变属性**：
    * `apothic_attributes:current_hp_damage`: `+0.08` (ADDITION, 40% 压碎打击)
    * `irons_spellbooks:fire_spell_power`: `+0.35` (MULTIPLY_BASE, 强化附魔)
    * `apothic_attributes:armor_shred`: `+0.25` (ADDITION)
    * `irons_spellbooks:spell_resist`: `+0.30` (MULTIPLY_BASE)

#### 40. 财富 (Wealth)
* **适用装备**：胸甲
* **刻印顺序**：
    1. `第 1 步`: 20# Lem (`more_armor_trims:greed` + `minecraft:gold`)
    2. `第 2 步`: 18# Ko (`minecraft:spire` + `minecraft:emerald`)
    3. `第 3 步`: 03# Tir (`minecraft:wayfinder` + `minecraft:copper`)
* **共鸣质变属性**：
    * `apothic_attributes:experience_gained`: `+0.80` (MULTIPLY_BASE, 300% 额外打钱/经验)
    * `l2hostility:extra_difficulty`: `+5.0` (ADDITION, 增加刷怪高爆率)

#### 41. 背叛 (Treachery)
* **适用装备**：胸甲
* **刻印顺序**：
    1. `第 1 步`: 13# Shael (`minecraft:flow` + `minecraft:amethyst`)
    2. `第 2 步`: 10# Thul (`minecraft:coast` + `minecraft:lapis`)
    3. `第 3 步`: 20# Lem (`more_armor_trims:greed` + `minecraft:gold`)
* **共鸣质变属性**：
    * `minecraft:generic.attack_speed`: `+0.45` (MULTIPLY_BASE, 45% 极速攻速)
    * `artifacts:generic.invincibility_ticks`: `+12.0` (ADDITION, 能量消解 Fade 无敌帧延长)
    * `l2damagetracker:damage_reduction`: `+0.15` (MULTIPLY_BASE)

#### 42. 理性之声 (Voice of Reason)
* **适用装备**：剑 / 链枷
* **刻印顺序**：
    1. `第 1 步`: 20# Lem (`more_armor_trims:greed` + `minecraft:gold`)
    2. `第 2 步`: 18# Ko (`minecraft:spire` + `minecraft:emerald`)
    3. `第 3 步`: 01# El (`minecraft:sentry` + `minecraft:iron`)
    4. `第 4 步`: 02# Eld (`minecraft:raiser` + `minecraft:iron`)
* **共鸣质变属性**：
    * `irons_spellbooks:ice_spell_power`: `+0.35` (MULTIPLY_BASE)
    * `apothic_attributes:crit_chance`: `+0.15` (ADDITION)
    * `minecraft:generic.movement_speed`: `+0.10` (MULTIPLY_BASE, 无法被冻结)

#### 43. 热情 (Passion)
* **适用装备**：近战武器
* **刻印顺序**：
    1. `第 1 步`: 14# Dol (`more_armor_trims:wraith` + `minecraft:quartz`)
    2. `第 2 步`: 09# Ort (`minecraft:bolt` + `minecraft:copper`)
    3. `第 3 步`: 02# Eld (`minecraft:raiser` + `minecraft:iron`)
    4. `第 4 步`: 20# Lem (`more_armor_trims:greed` + `minecraft:gold`)
* **共鸣质变属性**：
    * `minecraft:generic.attack_speed`: `+0.30` (MULTIPLY_BASE, 白嫖热诚连击)
    * `minecraft:generic.attack_damage`: `+5.0` (ADDITION)
    * `apothic_attributes:crit_chance`: `+0.20` (ADDITION)

#### 44. 执法者 (Lawbringer)
* **适用装备**：剑 / 权杖
* **刻印顺序**：
    1. `第 1 步`: 11# Amn (`more_armor_trims:ram` + `minecraft:iron`)
    2. `第 2 步`: 20# Lem (`more_armor_trims:greed` + `minecraft:gold`)
    3. `第 3 步`: 18# Ko (`minecraft:spire` + `minecraft:emerald`)
* **共鸣质变属性**：
    * `apothic_attributes:armor_shred`: `+0.45` (ADDITION, 衰老诅咒剥夺防御)
    * `irons_spellbooks:holy_spell_power`: `+0.35` (MULTIPLY_BASE, 庇护所光环)
    * `apothic_attributes:life_steal`: `+0.07` (ADDITION)

#### 45. 启蒙 (Enlightenment)
* **适用装备**：胸甲 / 法师袍
* **刻印顺序**：
    1. `第 1 步`: 21# Pul (`minecraft:ward` + `twilightforest:knightmetal`)
    2. `第 2 步`: 08# Ral (`more_armor_trims:fever` + `minecraft:redstone`)
    3. `第 3 步`: 12# Sol (`minecraft:snout` + `minecraft:gold`)
* **共鸣质变属性**：
    * `irons_spellbooks:spell_power`: `+0.20` (MULTIPLY_BASE)
    * `irons_spellbooks:mana_regen`: `+0.40` (MULTIPLY_BASE)
    * `minecraft:generic.armor`: `+5.0` (ADDITION)

#### 46. 智慧 (Wisdom)
* **适用装备**：头盔
* **刻印顺序**：
    1. `第 1 步`: 21# Pul (`minecraft:ward` + `twilightforest:knightmetal`)
    2. `第 2 步`: 06# Ith (`minecraft:rib` + `minecraft:iron`)
    3. `第 3 步`: 02# Eld (`minecraft:raiser` + `minecraft:iron`)
* **共鸣质变属性**：
    * `apothic_attributes:arrow_damage`: `+0.30` (MULTIPLY_BASE, 33% 穿刺攻击)
    * `irons_spellbooks:mana_regen`: `+0.35` (MULTIPLY_BASE)
    * `minecraft:generic.movement_speed`: `+0.10` (MULTIPLY_BASE, 无法被冻结)

#### 47. 石块 (Stone)
* **适用装备**：胸甲
* **刻印顺序**：
    1. `第 1 步`: 13# Shael (`minecraft:flow` + `minecraft:amethyst`)
    2. `第 2 步`: 22# Um (`more_armor_trims:storm` + `twilightforest:steeleaf`)
    3. `第 3 步`: 21# Pul (`minecraft:ward` + `twilightforest:knightmetal`)
    4. `第 4 步`: 17# Lum (`minecraft:eye` + `minecraft:lapis`)
* **共鸣质变属性**：
    * `minecraft:generic.armor`: `+14.0` (ADDITION, 290% 极高护甲加成)
    * `minecraft:generic.armor_toughness`: `+8.0` (ADDITION)
    * `minecraft:generic.knockback_resistance`: `+0.4` (ADDITION)

#### 48. 幽暗 (Gloom)
* **适用装备**：胸甲
* **刻印顺序**：
    1. `第 1 步`: 19# Fal (`more_armor_trims:beast` + `twilightforest:ironwood`)
    2. `第 2 步`: 22# Um (`more_armor_trims:storm` + `twilightforest:steeleaf`)
    3. `第 3 步`: 21# Pul (`minecraft:ward` + `twilightforest:knightmetal`)
* **共鸣质变属性**：
    * `irons_spellbooks:spell_resist`: `+0.45` (MULTIPLY_BASE, 全抗+45)
    * `minecraft:generic.armor`: `+9.0` (ADDITION)
    * `apothic_attributes:dodge_chance`: `+0.15` (ADDITION)

#### 49. 强制 (Duress)
* **适用装备**：胸甲
* **刻印顺序**：
    1. `第 1 步`: 13# Shael (`minecraft:flow` + `minecraft:amethyst`)
    2. `第 2 步`: 22# Um (`more_armor_trims:storm` + `twilightforest:steeleaf`)
    3. `第 3 步`: 10# Thul (`minecraft:coast` + `minecraft:lapis`)
* **共鸣质变属性**：
    * `apothic_attributes:current_hp_damage`: `+0.04` (ADDITION, 15% 压碎打击)
    * `apothic_attributes:crit_damage`: `+0.35` (MULTIPLY_BASE)
    * `apothic_attributes:armor_pierce`: `+4.0` (ADDITION, 33% 撕裂伤口)
    * `minecraft:generic.armor`: `+7.0` (ADDITION)

#### 50. 白骨 (Bone)
* **适用装备**：胸甲
* **刻印顺序**：
    1. `第 1 步`: 12# Sol (`minecraft:snout` + `minecraft:gold`)
    2. `第 2 步`: 22# Um (`more_armor_trims:storm` + `twilightforest:steeleaf`)
    3. `第 3 步`: 22# Um (`more_armor_trims:storm` + `twilightforest:steeleaf`)
* **共鸣质变属性**：
    * `irons_spellbooks:eldritch_spell_power`: `+0.35` (MULTIPLY_BASE, 死灵技能+2)
    * `minecraft:generic.max_absorption`: `+8.0` (ADDITION, 被击中触发骨甲护盾)
    * `irons_spellbooks:max_mana`: `+120.0` (ADDITION)

#### 51. 新月 (Crescent Moon)
* **适用装备**：斧 / 剑 / 长柄武器
* **刻印顺序**：
    1. `第 1 步`: 13# Shael (`minecraft:flow` + `minecraft:amethyst`)
    2. `第 2 步`: 22# Um (`more_armor_trims:storm` + `twilightforest:steeleaf`)
    3. `第 3 步`: 03# Tir (`minecraft:wayfinder` + `minecraft:copper`)
* **共鸣质变属性**：
    * `irons_spellbooks:lightning_spell_power`: `+0.40` (MULTIPLY_BASE, -35% 敌人闪电抗性)
    * `minecraft:generic.attack_speed`: `+0.25` (MULTIPLY_BASE)
    * `apothic_attributes:armor_pierce`: `+5.0` (ADDITION)

#### 52. 雨云 (Rain)
* **适用装备**：胸甲
* **刻印顺序**：
    1. `第 1 步`: 09# Ort (`minecraft:bolt` + `minecraft:copper`)
    2. `第 2 步`: 23# Mal (`minecraft:dune` + `twilightforest:steeleaf`)
    3. `第 3 步`: 06# Ith (`minecraft:rib` + `minecraft:iron`)
* **共鸣质变属性**：
    * `irons_spellbooks:nature_spell_power`: `+0.35` (MULTIPLY_BASE, 德鲁伊技能+2)
    * `irons_spellbooks:max_mana`: `+150.0` (ADDITION)
    * `irons_spellbooks:lightning_magic_resist`: `+0.40` (MULTIPLY_BASE)

#### 53. 审慎 (Prudence)
* **适用装备**：胸甲
* **刻印顺序**：
    1. `第 1 步`: 23# Mal (`minecraft:dune` + `twilightforest:steeleaf`)
    2. `第 2 步`: 03# Tir (`minecraft:wayfinder` + `minecraft:copper`)
* **共鸣质变属性**：
    * `minecraft:generic.armor`: `+8.0` (ADDITION)
    * `irons_spellbooks:spell_resist`: `+0.35` (MULTIPLY_BASE)
    * `l2damagetracker:damage_reduction`: `+0.08` (MULTIPLY_BASE)

#### 54. 圣堂 (Sanctuary)
* **适用装备**：盾牌
* **刻印顺序**：
    1. `第 1 步`: 18# Ko (`minecraft:spire` + `minecraft:emerald`)
    2. `第 2 步`: 18# Ko (`minecraft:spire` + `minecraft:emerald`)
    3. `第 3 步`: 23# Mal (`minecraft:dune` + `twilightforest:steeleaf`)
* **共鸣质变属性**：
    * `irons_spellbooks:spell_resist`: `+0.55` (MULTIPLY_BASE, 全抗+70)
    * `l2weaponry:shield_defense`: `+16.0` (ADDITION)
    * `l2weaponry:reflect_time`: `+3.0` (ADDITION)

#### 55. 毒液 (Venom)
* **适用装备**：武器
* **刻印顺序**：
    1. `第 1 步`: 07# Tal (`minecraft:wild` + `minecraft:emerald`)
    2. `第 2 步`: 14# Dol (`more_armor_trims:wraith` + `minecraft:quartz`)
    3. `第 3 步`: 23# Mal (`minecraft:dune` + `twilightforest:steeleaf`)
* **共鸣质变属性**：
    * `apothic_attributes:armor_shred`: `+0.35` (ADDITION, 毒爆无视防御)
    * `irons_spellbooks:nature_spell_power`: `+0.40` (MULTIPLY_BASE)
    * `apothic_attributes:current_hp_damage`: `+0.05` (ADDITION)

#### 56. 誓约 (Oath)
* **适用装备**：剑 / 斧 / 链枷
* **刻印顺序**：
    1. `第 1 步`: 13# Shael (`minecraft:flow` + `minecraft:amethyst`)
    2. `第 2 步`: 21# Pul (`minecraft:ward` + `twilightforest:knightmetal`)
    3. `第 3 步`: 23# Mal (`minecraft:dune` + `twilightforest:steeleaf`)
    4. `第 4 步`: 17# Lum (`minecraft:eye` + `minecraft:lapis`)
* **共鸣质变属性**：
    * `minecraft:generic.attack_speed`: `+0.45` (MULTIPLY_BASE, 50% 攻速)
    * `minecraft:generic.attack_damage`: `+8.0` (ADDITION)
    * `apothic_attributes:crit_damage`: `+0.45` (MULTIPLY_BASE)

#### 57. 女巫集会 (Coven)
* **适用装备**：头盔
* **刻印顺序**：
    1. `第 1 步`: 24# Ist (`more_armor_trims:witness` + `minecraft:diamond`)
    2. `第 2 步`: 08# Ral (`more_armor_trims:fever` + `minecraft:redstone`)
    3. `第 3 步`: 16# Io (`more_armor_trims:horizon` + `minecraft:amethyst`)
* **共鸣质变属性**：
    * `irons_spellbooks:spell_power`: `+0.20` (MULTIPLY_BASE)
    * `irons_spellbooks:cast_time_reduction`: `+0.25` (MULTIPLY_BASE)
    * `apothic_attributes:experience_gained`: `+0.40` (MULTIPLY_BASE)

#### 58. 谵妄 (Delirium)
* **适用装备**：头盔
* **刻印顺序**：
    1. `第 1 步`: 20# Lem (`more_armor_trims:greed` + `minecraft:gold`)
    2. `第 2 步`: 24# Ist (`more_armor_trims:witness` + `minecraft:diamond`)
    3. `第 3 步`: 16# Io (`more_armor_trims:horizon` + `minecraft:amethyst`)
* **共鸣质变属性**：
    * `irons_spellbooks:spell_power`: `+0.25` (MULTIPLY_BASE, 全技能+2)
    * `apothic_attributes:dodge_chance`: `+0.20` (ADDITION, 迷乱致幻)
    * `apothic_attributes:experience_gained`: `+0.50` (MULTIPLY_BASE)

#### 59. 警惕 (Vigilance)
* **适用装备**：盾牌
* **刻印顺序**：
    1. `第 1 步`: 14# Dol (`more_armor_trims:wraith` + `minecraft:quartz`)
    2. `第 2 步`: 25# Gul (`minecraft:tide` + `minecraft:diamond`)
* **共鸣质变属性**：
    * `minecraft:generic.armor`: `+7.0` (ADDITION)
    * `irons_spellbooks:spell_resist`: `+0.35` (MULTIPLY_BASE)
    * `minecraft:generic.max_health`: `+8.0` (ADDITION)

#### 60. 原则 (Principle)
* **适用装备**：胸甲
* **刻印顺序**：
    1. `第 1 步`: 08# Ral (`more_armor_trims:fever` + `minecraft:redstone`)
    2. `第 2 步`: 25# Gul (`minecraft:tide` + `minecraft:diamond`)
    3. `第 3 步`: 02# Eld (`minecraft:raiser` + `minecraft:iron`)
* **共鸣质变属性**：
    * `irons_spellbooks:holy_spell_power`: `+0.35` (MULTIPLY_BASE, 圣骑士技能+2)
    * `minecraft:generic.max_health`: `+6.0` (ADDITION)
    * `irons_spellbooks:fire_magic_resist`: `+0.35` (MULTIPLY_BASE)

#### 61. 裂隙 (Rift)
* **适用装备**：长柄武器 / 权杖
* **刻印顺序**：
    1. `第 1 步`: 15# Hel (`more_armor_trims:origin` + `minecraft:quartz`)
    2. `第 2 步`: 18# Ko (`minecraft:spire` + `minecraft:emerald`)
    3. `第 3 步`: 20# Lem (`more_armor_trims:greed` + `minecraft:gold`)
    4. `第 4 步`: 25# Gul (`minecraft:tide` + `minecraft:diamond`)
* **共鸣质变属性**：
    * `irons_spellbooks:ice_spell_power`: `+0.35` (MULTIPLY_BASE, 触发冰封球)
    * `irons_spellbooks:fire_spell_power`: `+0.35` (MULTIPLY_BASE)
    * `apothic_attributes:crit_chance`: `+0.20` (ADDITION)

#### 62. 马赛克 (Mosaic)
* **适用装备**：拳刃 / 护手
* **刻印顺序**：
    1. `第 1 步`: 23# Mal (`minecraft:dune` + `twilightforest:steeleaf`)
    2. `第 2 步`: 25# Gul (`minecraft:tide` + `minecraft:diamond`)
    3. `第 3 步`: 11# Amn (`more_armor_trims:ram` + `minecraft:iron`)
* **共鸣质变属性**：
    * `minecraft:generic.attack_speed`: `+0.30` (MULTIPLY_BASE)
    * `irons_spellbooks:ice_spell_power`: `+0.25` (MULTIPLY_BASE)
    * `irons_spellbooks:lightning_spell_power`: `+0.25` (MULTIPLY_BASE)
    * `irons_spellbooks:fire_spell_power`: `+0.25` (MULTIPLY_BASE)
    * `apothic_attributes:life_steal`: `+0.07` (ADDITION)

#### 63. 弑君者 (Kingslayer)
* **适用装备**：剑 / 斧
* **刻印顺序**：
    1. `第 1 步`: 23# Mal (`minecraft:dune` + `twilightforest:steeleaf`)
    2. `第 2 步`: 22# Um (`more_armor_trims:storm` + `twilightforest:steeleaf`)
    3. `第 3 步`: 25# Gul (`minecraft:tide` + `minecraft:diamond`)
    4. `第 4 步`: 19# Fal (`more_armor_trims:beast` + `twilightforest:ironwood`)
* **共鸣质变属性**：
    * `apothic_attributes:current_hp_damage`: `+0.07` (ADDITION, 33% 压碎打击)
    * `apothic_attributes:armor_shred`: `+0.30` (ADDITION)
    * `apothic_attributes:crit_chance`: `+0.20` (ADDITION)
    * `minecraft:generic.attack_speed`: `+0.30` (MULTIPLY_BASE)

#### 64. 寂静 (Silence)
* **适用装备**：武器
* **刻印顺序**：
    1. `第 1 步`: 14# Dol (`more_armor_trims:wraith` + `minecraft:quartz`)
    2. `第 2 步`: 02# Eld (`minecraft:raiser` + `minecraft:iron`)
    3. `第 3 步`: 15# Hel (`more_armor_trims:origin` + `minecraft:quartz`)
    4. `第 4 步`: 24# Ist (`more_armor_trims:witness` + `minecraft:diamond`)
    5. `第 5 步`: 03# Tir (`minecraft:wayfinder` + `minecraft:copper`)
    6. `第 6 步`: 26# Vex (`minecraft:vex` + `twilightforest:fiery`)
* **共鸣质变属性**：
    * `irons_spellbooks:spell_resist`: `+0.65` (MULTIPLY_BASE, 全抗+75)
    * `irons_spellbooks:spell_power`: `+0.20` (MULTIPLY_BASE, 全技能+2)
    * `minecraft:generic.attack_speed`: `+0.25` (MULTIPLY_BASE)
    * `irons_spellbooks:mana_regen`: `+0.45` (MULTIPLY_BASE)

#### 65. 橡树之心 (Heart of the Oak)
* **适用装备**：法杖 / 链枷
* **刻印顺序**：
    1. `第 1 步`: 18# Ko (`minecraft:spire` + `minecraft:emerald`)
    2. `第 2 步`: 26# Vex (`minecraft:vex` + `twilightforest:fiery`)
    3. `第 3 步`: 21# Pul (`minecraft:ward` + `twilightforest:knightmetal`)
    4. `第 4 步`: 10# Thul (`minecraft:coast` + `minecraft:lapis`)
* **共鸣质变属性**：
    * `irons_spellbooks:spell_power`: `+0.35` (MULTIPLY_BASE, 全技能+3)
    * `irons_spellbooks:cooldown_reduction`: `+0.35` (MULTIPLY_BASE, 40% 快速施法)
    * `irons_spellbooks:max_mana`: `+180.0` (ADDITION)
    * `irons_spellbooks:spell_resist`: `+0.40` (MULTIPLY_BASE)

#### 66. 死亡 (Death)
* **适用装备**：剑 / 斧
* **刻印顺序**：
    1. `第 1 步`: 15# Hel (`more_armor_trims:origin` + `minecraft:quartz`)
    2. `第 2 步`: 01# El (`minecraft:sentry` + `minecraft:iron`)
    3. `第 3 步`: 26# Vex (`minecraft:vex` + `twilightforest:fiery`)
    4. `第 4 步`: 09# Ort (`minecraft:bolt` + `minecraft:copper`)
    5. `第 5 步`: 25# Gul (`minecraft:tide` + `minecraft:diamond`)
* **共鸣质变属性**：
    * `apothic_attributes:current_hp_damage`: `+0.10` (ADDITION, 50% 压碎打击)
    * `apothic_attributes:crit_chance`: `+0.35` (ADDITION, 致命一击)
    * `apothic_attributes:crit_damage`: `+0.60` (MULTIPLY_BASE)
    * `minecraft:generic.attack_damage`: `+8.0` (ADDITION)

#### 67. 摇曳焰火 (Flickering Flame)
* **适用装备**：头盔
* **刻印顺序**：
    1. `第 1 步`: 04# Nef (`minecraft:shaper` + `minecraft:copper`)
    2. `第 2 步`: 21# Pul (`minecraft:ward` + `twilightforest:knightmetal`)
    3. `第 3 步`: 26# Vex (`minecraft:vex` + `twilightforest:fiery`)
* **共鸣质变属性**：
    * `irons_spellbooks:fire_spell_power`: `+0.45` (MULTIPLY_BASE, 火焰技能+3)
    * `irons_spellbooks:fire_magic_resist`: `+0.65` (MULTIPLY_BASE, 抗火光环)
    * `irons_spellbooks:mana_cost_reduction`: `0.20` (MULTIPLY_BASE)

#### 68. 仪式 (Ritual)
* **适用装备**：匕首
* **刻印顺序**：
    1. `第 1 步`: 11# Amn (`more_armor_trims:ram` + `minecraft:iron`)
    2. `第 2 步`: 13# Shael (`minecraft:flow` + `minecraft:amethyst`)
    3. `第 3 步`: 27# Ohm (`more_armor_trims:myth` + `cataclysm:ancient_metal`)
* **共鸣质变属性**：
    * `minecraft:generic.attack_speed`: `+0.40` (MULTIPLY_BASE)
    * `minecraft:generic.attack_damage`: `+7.0` (ADDITION)
    * `apothic_attributes:life_steal`: `+0.10` (ADDITION)

#### 69. 流亡 (Exile)
* **适用装备**：盾牌
* **刻印顺序**：
    1. `第 1 步`: 26# Vex (`minecraft:vex` + `twilightforest:fiery`)
    2. `第 2 步`: 27# Ohm (`more_armor_trims:myth` + `cataclysm:ancient_metal`)
    3. `第 3 步`: 24# Ist (`more_armor_trims:witness` + `minecraft:diamond`)
    4. `第 4 步`: 14# Dol (`more_armor_trims:wraith` + `minecraft:quartz`)
* **共鸣质变属性**：
    * `apothic_attributes:life_steal`: `+0.15` (ADDITION, 击中触发生命偷取 Life Tap)
    * `minecraft:generic.armor`: `+12.0` (ADDITION, 反抗光环)
    * `l2weaponry:shield_defense`: `+22.0` (ADDITION)
    * `minecraft:generic.armor_toughness`: `+8.0` (ADDITION)

#### 70. 混沌 (Chaos)
* **适用装备**：拳刃 / 双持武器
* **刻印顺序**：
    1. `第 1 步`: 19# Fal (`more_armor_trims:beast` + `twilightforest:ironwood`)
    2. `第 2 步`: 27# Ohm (`more_armor_trims:myth` + `cataclysm:ancient_metal`)
    3. `第 3 步`: 22# Um (`more_armor_trims:storm` + `twilightforest:steeleaf`)
* **共鸣质变属性**：
    * `minecraft:player.sweeping_damage_ratio`: `+0.60` (ADDITION, 白嫖旋风斩 Whirlwind)
    * `minecraft:generic.attack_speed`: `+0.35` (MULTIPLY_BASE)
    * `apothic_attributes:armor_pierce`: `+7.0` (ADDITION)

#### 71. 战争召唤 (Call to Arms)
* **适用装备**：武器 / 权杖
* **刻印顺序**：
    1. `第 1 步`: 11# Amn (`more_armor_trims:ram` + `minecraft:iron`)
    2. `第 2 步`: 08# Ral (`more_armor_trims:fever` + `minecraft:redstone`)
    3. `第 3 步`: 23# Mal (`minecraft:dune` + `twilightforest:steeleaf`)
    4. `第 4 步`: 24# Ist (`more_armor_trims:witness` + `minecraft:diamond`)
    5. `第 5 步`: 27# Ohm (`more_armor_trims:myth` + `cataclysm:ancient_metal`)
* **共鸣质变属性**：
    * `minecraft:generic.max_health`: `+12.0` (ADDITION, 战斗体质 BO 撑血核心)
    * `irons_spellbooks:max_mana`: `+120.0` (ADDITION)
    * `irons_spellbooks:spell_power`: `+0.15` (MULTIPLY_BASE, 战斗指挥全技能+1)
    * `minecraft:generic.attack_speed`: `+0.30` (MULTIPLY_BASE)

#### 72. 刚毅 (Fortitude - 防具)
* **适用装备**：胸甲
* **刻印顺序**：
    1. `第 1 步`: 01# El (`minecraft:sentry` + `minecraft:iron`)
    2. `第 2 步`: 12# Sol (`minecraft:snout` + `minecraft:gold`)
    3. `第 3 步`: 14# Dol (`more_armor_trims:wraith` + `minecraft:quartz`)
    4. `第 4 步`: 28# Lo (`cataclysm:ignitium` + `minecraft:bolt`)
* **共鸣质变属性**：
    * `minecraft:generic.attack_damage`: `+8.0` (ADDITION, 护甲提供物理增伤)
    * `minecraft:generic.armor`: `+12.0` (ADDITION, 寒冰装甲防御)
    * `minecraft:generic.max_health`: `+15.0` (ADDITION, 等级大幅撑血)
    * `irons_spellbooks:spell_resist`: `+0.35` (MULTIPLY_BASE)

#### 73. 刚毅 (Fortitude - 武器)
* **适用装备**：武器
* **刻印顺序**：
    1. `第 1 步`: 01# El (`minecraft:sentry` + `minecraft:iron`)
    2. `第 2 步`: 12# Sol (`minecraft:snout` + `minecraft:gold`)
    3. `第 3 步`: 14# Dol (`more_armor_trims:wraith` + `minecraft:quartz`)
    4. `第 4 步`: 28# Lo (`cataclysm:ignitium` + `minecraft:bolt`)
* **共鸣质变属性**：
    * `minecraft:generic.attack_damage`: `+14.0` (ADDITION, +300% 增强伤害)
    * `apothic_attributes:crit_chance`: `+0.20` (ADDITION, 致命一击)
    * `irons_spellbooks:spell_resist`: `+0.30` (MULTIPLY_BASE)

#### 74. 悔恨 (Grief)
* **适用装备**：单手剑 / 斧
* **刻印顺序**：
    1. `第 1 步`: 05# Eth (`minecraft:flow` + `minecraft:copper`)
    2. `第 2 步`: 03# Tir (`minecraft:wayfinder` + `minecraft:copper`)
    3. `第 3 步`: 28# Lo (`cataclysm:ignitium` + `minecraft:bolt`)
    4. `第 4 步`: 23# Mal (`minecraft:dune` + `twilightforest:steeleaf`)
    5. `第 5 步`: 08# Ral (`more_armor_trims:fever` + `minecraft:redstone`)
* **共鸣质变属性**：
    * `minecraft:generic.attack_damage`: `+18.0` (ADDITION, 纯点伤之王 +400 点伤折算)
    * `minecraft:generic.attack_speed`: `+0.40` (MULTIPLY_BASE, 40% 攻速)
    * `apothic_attributes:armor_shred`: `+0.40` (ADDITION, 彻底破甲)
    * `apothic_attributes:crit_chance`: `+0.20` (ADDITION)

#### 75. 荆棘 (Bramble)
* **适用装备**：胸甲
* **刻印顺序**：
    1. `第 1 步`: 08# Ral (`more_armor_trims:fever` + `minecraft:redstone`)
    2. `第 2 步`: 27# Ohm (`more_armor_trims:myth` + `cataclysm:ancient_metal`)
    3. `第 3 步`: 29# Sur (`minecraft:silence` + `cataclysm:cursium`)
    4. `第 4 步`: 05# Eth (`minecraft:flow` + `minecraft:copper`)
* **共鸣质变属性**：
    * `irons_spellbooks:nature_spell_power`: `+0.55` (MULTIPLY_BASE, 剧毒法强提升)
    * `l2damagetracker:damage_reduction`: `+0.15` (MULTIPLY_BASE)
    * `irons_spellbooks:mana_regen`: `+0.35` (MULTIPLY_BASE)

#### 76. 狂暴 (Beast)
* **适用装备**：斧 / 锤
* **刻印顺序**：
    1. `第 1 步`: 30# Ber (`alexscaves:polarity` + `cataclysm:witherite`)
    2. `第 2 步`: 03# Tir (`minecraft:wayfinder` + `minecraft:copper`)
    3. `第 3 步`: 22# Um (`more_armor_trims:storm` + `twilightforest:steeleaf`)
    4. `第 4 步`: 23# Mal (`minecraft:dune` + `twilightforest:steeleaf`)
    5. `第 5 步`: 17# Lum (`minecraft:eye` + `minecraft:lapis`)
* **共鸣质变属性**：
    * `minecraft:generic.attack_speed`: `+0.40` (MULTIPLY_BASE, 狂热光环)
    * `minecraft:generic.attack_damage`: `+8.0` (ADDITION)
    * `apothic_attributes:current_hp_damage`: `+0.05` (ADDITION)
    * `minecraft:generic.max_health`: `+12.0` (ADDITION, 变身熊人)

#### 77. 无限 (Infinity)
* **适用装备**：长柄武器 / 矛
* **刻印顺序**：
    1. `第 1 步`: 30# Ber (`alexscaves:polarity` + `cataclysm:witherite`)
    2. `第 2 步`: 23# Mal (`minecraft:dune` + `twilightforest:steeleaf`)
    3. `第 3 步`: 30# Ber (`alexscaves:polarity` + `cataclysm:witherite`)
    4. `第 4 步`: 24# Ist (`more_armor_trims:witness` + `minecraft:diamond`)
* **共鸣质变属性**：
    * `irons_spellbooks:lightning_spell_power`: `+0.60` (MULTIPLY_BASE, 12级信念/审判光环)
    * `apothic_attributes:current_hp_damage`: `+0.08` (ADDITION, 40% 压碎打击)
    * `minecraft:generic.movement_speed`: `+0.25` (MULTIPLY_BASE)
    * `apothic_attributes:armor_pierce`: `+8.0` (ADDITION)

#### 78. 谜团 (Enigma)
* **适用装备**：胸甲
* **刻印顺序**：
    1. `第 1 步`: 31# Jah (`more_armor_trims:nihility` + `l2complements:shulkerate`)
    2. `第 2 步`: 06# Ith (`minecraft:rib` + `minecraft:iron`)
    3. `第 3 步`: 30# Ber (`alexscaves:polarity` + `cataclysm:witherite`)
* **共鸣质变属性**：
    * `minecraft:generic.movement_speed`: `+0.45` (MULTIPLY_BASE, 45% 跑速)
    * `irons_spellbooks:spell_power`: `+0.25` (MULTIPLY_BASE, 全技能+2)
    * `l2damagetracker:damage_reduction`: `+0.08` (MULTIPLY_BASE, 8% 物理硬减伤)
    * `minecraft:generic.max_health`: `+12.0` (ADDITION)
    * `irons_spellbooks:mana_cost_reduction`: `0.15` (MULTIPLY_BASE, 传送位移减耗)
    * `apothic_attributes:experience_gained`: `+0.60` (MULTIPLY_BASE, 极高寻宝增益)
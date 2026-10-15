import os
import json

# =========================================================================
# 1. 基础配置
# =========================================================================
MOD_ID = "examplemod"

OUTPUT_AFFIX_DIR = r"C:\mod\examplemod-template-1.21.1 (2)\src\main\resources\data\apotheosis\affixes\weapons\attribute"
OUTPUT_LANG_DIR = r"C:\mod\examplemod-template-1.21.1 (2)\src\main\resources\assets\examplemod\lang"

# 全部打通！所有主手武器与副手法器共享转化池，支持纯物理大剑配副手法术书魔剑士流派
CATEGORIES = [
    "apotheosis:melee_weapon",
    "apotheosis:bow",
    "apotheosis:trident",
    "irons_apothic:staff",
    "irons_apothic:spellbook"
]

# 单条词缀权重与品质调节（解决池子稀释问题）
# 单条设为 2 ~ 3，91 个词条总权重约 180 ~ 270，刚好占神化掉落池的 30%~40%，平衡感最佳
AFFIX_WEIGHT = 1
AFFIX_QUALITY = 0.12

# 是否启用单装备互斥？
# True  -> 一件装备至多洗出 1 个转化词条（保护其他词条孔位）
# False -> 一件装备允许洗出多个转化词条（支持单件装备实现 物转火 + 火转雷 连环质变）
ENABLE_MUTUAL_EXCLUSIVE = True

# 采用你微调后的精细平衡数值（步长 0.02，最高 0.60）
CONVERSION_VALUES = {
    "apotheosis:common": {
        "min": 0.10,
        "max": 0.20,
        "step": 0.02
    },
    "apotheosis:uncommon": {
        "min": 0.15,
        "max": 0.27,
        "step": 0.02
    },
    "apotheosis:rare": {
        "min": 0.20,
        "max": 0.40,
        "step": 0.02
    },
    "apotheosis:epic": {
        "min": 0.30,
        "max": 0.50,
        "step": 0.02
    },
    "apotheosis:mythic": {
        "min": 0.40,
        "max": 0.60,
        "step": 0.02
    }
}

SCHOOL_NODES = [
    {"id": "physical",  "zh": "物理", "en": "Physical",  "zh_attr": "物质", "en_adj": "Kinetic",   "short": "物"},
    {"id": "fire",      "zh": "炽焰", "en": "Fire",      "zh_attr": "炽炎", "en_adj": "Pyre",      "short": "焰"},
    {"id": "ice",       "zh": "冰霜", "en": "Ice",       "zh_attr": "极寒", "en_adj": "Glacial",   "short": "冰"},
    {"id": "lightning", "zh": "雷霆", "en": "Lightning", "zh_attr": "雷霆", "en_adj": "Storm",     "short": "雷"},
    {"id": "nature",    "zh": "自然", "en": "Nature",    "zh_attr": "生息", "en_adj": "Verdant",   "short": "木"},
    {"id": "spirit",    "zh": "魂灵", "en": "Spirit",    "zh_attr": "幽魂", "en_adj": "Spectral",  "short": "魂"},
    {"id": "blood",     "zh": "猩红", "en": "Blood",     "zh_attr": "猩红", "en_adj": "Sanguine",  "short": "血"},
    {"id": "ender",     "zh": "末影", "en": "Ender",     "zh_attr": "虚空", "en_adj": "Void",      "short": "影"},
    {"id": "eldritch",  "zh": "邪术", "en": "Eldritch",  "zh_attr": "深渊", "en_adj": "Abyssal",   "short": "邪"},
    {"id": "dune",      "zh": "沙丘", "en": "Dune",      "zh_attr": "流沙", "en_adj": "Dune",      "short": "沙"},
    {"id": "evocation", "zh": "唤魔", "en": "Evocation", "zh_attr": "塑灵", "en_adj": "Phantasmal", "short": "魔"},
    {"id": "holy",      "zh": "神圣", "en": "Holy",      "zh_attr": "圣裁", "en_adj": "Hallowed",   "short": "圣"},
    {"id": "melody",    "zh": "旋律", "en": "Melody",    "zh_attr": "谐律", "en_adj": "Harmonic",   "short": "律"},
    {"id": "symmetry",  "zh": "两仪", "en": "Symmetry",  "zh_attr": "两仪", "en_adj": "Mirrored",   "short": "仪"}
]

# =========================================================================
# 2. 生成核心逻辑
# =========================================================================
def generate_affixes():
    os.makedirs(OUTPUT_AFFIX_DIR, exist_ok=True)
    os.makedirs(OUTPUT_LANG_DIR, exist_ok=True)

    zh_cn_entries = {}
    en_us_entries = {}
    count = 0

    print("🚀 开始按全局人物属性架构生成神化词缀 JSON...")

    exclusive_list = [f"{MOD_ID}:damage_conversion"] if ENABLE_MUTUAL_EXCLUSIVE else []

    for i in range(len(SCHOOL_NODES)):
        src = SCHOOL_NODES[i]

        for j in range(i + 1, len(SCHOOL_NODES)):
            dst = SCHOOL_NODES[j]
            affix_name = f"convert_{src['id']}_to_{dst['id']}"
            file_name = f"{affix_name}.json"
            file_path = os.path.join(OUTPUT_AFFIX_DIR, file_name)

            affix_json = {
                "type": "apotheosis:attribute",
                "attribute": f"{MOD_ID}:{affix_name}",
                "categories": CATEGORIES,
                "definition": {
                    "affix_type": "stat",
                    "exclusive_set": exclusive_list,
                    "weights": {
                        "quality": AFFIX_QUALITY,
                        "weight": AFFIX_WEIGHT
                    }
                },
                "operation": "add_value",
                "values": CONVERSION_VALUES
            }

            with open(file_path, "w", encoding="utf-8") as f:
                json.dump(affix_json, f, indent=2, ensure_ascii=False)

            # 本地化键
            affix_id = f"apotheosis:weapons/attribute/{affix_name}"
            prefix_key = f"affix.{affix_id}"
            suffix_key = f"affix.{affix_id}.suffix"
            attr_key = f"attribute.{MOD_ID}.{affix_name}"

            # 中文本地化（适配你调整后的属性描述格式）
            zh_cn_entries[prefix_key] = f"{src['short']}转{dst['zh_attr']}之"
            zh_cn_entries[suffix_key] = f"之{dst['zh_attr']}质变"
            zh_cn_entries[attr_key] = f"{src['zh']}伤害转化为{dst['zh']}伤害"

            # 英文本地化
            en_us_entries[prefix_key] = f"{dst['en_adj']}-Transmuting "
            en_us_entries[suffix_key] = f" of {dst['en']} Conversion"
            en_us_entries[attr_key] = f"{src['en']} Damage converted to {dst['en']}"

            count += 1

    # 输出语言文件补丁
    zh_patch_path = os.path.join(OUTPUT_LANG_DIR, "affix_lang_patch_zh_cn.json")
    en_patch_path = os.path.join(OUTPUT_LANG_DIR, "affix_lang_patch_en_us.json")

    with open(zh_patch_path, "w", encoding="utf-8") as f:
        json.dump(zh_cn_entries, f, indent=2, ensure_ascii=False)

    with open(en_patch_path, "w", encoding="utf-8") as f:
        json.dump(en_us_entries, f, indent=2, ensure_ascii=False)

    print(f"✅ 生成完毕！成功生成 {count} 个神化词缀！")
    print(f"📊 单条权重: {AFFIX_WEIGHT} | 互斥模式: {'开启' if ENABLE_MUTUAL_EXCLUSIVE else '关闭'}")
    print(f"📁 词缀文件输出: {OUTPUT_AFFIX_DIR}")
    print(f"📝 语言包输出: {OUTPUT_LANG_DIR}")

if __name__ == "__main__":
    generate_affixes()
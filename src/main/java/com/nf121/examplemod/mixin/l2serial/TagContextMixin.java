package com.nf121.examplemod.mixin.l2serial;

import dev.xkmc.l2serial.serialization.custom_handler.Handlers;
import dev.xkmc.l2serial.serialization.custom_handler.NBTClassHandler;
import dev.xkmc.l2serial.serialization.generic_types.CodecReg;
import dev.xkmc.l2serial.serialization.generic_types.HolderCodec;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.serialization.unified_processor.TagContext;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;

@Mixin(value = TagContext.class, remap = false)
@SuppressWarnings({"rawtypes", "unchecked", "cast"})
public abstract class TagContextMixin {

    @Unique
    private static final Logger EXAMPLEMOD_LOGGER =
            LogManager.getLogger("examplemod/l2serial");

    /** 复制原 TagContext 里的 private NULL，mixin 访问不到原字段 */
    @Unique
    private static final CompoundTag EXAMPLEMOD_NULL = new CompoundTag();
    static {
        EXAMPLEMOD_NULL.putBoolean("_null", true);
    }

    /**
     * @author examplemod
     * @reason 逐项容错：单个 map 条目解析失败时跳过该条目，而不是清空整个 map
     */
    @Overwrite(remap = false)
    public Object deserializeEfficientMap(Tag tag, TypeInfo key, TypeInfo val, Object def) throws Exception {
        CompoundTag ctag = (CompoundTag) tag;
        Map map = (Map) def;
        map.clear();

        for (String str : ctag.getAllKeys()) {
            try {
                Class<?> kcls = key.getAsClass();
                Object mkey = null;
                if (kcls == String.class) {
                    mkey = str;
                } else if (kcls.isEnum()) {
                    mkey = Enum.valueOf((Class<? extends Enum>) kcls, str);
                } else if (HolderCodec.INS.predicate(kcls)) {
                    mkey = HolderCodec.INS.deserializeValue(
                            (TagContext)(Object)this,
                            StringTag.valueOf(str), key, null);
                } else if (Handlers.CODEC_MAP.containsKey(kcls)) {
                    mkey = ((TagContext)(Object)this).deserializeCodec(
                            Handlers.CODEC_MAP.get(kcls),
                            StringTag.valueOf(str));
                } else if (Handlers.NBT_MAP.containsKey(kcls)) {
                    mkey = ((NBTClassHandler) Handlers.NBT_MAP.get(kcls))
                            .fromTag(StringTag.valueOf(str));
                }

                Tag t = ctag.get(str);
                if (mkey != null) {
                    map.put(mkey, UnifiedCodec.deserializeValue(
                            (TagContext)(Object)this,
                            t == null ? EXAMPLEMOD_NULL : t,
                            val, null));
                }
            } catch (Throwable ex) {
                EXAMPLEMOD_LOGGER.warn(
                        "[l2serial-safe] Skipping invalid map entry '{}': {}",
                        str, ex.toString());
            }
        }

        return map;
    }
}
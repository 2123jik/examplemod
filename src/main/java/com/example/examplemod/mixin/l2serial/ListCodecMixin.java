package com.example.examplemod.mixin.l2serial;

import dev.xkmc.l2serial.serialization.generic_types.ListCodec;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedCodec;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(value = ListCodec.class, remap = false)
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class ListCodecMixin {

    @Unique
    private static final Logger EXAMPLEMOD_LOGGER =
            LogManager.getLogger("examplemod/l2serial");

    /**
     * @author examplemod
     * @reason 逐项容错：单个列表元素解析失败时跳过该元素，而不是清空整个列表
     */
    @Overwrite(remap = false)
    public <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E> Object deserializeValue(
            C ctx, E e, TypeInfo cls, @Nullable Object ans) throws Exception {
        A arr = ctx.castAsList(e);
        TypeInfo com = cls.getGenericType(0);
        int n = ctx.getSize(arr);
        if (ans == null) {
            ans = cls.newInstance();
        }

        List list = (List) ans;
        list.clear();

        for (int i = 0; i < n; ++i) {
            try {
                list.add(UnifiedCodec.deserializeValue(
                        ctx, ctx.getElement(arr, i), com, null));
            } catch (Throwable ex) {
                EXAMPLEMOD_LOGGER.warn(
                        "[l2serial-safe] Skipping invalid list element #{} of {}: {}",
                        i, cls.getAsClass().getSimpleName(), ex.toString());
            }
        }

        return ans;
    }
}
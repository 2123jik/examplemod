package com.example.examplemod.mixin.safety;

import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.BiFunction;

@Mixin(value = DimensionDataStorage.class)
public abstract class MixinDimensionDataStorage {

    @Shadow @Final private static Logger LOGGER;
    @Shadow private File getDataFile(String name) { throw new AssertionError(); }

    /**
     * 精准匹配源码中 readSavedData 的 catch 块：
     * LOGGER.error("Error loading saved data: {}", filename, exception);
     */
    @Inject(
        method = "readSavedData",
        at = @At(
            value = "INVOKE",
            target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
            remap = false
        )
    )
    private <T extends SavedData> void safety$backupBeforeCorruptionLoss(
            BiFunction<?, ?, T> reader,
            DataFixTypes dataFixType,
            String filename,
            CallbackInfoReturnable<T> cir
    ) {
        File file = this.getDataFile(filename);
        if (file.exists() && file.length() > 0) {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File backup = new File(file.getParentFile(), file.getName() + ".corrupted." + timestamp + ".bak");
            File oldFile = new File(file.getParentFile(), file.getName() + "_old");

            try {
                // 1. 生成永久带时间戳的灾难备份文件
                Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                // 2. 补全原版缺失的 .dat_old
                Files.copy(file.toPath(), oldFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                LOGGER.error("################################################################");
                LOGGER.error("[SafetyGuard] 严重数据异常拦截！数据文件 [{}.dat] 解析失败！", filename);
                LOGGER.error("[SafetyGuard] 为防止后续空白数据覆盖丢失，已紧急冷备份至: ");
                LOGGER.error("[SafetyGuard] 灾难备份: {}", backup.getAbsolutePath());
                LOGGER.error("[SafetyGuard] 备用数据: {}", oldFile.getAbsolutePath());
                LOGGER.error("################################################################");
            } catch (IOException e) {
                LOGGER.error("[SafetyGuard] 创建防灾冷备份失败！", e);
            }
        }
    }
}
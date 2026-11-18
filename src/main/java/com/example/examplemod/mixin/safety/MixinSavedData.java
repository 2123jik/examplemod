package com.example.examplemod.mixin.safety;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.saveddata.SavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Mixin(value = SavedData.class)
public abstract class MixinSavedData {

    @Shadow public abstract boolean isDirty();

    /**
     * 在每次保存落盘前（在丢给 IOUtilities 异步写入之前），在主线程强制生成 .dat_old
     */
    @Inject(method = "save(Ljava/io/File;Lnet/minecraft/core/HolderLookup$Provider;)V", at = @At("HEAD"))
    private void safety$syncBackupOldFile(File file, HolderLookup.Provider registries, CallbackInfo ci) {
        // 只有数据真的被修改、且原文件存在时才做备份，不增加无意义的 IO 开销
        if (this.isDirty() && file.exists() && file.length() > 0) {
            File oldFile = new File(file.getParentFile(), file.getName() + "_old");
            try {
                Files.copy(file.toPath(), oldFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // 静默忽略备份错误，不阻塞游戏保存
            }
        }
    }
}
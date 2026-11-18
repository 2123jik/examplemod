package com.example.examplemod.skill.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillIconResolver {

    private static final Map<ResourceLocation, ResourceLocation> CACHE = new ConcurrentHashMap<>();

    private SkillIconResolver() {}

    /**
     * 游戏重载资源包时可调用此方法清空
     */
    public static void clearCache() {
        CACHE.clear();
    }

    public static ResourceLocation resolve(ResourceLocation rawLoc) {
        if (rawLoc == null) return null;
        return CACHE.computeIfAbsent(rawLoc, SkillIconResolver::doResolve);
    }

    private static ResourceLocation doResolve(ResourceLocation loc) {
        String path = loc.getPath();

        // 提取并判断物品注册名 (例如 "item/apple" 或直接 "apple")
        String itemName = path.replace("textures/item/", "").replace("item/", "").replace(".png", "");
        ResourceLocation itemKey = ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), itemName);

        if (BuiltInRegistries.ITEM.containsKey(itemKey)) {
            Item item = BuiltInRegistries.ITEM.get(itemKey);
            if (item != Items.AIR) {
                try {
                    // 获取原版模型粒子图标，准确获取 2D 物品纹理
                    BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(new ItemStack(item), null, null, 0);
                    TextureAtlasSprite sprite = model.getParticleIcon();
                    ResourceLocation sLoc = sprite.contents().name();
                    return ResourceLocation.fromNamespaceAndPath(sLoc.getNamespace(), "textures/" + sLoc.getPath() + ".png");
                } catch (Exception ignored) {}
            }
        }

        // 普通材质路径规范化
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
        }
        if (!path.endsWith(".png")) {
            path = path + ".png";
        }
        return ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), path);
    }
}

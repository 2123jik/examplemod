package com.nf121.examplemod.event;

import com.nf121.examplemod.util.WeaponReplacer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber
public class MobWeaponEventHandler {

    private static final String CHECKED_TAG = "examplemod_weapon_checked";

    /**
     * 黑名单实体标签：允许通过 Datapack 自由补充需要保护的友方/模组生物
     * 路径：data/examplemod/tags/entity_type/protected_allies.json
     */
    public static final TagKey<EntityType<?>> PROTECTED_ALLIES_TAG = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("examplemod", "protected_allies")
    );

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) {
            return;
        }

        if (event.getEntity() instanceof Mob mob) {
            // 1. 防线：拦截所有玩家队伍/友方/傀儡/宠物
            if (isPlayerAllyOrProtected(mob)) {
                return;
            }

            // 2. 防线：避免重复处理
            if (mob.getPersistentData().getBoolean(CHECKED_TAG)) {
                return;
            }

            ItemStack mainHandStack = mob.getItemBySlot(EquipmentSlot.MAINHAND);

            // 3. 防线：如果是可附魔武器，直接赋予幻化模型
            if (WeaponReplacer.isEnchantableWeapon(mainHandStack)) {
                boolean transmogged = WeaponReplacer.tryApplyWeaponAppearance(mainHandStack, mob.getRandom());
                if (transmogged) {
                    // 重新放回主手槽位触发装备同步与客户端渲染刷新
                    mob.setItemSlot(EquipmentSlot.MAINHAND, mainHandStack);
                }
            }

            mob.getPersistentData().putBoolean(CHECKED_TAG, true);
        }
    }

    /**
     * 判断生物是否为玩家同盟、宠物、受保护单位或模块化傀儡等
     */
    private static boolean isPlayerAllyOrProtected(Mob mob) {
        // A. 数据包黑名单保护
        if (mob.getType().is(PROTECTED_ALLIES_TAG)) {
            return true;
        }

        // B. 实现了 OwnableEntity 接口且有主人的生物（涵盖大多数仆从、宠物和模组傀儡）
        if (mob instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            return true;
        }

        // C. 驯服动物（狗、猫、马等）
        if (mob instanceof TamableAnimal tamable && tamable.isTame()) {
            return true;
        }

        // D. 玩家亲手建造的原版铁傀儡
        if (mob instanceof IronGolem ironGolem && ironGolem.isPlayerCreated()) {
            return true;
        }

        // E. 记分板队伍（如果生物加入了任何队伍，通常为玩家阵营或地图机制保护）
        if (mob.getTeam() != null) {
            return true;
        }

        // F. 深度针对：模块化傀儡 (Modular Golems) 与常见仆从模组的兜底拦截
        String entityNamespace = mob.getType().toShortString(); // 如 "modulargolems:metal_golem"
        if (entityNamespace.startsWith("modulargolems:")) {
            return true;
        }

        // G. NBT 中常见的拥有者标记拦截（某些模组未继承 OwnableEntity 但存了 OwnerUUID）
        if (mob.getPersistentData().contains("Owner") || mob.getPersistentData().contains("OwnerUUID")) {
            return true;
        }

        return false;
    }
}
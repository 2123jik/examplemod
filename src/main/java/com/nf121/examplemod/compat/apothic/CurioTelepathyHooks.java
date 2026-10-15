package com.nf121.examplemod.compat.apothic;

import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

public final class CurioTelepathyHooks {

    private CurioTelepathyHooks() {
    }

    public static void blockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof LivingEntity wearer)) {
            return;
        }

        if (WornCurios.affixes(wearer)
                .anyMatch(AffixInstance::enablesTelepathy)) {
            move(event.getDrops(), wearer.position());
        }
    }

    public static void entityDrops(LivingDropsEvent event) {
        Entity direct = event.getSource().getDirectEntity();

        if (direct instanceof Projectile projectile) {
            if (projectile.getOwner() != null
                    && CurioProjectileHooks.affixes(projectile)
                            .anyMatch(AffixInstance::enablesTelepathy)) {
                move(
                        event.getDrops(),
                        projectile.getOwner().position()
                );
            }

            return;
        }

        if (direct instanceof LivingEntity wearer
                && WornCurios.affixes(wearer)
                        .anyMatch(AffixInstance::enablesTelepathy)) {
            move(event.getDrops(), wearer.position());
        }
    }

    private static void move(
            Iterable<ItemEntity> drops,
            Vec3 position
    ) {
        for (ItemEntity item : drops) {
            item.setPos(position.x, position.y, position.z);
            item.setPickUpDelay(0);
        }
    }
}

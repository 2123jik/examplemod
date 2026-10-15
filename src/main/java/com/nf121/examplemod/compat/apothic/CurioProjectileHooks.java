package com.nf121.examplemod.compat.apothic;

import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class CurioProjectileHooks {

    private static final String SOURCES =
            "examplemod.curio_projectile_sources";

    private CurioProjectileHooks() {
    }

    public static void onFired(Projectile projectile) {
        if (projectile.level().isClientSide()
                || projectile.getPersistentData()
                        .getBoolean("apoth.generated")
                || projectile.getPersistentData().contains(SOURCES)
                || !(projectile.getOwner() instanceof LivingEntity owner)) {
            return;
        }

        List<ItemStack> sources = WornCurios.entries(owner).stream()
                .map(entry -> entry.stack().copyWithCount(1))
                .toList();

        ListTag encoded = new ListTag();

        for (ItemStack stack : sources) {
            encoded.add(
                    stack.save(projectile.level().registryAccess())
            );
        }

        /*
         * 在执行可能生成次生箭的回调之前写入标记，
         * 避免同一弹射物被重复初始化。
         */
        projectile.getPersistentData().put(SOURCES, encoded);

        for (ItemStack stack : sources) {
            SocketHelper.getGems(stack)
                    .onProjectileFired(owner, projectile);

            AffixHelper.streamAffixes(stack).forEach(inst ->
                    inst.onProjectileFired(owner, projectile)
            );
        }
    }

    public static List<ItemStack> sources(Projectile projectile) {
        if (!projectile.getPersistentData().contains(
                SOURCES,
                Tag.TAG_LIST
        )) {
            return List.of();
        }

        ListTag encoded = projectile.getPersistentData()
                .getList(SOURCES, Tag.TAG_COMPOUND);

        List<ItemStack> result = new ArrayList<>(encoded.size());

        for (int index = 0; index < encoded.size(); index++) {
            ItemStack stack = ItemStack.parseOptional(
                    projectile.level().registryAccess(),
                    encoded.getCompound(index)
            );

            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }

        return result;
    }

    public static Stream<AffixInstance> affixes(
            Projectile projectile
    ) {
        return sources(projectile).stream()
                .flatMap(AffixHelper::streamAffixes);
    }

    public static void onImpact(
            Projectile projectile,
            HitResult hit
    ) {
        if (projectile.level().isClientSide()) {
            return;
        }

        for (ItemStack stack : sources(projectile)) {
            SocketHelper.getGems(stack).streamValidGems()
                    .forEach(gem ->
                            gem.onProjectileImpact(projectile, hit)
                    );

            AffixHelper.streamAffixes(stack).forEach(inst ->
                    inst.onProjectileImpact(
                            projectile,
                            hit,
                            hit.getType()
                    )
            );
        }
    }
}

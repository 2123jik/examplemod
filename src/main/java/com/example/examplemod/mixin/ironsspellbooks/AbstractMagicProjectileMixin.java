package com.example.examplemod.mixin.ironsspellbooks;

import com.example.examplemod.event.explosion.ISchoolHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile")
public abstract class AbstractMagicProjectileMixin extends Projectile implements ISchoolHolder {

    protected AbstractMagicProjectileMixin(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Unique
    private int shockwave$schoolColor = -1;

    @Override
    public int getSchoolColor() {
        return this.shockwave$schoolColor;
    }

    @Override
    public void setSchoolColor(int color) {
        this.shockwave$schoolColor = color;
    }
}
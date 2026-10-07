package com.example.examplemod.datagen;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.compat.l2hostility.SpellTraitRegistrar;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class SpellTraitItemModels extends ItemModelProvider {

    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath("l2hostility", "item/bg");

    public SpellTraitItemModels(PackOutput output, ExistingFileHelper helper) {
        super(output, ExampleMod.MOD_ID, helper);
    }

    @Override
    protected void registerModels() {
        for (String idStr : SpellTraitRegistrar.WHITELIST) {
            ResourceLocation spellId = ResourceLocation.tryParse(idStr);
            if (spellId == null) continue;

            String itemPath = spellId.getNamespace() + "_" + spellId.getPath() + "_spelltrait";

            ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(
                    spellId.getNamespace(),
                    "gui/spell_icons/" + spellId.getPath()
            );

            getBuilder(itemPath)
                    .parent(new ModelFile.UncheckedModelFile("item/generated"))
                    .texture("layer0", BG)
                    .texture("layer1", icon);
        }
    }
}
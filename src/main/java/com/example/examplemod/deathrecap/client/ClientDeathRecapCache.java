package com.example.examplemod.deathrecap.client;

import com.example.examplemod.deathrecap.DamageEventSnapshot;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class ClientDeathRecapCache {
    public static List<DamageEventSnapshot> LAST_RECORDS = new ArrayList<>();

    public static void setRecords(List<DamageEventSnapshot> records) {
        LAST_RECORDS = new ArrayList<>(records);
    }

    public static void openScreen() {
        if (!LAST_RECORDS.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            // 把当前的 mc.screen 传进去（如果是在游戏里按K，mc.screen 就是 null，关闭时就会正常回到游戏）
            mc.setScreen(new DeathRecapScreen(LAST_RECORDS, mc.screen));
        }
    }
}
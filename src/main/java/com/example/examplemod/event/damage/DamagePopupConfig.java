package com.example.examplemod.event.damage;

import com.example.examplemod.ExampleMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class DamagePopupConfig {

    public static final int BIT_SELF_DEALT = 1 << 0;
    public static final int BIT_SELF_TAKEN = 1 << 1;
    public static final int BIT_OWNED_DEALT = 1 << 2;
    public static final int BIT_ENVIRONMENTAL = 1 << 3;
    public static final int BIT_OTHER_SOURCE_DEALT = 1 << 4;
    public static final int BIT_OTHER_PLAYER_DEALT = 1 << 5;
    public static final int BIT_OTHER_PLAYER_TAKEN = 1 << 6;
    public static final int BIT_MASTER_ENABLE = 1 << 7;

    public static final int DEFAULT_MASK =
            BIT_MASTER_ENABLE | BIT_SELF_DEALT | BIT_SELF_TAKEN | BIT_OWNED_DEALT;

    public static final int DEFAULT_SERVER_MASK =
            BIT_MASTER_ENABLE
                    | BIT_SELF_DEALT
                    | BIT_SELF_TAKEN
                    | BIT_OWNED_DEALT
                    | BIT_ENVIRONMENTAL
                    | BIT_OTHER_SOURCE_DEALT
                    | BIT_OTHER_PLAYER_DEALT
                    | BIT_OTHER_PLAYER_TAKEN;

    public static final ClientConfig CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    public static final ServerConfig SERVER;
    public static final ModConfigSpec SERVER_SPEC;

    static {
        Pair<ClientConfig, ModConfigSpec> clientPair =
                new ModConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT = clientPair.getLeft();
        CLIENT_SPEC = clientPair.getRight();

        Pair<ServerConfig, ModConfigSpec> serverPair =
                new ModConfigSpec.Builder().configure(ServerConfig::new);
        SERVER = serverPair.getLeft();
        SERVER_SPEC = serverPair.getRight();
    }

    private DamagePopupConfig() {}

    @SubscribeEvent
    public static void onConstructMod(FMLConstructModEvent event) {
        event.enqueueWork(() -> ModList.get().getModContainerById(ExampleMod.MODID).ifPresent(container -> {
            container.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
            container.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
        }));
    }

    public static int getClientPopupMask() {
        return CLIENT_SPEC.isLoaded() ? CLIENT.popup.toMask() : DEFAULT_MASK;
    }

    public static int getClientChatMask() {
        return CLIENT_SPEC.isLoaded() ? CLIENT.chat.toMask() : DEFAULT_MASK;
    }

    public static int getServerPopupMask() {
        return SERVER_SPEC.isLoaded() ? SERVER.toMask(SERVER.enablePopup.get()) : DEFAULT_SERVER_MASK;
    }

    public static int getServerChatMask() {
        return SERVER_SPEC.isLoaded() ? SERVER.toMask(SERVER.enableChatReport.get()) : DEFAULT_SERVER_MASK;
    }

    public static boolean matchesMask(
            int mask,
            int viewerId,
            int targetId,
            int attackerId,
            int ownerId,
            boolean targetIsPlayer,
            boolean attackerIsPlayer,
            boolean ownerIsPlayer
    ) {
        if ((mask & BIT_MASTER_ENABLE) == 0) {
            return false;
        }
        // 1. 自己受到的伤害（含自己受到的环境/怪物/玩家伤害）
        if (targetId == viewerId) {
            return (mask & BIT_SELF_TAKEN) != 0;
        }
        // 2. 自己直接造成的伤害（含自己射出的箭/法术）
        if (attackerId == viewerId) {
            return (mask & BIT_SELF_DEALT) != 0;
        }
        // 3. 我的狗、傀儡、召唤物等有归属实体造成的伤害
        if (ownerId == viewerId) {
            return (mask & BIT_OWNED_DEALT) != 0;
        }
        // 4. 其他玩家受到的伤害（若为其他玩家之间的 PvP，满足任一开关即可）
        if (targetIsPlayer) {
            if (ownerIsPlayer || attackerIsPlayer) {
                return (mask & (BIT_OTHER_PLAYER_TAKEN | BIT_OTHER_PLAYER_DEALT)) != 0;
            }
            return (mask & BIT_OTHER_PLAYER_TAKEN) != 0;
        }
        // 5. 其他玩家（及其宠物/傀儡/召唤物）造成的伤害
        if (ownerIsPlayer || attackerIsPlayer) {
            return (mask & BIT_OTHER_PLAYER_DEALT) != 0;
        }
        // 6. 环境（无来源）伤害
        if (attackerId < 0) {
            return (mask & BIT_ENVIRONMENTAL) != 0;
        }
        // 7. 其他有来源的非玩家实体（野生怪物互殴、发射器等）
        return (mask & BIT_OTHER_SOURCE_DEALT) != 0;
    }

    private static ModConfigSpec.BooleanValue defineOption(
            ModConfigSpec.Builder builder,
            String name,
            String comment,
            boolean defaultValue
    ) {
        return builder
                .comment(comment)
                .translation(name)
                .define(name, defaultValue);
    }

    public static final class ClientConfig {
        public final CategoryToggles popup;
        public final CategoryToggles chat;

        ClientConfig(ModConfigSpec.Builder builder) {
            builder.push("3D浮空弹字");
            popup = new CategoryToggles(
                    builder,
                    "是否启用 3D 浮空伤害弹字",
                    true, true, true, false, false, false, false
            );
            builder.pop();

            builder.push("聊天框伤害日志");
            chat = new CategoryToggles(
                    builder,
                    "是否启用聊天框本地伤害日志",
                    true, true, true, false, false, false, false
            );
            builder.pop();
        }
    }

    public static final class CategoryToggles {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.BooleanValue selfDealt;
        public final ModConfigSpec.BooleanValue selfTaken;
        public final ModConfigSpec.BooleanValue ownedDealt;
        public final ModConfigSpec.BooleanValue environmental;
        public final ModConfigSpec.BooleanValue otherSourceDealt;
        public final ModConfigSpec.BooleanValue otherPlayerDealt;
        public final ModConfigSpec.BooleanValue otherPlayerTaken;

        CategoryToggles(
                ModConfigSpec.Builder builder,
                String titleComment,
                boolean defSelfDealt,
                boolean defSelfTaken,
                boolean defOwnedDealt,
                boolean defEnvironmental,
                boolean defOtherSourceDealt,
                boolean defOtherPlayerDealt,
                boolean defOtherPlayerTaken
        ) {
            enabled = defineOption(builder, "总开关", titleComment, true);
            selfDealt = defineOption(builder, "自己造成的伤害", "自己直接攻击、箭矢、法术等造成的伤害", defSelfDealt);
            selfTaken = defineOption(builder, "自己受到的伤害", "自己受到的任意伤害（含怪物、玩家、环境）", defSelfTaken);
            ownedDealt = defineOption(builder, "我的宠物或召唤物造成的伤害", "我的狗、傀儡、召唤物等有归属实体造成的伤害", defOwnedDealt);
            environmental = defineOption(builder, "环境无来源伤害", "岩浆、摔落、中毒、溺水等无来源伤害对其他目标造成的伤害", defEnvironmental);
            otherSourceDealt = defineOption(builder, "其他有来源非玩家伤害", "野生怪物互殴、发射器等其他有来源实体造成的伤害", defOtherSourceDealt);
            otherPlayerDealt = defineOption(builder, "其他玩家造成的伤害", "其他玩家（及其宠物、傀儡、召唤物）造成的伤害", defOtherPlayerDealt);
            otherPlayerTaken = defineOption(builder, "其他玩家受到的伤害", "其他玩家受到的伤害", defOtherPlayerTaken);
        }

        public int toMask() {
            int mask = 0;
            if (enabled.get()) mask |= BIT_MASTER_ENABLE;
            if (selfDealt.get()) mask |= BIT_SELF_DEALT;
            if (selfTaken.get()) mask |= BIT_SELF_TAKEN;
            if (ownedDealt.get()) mask |= BIT_OWNED_DEALT;
            if (environmental.get()) mask |= BIT_ENVIRONMENTAL;
            if (otherSourceDealt.get()) mask |= BIT_OTHER_SOURCE_DEALT;
            if (otherPlayerDealt.get()) mask |= BIT_OTHER_PLAYER_DEALT;
            if (otherPlayerTaken.get()) mask |= BIT_OTHER_PLAYER_TAKEN;
            return mask;
        }
    }

    public static final class ServerConfig {
        public final ModConfigSpec.BooleanValue enablePopup;
        public final ModConfigSpec.BooleanValue enableChatReport;
        public final ModConfigSpec.BooleanValue allowSelfDealt;
        public final ModConfigSpec.BooleanValue allowSelfTaken;
        public final ModConfigSpec.BooleanValue allowOwnedDealt;
        public final ModConfigSpec.BooleanValue allowEnvironmental;
        public final ModConfigSpec.BooleanValue allowOtherSourceDealt;
        public final ModConfigSpec.BooleanValue allowOtherPlayerDealt;
        public final ModConfigSpec.BooleanValue allowOtherPlayerTaken;

        ServerConfig(ModConfigSpec.Builder builder) {
            builder.push("服务端伤害同步总控");
            enablePopup = defineOption(builder, "允许服务端下发3D弹字", "服务端总开关：是否允许向客户端下发 3D 伤害弹字数据", true);
            enableChatReport = defineOption(builder, "允许服务端下发聊天日志", "服务端总开关：是否允许客户端接收用于本地聊天日志的伤害数据", true);
            allowSelfDealt = defineOption(builder, "允许同步_自己造成的伤害", "允许服务端同步玩家自己造成的伤害", true);
            allowSelfTaken = defineOption(builder, "允许同步_自己受到的伤害", "允许服务端同步玩家自己受到的伤害", true);
            allowOwnedDealt = defineOption(builder, "允许同步_宠物或召唤物伤害", "允许服务端同步玩家的宠物、傀儡、召唤物造成的伤害", true);
            allowEnvironmental = defineOption(builder, "允许同步_环境无来源伤害", "允许服务端同步环境（无来源）伤害", true);
            allowOtherSourceDealt = defineOption(builder, "允许同步_其他有来源非玩家伤害", "允许服务端同步其他有来源非玩家实体造成的伤害", true);
            allowOtherPlayerDealt = defineOption(builder, "允许同步_其他玩家造成的伤害", "允许服务端同步其他玩家造成的伤害", true);
            allowOtherPlayerTaken = defineOption(builder, "允许同步_其他玩家受到的伤害", "允许服务端同步其他玩家受到的伤害", true);
            builder.pop();
        }

        public int toMask(boolean masterEnabled) {
            if (!masterEnabled) {
                return 0;
            }
            int mask = BIT_MASTER_ENABLE;
            if (allowSelfDealt.get()) mask |= BIT_SELF_DEALT;
            if (allowSelfTaken.get()) mask |= BIT_SELF_TAKEN;
            if (allowOwnedDealt.get()) mask |= BIT_OWNED_DEALT;
            if (allowEnvironmental.get()) mask |= BIT_ENVIRONMENTAL;
            if (allowOtherSourceDealt.get()) mask |= BIT_OTHER_SOURCE_DEALT;
            if (allowOtherPlayerDealt.get()) mask |= BIT_OTHER_PLAYER_DEALT;
            if (allowOtherPlayerTaken.get()) mask |= BIT_OTHER_PLAYER_TAKEN;
            return mask;
        }
    }
}
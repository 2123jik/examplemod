package com.example.examplemod.entity.attachments;

public class ClientAuraData {
    private static float reservedMana = 0.0F;

    public static float getReservedMana() {
        return reservedMana;
    }

    public static void setReservedMana(float value) {
        reservedMana = Math.max(0.0F, value);
    }

    public static void reset() {
        reservedMana = 0.0F;
    }
}
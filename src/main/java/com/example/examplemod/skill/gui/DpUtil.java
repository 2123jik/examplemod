package com.example.examplemod.skill.gui;

import icyllis.modernui.core.Context;

public final class DpUtil {
    public static int dp(float dp, Context context) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
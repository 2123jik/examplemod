package com.example.examplemod.skill.gui;

import icyllis.modernui.text.Editable;
import icyllis.modernui.text.TextWatcher;

import java.util.function.Consumer;

/**
 * 封装 TextWatcher，允许只关心 afterTextChanged，避免每次都要重写 3 个方法
 */
public class SimpleTextWatcher implements TextWatcher {

    private final Consumer<Editable> onAfterChanged;

    public SimpleTextWatcher(Consumer<Editable> onAfterChanged) {
        this.onAfterChanged = onAfterChanged;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // 空实现
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // 空实现
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (onAfterChanged != null) {
            onAfterChanged.accept(s);
        }
    }
}
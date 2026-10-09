package com.example.examplemod.skill;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class SkillHistoryManager {
    private static final int MAX_HISTORY = 40;
    private final Deque<List<SkillNodeData>> undoStack = new ArrayDeque<>();
    private final Deque<List<SkillNodeData>> redoStack = new ArrayDeque<>();

    public void pushSnapshot() {
        List<SkillNodeData> snapshot = new ArrayList<>();
        for (SkillNode node : SkillRegistry.NODES.values()) {
            snapshot.add(SkillNodeData.fromNode(node));
        }
        undoStack.push(snapshot);
        if (undoStack.size() > MAX_HISTORY) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;

        List<SkillNodeData> current = new ArrayList<>();
        for (SkillNode node : SkillRegistry.NODES.values()) {
            current.add(SkillNodeData.fromNode(node));
        }
        redoStack.push(current);

        List<SkillNodeData> prev = undoStack.pop();
        restore(prev);
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) return false;

        List<SkillNodeData> current = new ArrayList<>();
        for (SkillNode node : SkillRegistry.NODES.values()) {
            current.add(SkillNodeData.fromNode(node));
        }
        undoStack.push(current);

        List<SkillNodeData> next = redoStack.pop();
        restore(next);
        return true;
    }

    private void restore(List<SkillNodeData> snapshot) {
        SkillRegistry.clear();
        for (SkillNodeData data : snapshot) {
            SkillRegistry.register(SkillNodeData.toNode(data));
        }
    }
}
package com.example.examplemod.skill;

import java.util.*;

public final class SkillLayoutEngine {

    /**
     * 对所有注册的技能节点计算拓扑坐标
     */
    public static void computeLayout(Collection<SkillNode> nodes) {
        if (nodes.isEmpty()) return;

        Map<String, SkillNode> nodeMap = new HashMap<>();
        for (SkillNode n : nodes) {
            nodeMap.put(n.getId(), n);
        }

        // 1. 计算每个节点的深度/层级（BFS 从根节点开始）
        Map<String, Integer> depths = new HashMap<>();
        Queue<SkillNode> queue = new ArrayDeque<>();

        for (SkillNode n : nodes) {
            if (n.getParents().isEmpty()) {
                depths.put(n.getId(), 0);
                queue.add(n);
            }
        }

        // 孤立循环安全防护：若没有显式无父节点的根，选第一个作为根
        if (queue.isEmpty()) {
            SkillNode fallbackRoot = nodes.iterator().next();
            depths.put(fallbackRoot.getId(), 0);
            queue.add(fallbackRoot);
        }

        // 收集所有子节点反向依赖表
        Map<String, List<String>> childrenMap = new HashMap<>();
        for (SkillNode n : nodes) {
            for (String p : n.getParents()) {
                childrenMap.computeIfAbsent(p, k -> new ArrayList<>()).add(n.getId());
            }
        }

        int maxDepthLimit = nodes.size();
        while (!queue.isEmpty()) {
            SkillNode curr = queue.poll();
            int curDepth = depths.get(curr.getId());
            if (curDepth >= maxDepthLimit) continue; // 环检测截断

            List<String> children = childrenMap.getOrDefault(curr.getId(), Collections.emptyList());
            for (String childId : children) {
                if (!depths.containsKey(childId) || depths.get(childId) < curDepth + 1) {
                    depths.put(childId, curDepth + 1);
                    SkillNode cNode = nodeMap.get(childId);
                    if (cNode != null) queue.add(cNode);
                }
            }
        }


        // 2. 确定性初始位置赋值：按层级在同心圆上均匀散开（固定随机种子保证跨端一致）
        Random rng = new Random(0x5EED_C0DE_1234L);
        Map<Integer, List<SkillNode>> depthGroups = new TreeMap<>();
        for (SkillNode n : nodes) {
            depthGroups.computeIfAbsent(depths.getOrDefault(n.getId(), 0), k -> new ArrayList<>()).add(n);
        }

        for (var entry : depthGroups.entrySet()) {
            int depth = entry.getKey();
            List<SkillNode> group = entry.getValue();
            if (depth == 0) {
                // 根节点固定在星图中心原点
                for (SkillNode root : group) {
                    root.setPosition(0, 0);
                }
                continue;
            }

            float baseRadius = depth * 110f; // 每级深度展开的基础半径
            float angleStep = (float) (2 * Math.PI / group.size());
            float angleOffset = rng.nextFloat() * 3.14f;

            for (int i = 0; i < group.size(); i++) {
                float angle = angleOffset + i * angleStep;
                float dist = baseRadius + (rng.nextFloat() - 0.5f) * 30f;
                group.get(i).setPosition(
                        (float) (Math.cos(angle) * dist),
                        (float) (Math.sin(angle) * dist)
                );
            }
        }

        // 3. 力导向松弛迭代（排斥力防重叠 + 连线弹簧力聚拢 + 根节点锚定）
        int iterations = 100;
        float idealSpringLength = 90f; // 连线理想间距
        float kRepel = 7500f;          // 节点间排斥力系数
        float kSpring = 0.05f;         // 连线弹力系数

        List<SkillNode> nodeList = new ArrayList<>(nodes);
        for (int step = 0; step < iterations; step++) {
            float tempFactor = 1.0f - (float) step / iterations; // 退火降温因子

            for (int i = 0; i < nodeList.size(); i++) {
                SkillNode a = nodeList.get(i);
                if (a.getParents().isEmpty()) continue; // 根节点固定在原点不动

                float fx = 0, fy = 0;

                // A. 所有节点之间的相互排斥力（库仑斥力）
                for (int j = 0; j < nodeList.size(); j++) {
                    if (i == j) continue;
                    SkillNode b = nodeList.get(j);
                    float dx = a.getX() - b.getX();
                    float dy = a.getY() - b.getY();
                    float distSq = dx * dx + dy * dy;
                    if (distSq < 1e-3f) {
                        dx = (rng.nextFloat() - 0.5f) * 5f;
                        dy = (rng.nextFloat() - 0.5f) * 5f;
                        distSq = dx * dx + dy * dy;
                    }
                    float dist = (float) Math.sqrt(distSq);
                    float force = kRepel / (distSq + 100f);
                    fx += (dx / dist) * force;
                    fy += (dy / dist) * force;
                }

                // B. 父子连接线之间的吸引弹力（胡克弹力）
                for (String pId : a.getParents()) {
                    SkillNode p = nodeMap.get(pId);
                    if (p != null) {
                        float dx = p.getX() - a.getX();
                        float dy = p.getY() - a.getY();
                        float dist = (float) Math.hypot(dx, dy);
                        float delta = dist - idealSpringLength;
                        fx += (dx / Math.max(1f, dist)) * delta * kSpring;
                        fy += (dy / Math.max(1f, dist)) * delta * kSpring;
                    }
                }

                // C. 限制最大单步位移并应用
                float maxStep = 18f * tempFactor;
                float moveLen = (float) Math.hypot(fx, fy);
                if (moveLen > maxStep) {
                    fx = (fx / moveLen) * maxStep;
                    fy = (fy / moveLen) * maxStep;
                }
                a.setPosition(a.getX() + fx, a.getY() + fy);
            }
        }
    }
}
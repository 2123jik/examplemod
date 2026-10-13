#version 150

uniform sampler2D Sampler0; // 斩痕主纹理/形状贴图
uniform sampler2D Sampler1; // 噪声贴图 (Noise)

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    float progress = 1.0 - vertexColor.a; // [0.0 出刀 -> 1.0 闭合]

    // 坐标中心化 [-1.0, 1.0]
    vec2 p = uv * 2.0 - 1.0;

    // 1. 几何刀光修型：两端自适应收尖（即使是长方形 Quad，也能裁剪出月牙/裂缝形态）
    float tipFade = 1.0 - smoothstep(0.7, 1.0, abs(p.x));
    float distToCenter = abs(p.y);

    // 2. 噪声撕裂（利用 Sampler1 产生微小空间抖动，而不是大面积撕烂）
    float noise = texture(Sampler1, vec2(uv.x * 2.0 - progress * 0.5, uv.y)).r;
    float jitter = (noise - 0.5) * 0.15 * (1.0 - progress);
    distToCenter += jitter;

    // 3. 裂口厚度动态演化：第1帧暴胀，随后两端向中心闭合缝合
    float maxThickness = 0.5 * tipFade * (1.0 - pow(progress, 1.5));
    if (distToCenter > maxThickness || tipFade <= 0.01) {
        discard;
    }

    // 归一化局部距离 [0.0 刀刃中心 -> 1.0 边缘]
    float normDist = distToCenter / maxThickness;

    // 4. 双向色散偏移采样主纹理（纵向撕扯效果）
    float disperseOffset = 0.04 * (1.0 - progress);
    float redEdge  = texture(Sampler0, uv + vec2(0.0, disperseOffset)).r;
    float blueEdge = texture(Sampler0, uv - vec2(0.0, disperseOffset)).b;

    // 5. 颜色分层构建：
    // [核心]：极致的虚空深黑（带有极微弱的宇宙深紫），切开画面的精髓
    vec3 colVoid = vec3(0.02, 0.0, 0.05);

    // [边缘色散与过载辉光]：
    vec3 colCyan    = vec3(0.0, 0.9, 1.0);
    vec3 colMagenta = vec3(1.0, 0.1, 0.6);
    vec3 colCore    = vec3(1.0, 1.0, 1.0); // 事件视界纯白刃口

    // 构造纯白刃线（处于缝隙最边缘）
    float whiteEdge = smoothstep(0.7, 0.98, normDist);
    // 构造内外渐变
    float innerGlow = smoothstep(0.0, 0.6, normDist);

    vec3 finalRgb = colVoid;
    // 叠加色散与虚空边框
    finalRgb = mix(finalRgb, colMagenta, redEdge * innerGlow);
    finalRgb = mix(finalRgb, colCyan, blueEdge * innerGlow);
    // 缝合最外层纯白光刃
    finalRgb += colCore * whiteEdge * 2.5;

    // 6. 最终 Alpha 控制（两端锐利衰减，边缘软羽化）
    float edgeAlpha = 1.0 - smoothstep(0.85, 1.0, normDist);
    float finalAlpha = edgeAlpha * vertexColor.a;

    fragColor = vec4(finalRgb, finalAlpha);
}
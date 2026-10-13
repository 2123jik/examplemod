#version 150

uniform sampler2D Sampler0;
uniform float TierTime;
uniform float PulseStrength;

in vec4 vertexColor;
in vec3 skyDirection;

out vec4 fragColor;

void main() {
    // 对插值后的世界方向重新归一化。
    // 世界 +Y 始终朝上，不随相机俯仰改变。
    vec3 direction = normalize(skyDirection);

    // 在固定的世界方向上采样，仅通过时间产生缓慢漂移。
    vec2 drift = vec2(
        TierTime * 0.003,
        TierTime * 0.002
    );

    vec2 baseUv = direction.xz * 0.35 + drift;

    // 使用 G、B 通道轻微扰动采样位置。
    vec2 distortion =
        (texture(Sampler0, baseUv * 0.5).gb - vec2(0.5))
        * 0.08;

    vec2 finalUv = baseUv + distortion;

    // R：主体云层；B：星团高亮。
    vec4 noiseSample = texture(Sampler0, finalUv);

    float cloudNoise = noiseSample.r;
    float starClusters = noiseSample.b;

    float pulse = sin(TierTime * 0.8) * 0.5 + 0.5;

    // 依赖世界高度方向，保持地平线固定在世界水平面。
    float skyDomeWeight =
        smoothstep(-0.05, 0.35, direction.y);

    vec3 color = vertexColor.rgb;

    float cloudDelta =
        (cloudNoise - 0.5)
        * (0.18 + PulseStrength * 0.30);

    color += color * cloudDelta * skyDomeWeight;

    float starGlow =
        pow(starClusters, 2.5)
        * (PulseStrength * 0.45);

    color += vec3(starGlow) * skyDomeWeight;

    color +=
        color
        * pulse
        * (0.05 + PulseStrength * 0.10);

    fragColor = vec4(color, vertexColor.a);
}

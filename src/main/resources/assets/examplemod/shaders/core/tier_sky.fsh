#version 150

uniform float TierTime;
uniform float PulseStrength;

in vec4 vertexColor;
in vec3 skyDirection;

out vec4 fragColor;

float hash(vec2 value) {
    return fract(
        sin(dot(value, vec2(127.1, 311.7)))
        * 43758.5453123
    );
}

float noise(vec2 value) {
    vec2 integerPart = floor(value);
    vec2 fractionalPart = fract(value);

    fractionalPart =
        fractionalPart
        * fractionalPart
        * (3.0 - 2.0 * fractionalPart);

    float a = hash(integerPart);
    float b = hash(integerPart + vec2(1.0, 0.0));
    float c = hash(integerPart + vec2(0.0, 1.0));
    float d = hash(integerPart + vec2(1.0, 1.0));

    return mix(
        mix(a, b, fractionalPart.x),
        mix(c, d, fractionalPart.x),
        fractionalPart.y
    );
}

void main() {
    vec3 direction = normalize(skyDirection);

    /*
     * 使用球面方向生成噪声。
     * 这里没有顶盖，因此不会在抬头时出现圆形边界。
     */
    vec2 movingCoordinates =
        direction.xz * 4.0
        + vec2(
            TierTime * 0.005,
            TierTime * 0.003
        );

    float cloudNoise = noise(movingCoordinates);

    float pulse =
        sin(TierTime * 0.65) * 0.5 + 0.5;

    vec3 color = vertexColor.rgb;

    color +=
        color
        * cloudNoise
        * 0.07
        * PulseStrength;

    color +=
        color
        * pulse
        * 0.12
        * PulseStrength;

    fragColor = vec4(
        color,
        vertexColor.a
    );
}

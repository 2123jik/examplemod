#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float TierBrightness;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 textureColor = texture(Sampler0, texCoord);

    if (textureColor.a < 0.015) {
        discard;
    }

    vec4 color =
        textureColor
        * vertexColor
        * ColorModulator;

    color.rgb *= TierBrightness;

    fragColor = color;
}

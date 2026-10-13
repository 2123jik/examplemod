#version 150

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec3 skyDirection;

void main() {
    vertexColor = Color;

    // Java 提交未经视图变换的球面顶点。
    // 这里保留世界方向，在片元阶段归一化。
    skyDirection = Position;

    // 视角旋转只用于决定顶点在屏幕上的位置。
    // Java 已去除该矩阵的平移，使天空始终以相机为中心。
    gl_Position =
        ProjMat
        * ModelViewMat
        * vec4(Position, 1.0);
}

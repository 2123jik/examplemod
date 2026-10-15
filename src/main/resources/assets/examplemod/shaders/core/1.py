from PIL import Image

def generate_blade_slash(filename="critical_hit.png", size=32):
    # 透明底
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    pixels = img.load()

    # 水平方向一条干净利落的刀刃（在粒子代码里去决定它的倾斜和斩切）
    # 刀身长度：26 像素（从 x=3 到 x=28）
    y_center = size // 2

    for x in range(3, 29):
        # 离中心比例 [0.0 ~ 1.0]
        dist_x = abs(x - 15.5) / 12.5

        # 核心厚度：两头尖 (0)，中间宽 (1~2)
        if dist_x > 0.8:
            half_h = 0  # 针尖：只有 1 像素高
        elif dist_x > 0.4:
            half_h = 1  # 刀身：3 像素高
        else:
            half_h = 1  # 刀刃核心

        # 纵向绘制刀身
        for dy in range(-half_h, half_h + 1):
            y = y_center + dy
            if dy == 0:
                # 刀锋中轴线：纯白亮刃，100% 不透明
                pixels[x, y] = (255, 255, 255, 255)
            else:
                # 刃面微反光：极窄的暖白，绝不向外晕开成一坨
                pixels[x, y] = (255, 240, 200, 210)

    img.save(filename, "PNG")
    print(f"纯粹刀刃贴图生成完毕: {filename}")

if __name__ == "__main__":
    generate_blade_slash()
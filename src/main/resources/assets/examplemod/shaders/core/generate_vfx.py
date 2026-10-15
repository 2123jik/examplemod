import math
import random
from PIL import Image

def generate_textures():
    # ================= 1. 打包贴图 space_slash_pack.png (256x64) =================
    # R 通道: 法线 X (左右撕扯力)
    # G 通道: 法线 Y (切线上下剪切错位)
    # B 通道: 核心极锐利刃芒遮罩
    # A 通道: 纵向两端针尖包络遮罩
    width, height = 256, 64
    pack_img = Image.new("RGBA", (width, height))
    pack_pixels = pack_img.load()

    for x in range(width):
        # 纵向包络 (x 两端收拢为尖针)
        nx = (x / (width - 1)) * 2.0 - 1.0 # [-1, 1]
        envelope = max(0.0, 1.0 - abs(nx)) ** 1.8

        for y in range(height):
            ny = (y / (height - 1)) * 2.0 - 1.0 # [-1, 1], 0 为中心斩线
            dist_to_slit = abs(ny)

            # 法线 X 与 Y（带切向剪切错位）
            norm_x = int((math.sin(nx * 3.14) * 0.5 + 0.5) * 255)
            # 切线上半部向上拉(255)，下半部向下拉(0)，形成物理空间撕裂剪切
            shear = 0.5 + math.copysign(0.45 * (1.0 - dist_to_slit), -ny)
            norm_y = int(max(0.0, min(1.0, shear)) * 255)

            # 核心极锐利刃线 (高斯极窄中轴)
            core_sharp = math.exp(-pow(dist_to_slit * 5.5, 2.0))
            core_b = int(core_sharp * 255)

            # 纵横双向衰减 Alpha
            alpha_val = int(envelope * max(0.0, 1.0 - pow(dist_to_slit, 1.5)) * 255)

            pack_pixels[x, y] = (norm_x, norm_y, core_b, alpha_val)

    pack_img.save("space_slash_pack.png")
    print("已生成: space_slash_pack.png")

    # ================= 2. 沃罗诺伊碎裂噪声 space_slash_noise.png (256x256) =================
    # 用于产生碎玻璃般的空间咬合碎屑，以及收刀时的碎片崩解 (Dissolve)
    n_size = 256
    noise_img = Image.new("RGBA", (n_size, n_size))
    noise_pixels = noise_img.load()

    # 随机生成 Voronoi 特征点 (无缝平铺)
    num_cells = 36
    points = [(random.random() * n_size, random.random() * n_size) for _ in range(num_cells)]

    for x in range(n_size):
        for y in range(n_size):
            min_dist = 9999.0
            second_dist = 9999.0
            for px, py in points:
                # 周期性环绕计算无缝距离
                dx = abs(x - px)
                dx = min(dx, n_size - dx)
                dy = abs(y - py)
                dy = min(dy, n_size - dy)
                dist = math.sqrt(dx*dx + dy*dy)

                if dist < min_dist:
                    second_dist = min_dist
                    min_dist = dist
                elif dist < second_dist:
                    second_dist = dist

            # F2 - F1 细胞裂纹边缘
            crack = (second_dist - min_dist) / (n_size / math.sqrt(num_cells))
            val = int(max(0.0, min(1.0, crack)) * 255)
            # R: 基础噪声高度, G: 扰动偏移, B: 边缘裂痕
            noise_pixels[x, y] = (val, int(min_dist * 4) % 256, val, 255)

    noise_img.save("space_slash_noise.png")
    print("已生成: space_slash_noise.png")

if __name__ == "__main__":
    generate_textures()
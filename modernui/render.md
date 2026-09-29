这份文档是专门为 **Modern UI (NeoForge 1.21+)** 提炼的 **开发者速查与提示词参考规范（Cheatsheet / Prompt Guide）**。你可以将它直接保存为 `ModernUI_Dev_Guide.md`，后续需要让 AI 编写 Modern UI 相关渲染/界面时，直接把这篇 Markdown 发送给 AI 即可防止其产生幻觉。

---

# Modern UI (Minecraft 1.21+ / NeoForge) 开发与渲染规范速查

> **文档用途**：指导基于 Modern UI 核心架构进行 Minecraft 客户端 UI、HUD、容器界面及图形渲染开发。禁止幻想不存在的原版 UI 绑定方式，严格按照 Modern UI 的 Android 视图体系与渲染桥接模式进行编写。

---

## 一、 核心架构概念

Modern UI **完全搬照了 Android View 布局体系与 Skia/Arc3D 图形渲染管线**，但桥接在 Minecraft 客户端中：
1. **视图树体系**：继承或直接使用 `icyllis.modernui.view.View`、`ViewGroup`、`LinearLayout`、`FrameLayout`、`TextView`、`ProgressBar`、`EditText` 等类（注意包名是 `icyllis.modernui.*`，不是 `android.*`）。
2. **生命周期/页面承载**：使用 `icyllis.modernui.fragment.app.Fragment`，通过 `SimpleScreen` 或 `MenuScreen` 承载。
3. **坐标与像素单位**：使用设备无关像素（dp）。可以使用 View 自带的 `view.dp(数值)` 进行转换（例如 `view.dp(16)`）。
4. **与原版 GuiGraphics 桥接**：
    - 增强型绘制：使用 `icyllis.modernui.mc.ExtendedGuiGraphics`，支持无缝绘制圆角矩形、渐变、环形/圆形等，避免自己手写 Shader 和复杂的顶点计算。
    - 3D/原生物品混编：在 Modern UI View 中嵌入 Minecraft 原生模型/物品/实体时，使用 `MinecraftSurfaceView`。

---

## 二、 关键类与 API 索引

| 类名与完整限定名 | 作用说明与典型用法 |
| :--- | :--- |
| `icyllis.modernui.mc.MuiModApi` | Modern UI 门面 API。获取时间、打开屏幕 `openScreen(Fragment)`、计算 GUI 缩放等。 |
| `icyllis.modernui.mc.ExtendedGuiGraphics` | **原生渲染增强工具**。包装原版 `GuiGraphics`，提供圆角、描边、抗锯齿渐变矩形绘制。 |
| `icyllis.modernui.mc.MinecraftSurfaceView` | **嵌套原生元素 View**。在其内部的 `Renderer.onDraw(GuiGraphics, ...)` 中可调用原版物品/方块/实体渲染。 |
| `icyllis.modernui.mc.BlurHandler` | 负责高斯模糊效果、暗色背景计算，单例为 `BlurHandler.INSTANCE`。 |
| `icyllis.modernui.mc.ImageStore` | 材质与图片缓存池，单例为 `ImageStore.getInstance()`，支持加载 ResourceLocation 贴图为 `Image`。 |
| `icyllis.modernui.mc.ScreenCallback` | 屏幕控制接口，控制是否允许 ESC 返回、是否开启背景虚化 `shouldBlurBackground()`、是否默认底色。 |
| `icyllis.modernui.mc.neoforge.MenuScreenFactory` | 用于注册容器（Container / Menu）界面的工厂构造器。 |

---

## 三、 典型开发范式与代码模板

### 1. 原生渲染增强（画圆角、渐变、圆形）
不想自己写 Blit 和 Shader，只想在原版渲染管道（如 HUD、Tooltip、Layer）中画高级几何图形：

```java
import icyllis.modernui.mc.ExtendedGuiGraphics;
import net.minecraft.client.gui.GuiGraphics;

public void renderMyModernHud(GuiGraphics gr, int x, int y, int w, int h) {
    ExtendedGuiGraphics exGr = new ExtendedGuiGraphics(gr);

    // 1. 设置渐变方向与颜色 (支持 8 种方位：TL_BR, TOP_BOTTOM, LEFT_RIGHT 等)
    exGr.setGradient(ExtendedGuiGraphics.Orientation.TL_BR, 0xFF295E8A, 0xFF1E3A5F);

    // 2. 填充圆角矩形 (left, top, right, bottom, radius)
    exGr.fillRoundRect(x, y, x + w, y + h, 6.0f);

    // 3. 边框描边
    exGr.setColor(0xFFFFFFFF);
    exGr.setStrokeWidth(1.5f);
    exGr.strokeRoundRect(x, y, x + w, y + h, 6.0f);

    // 4. 画圆或圆环
    // exGr.fillCircle(centerX, centerY, radius);
    // exGr.strokeCircle(centerX, centerY, radius);
}
```

---

### 2. 标准 UI 页面开发（基于 Fragment 与 View 树）
构建独立设置面板、弹窗、信息面板，脱离原版死板的 Widget 按钮：

```java
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.fragment.app.Fragment;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.util.DataSet;

public class MyCustomPanelFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable DataSet savedInstanceState) {
        // 创建主容器（垂直线性布局）
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);

        // 设置背景样式 (带圆角和描边的背景)
        ShapeDrawable background = new ShapeDrawable();
        background.setCornerRadius(root.dp(8));
        background.setColor(0xCC1E1E1E); // 半透明暗底
        background.setStroke(root.dp(2), 0xFF3F51B5); // 描边
        root.setBackground(background);
        root.setPadding(root.dp(16), root.dp(16), root.dp(16), root.dp(16));

        // 可以在此添加各种 View：TextView, EditText, Button 等
        // ...

        // 居中布局参数
        root.setLayoutParams(new FrameLayout.LayoutParams(
                root.dp(300), 
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                Gravity.CENTER
        ));

        return root;
    }
}
```
**打开该 UI**：
```java
icyllis.modernui.mc.MuiModApi.openScreen(new MyCustomPanelFragment());
```

---

### 3. 在 Modern UI 布局中嵌入 Minecraft 原生物品 / 模型 (`MinecraftSurfaceView`)
当需要在 Modern UI 的布局体系中嵌入 MC 原生物品或实体时，切勿手动混插渲染调用，必须使用 `MinecraftSurfaceView`：

```java
import icyllis.modernui.mc.MinecraftSurfaceView;
import icyllis.modernui.mc.ExtendedGuiGraphics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ItemDisplayView extends MinecraftSurfaceView {

    public ItemDisplayView(Context context) {
        super(context);
        
        // 绑定自定义的原生 Renderer
        setRenderer(new Renderer() {
            @Override
            public void onSurfaceChanged(int width, int height) {}

            @Override
            public void onDraw(GuiGraphics gr, int mouseX, int mouseY, float deltaTick, double guiScale, float alpha) {
                // 转换坐标
                int itemX = 0;
                int itemY = 0;

                // 可以同时混用 ExtendedGuiGraphics
                ExtendedGuiGraphics exGr = new ExtendedGuiGraphics(gr);
                exGr.fillRoundRect(itemX - 2, itemY - 2, itemX + 18, itemY + 18, 2);

                // 调用原版物品渲染
                gr.renderItem(new ItemStack(Items.NETHERITE_SWORD), itemX, itemY);
            }
        });
    }
}
```

---

### 4. 容器界面注册与桥接 (`MenuScreenFactory`)
替代原版笨重的 `AbstractContainerScreen`：

```java
// 在客户端 Mod 事件总线的 onRegisterMenuScreens 中：
@SubscribeEvent
public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
    event.register(
        MyMenus.TEST_MENU.get(), 
        MenuScreenFactory.create(menu -> new MyCustomContainerFragment(menu))
    );
}
```

---

### 5. 动画系统调用规范
Modern UI 自带 Android 级属性动画机制（`ObjectAnimator`、`TimeInterpolator`），严禁使用基于 Tick 递增的计算方式：

```java
import icyllis.modernui.animation.ObjectAnimator;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.util.FloatProperty;

// 针对某个 View 的 float 属性做平滑缓动动画
ObjectAnimator animator = ObjectAnimator.ofFloat(targetView, new FloatProperty<>("customProp") {
    @Override
    public void setValue(MyView object, float value) {
        object.setCustomValue(value);
        object.invalidate(); // 触发重绘
    }

    @Override
    public Float get(MyView object) {
        return object.getCustomValue();
    }
}, 0f, 100f);

animator.setDuration(300); // 毫秒
animator.setInterpolator(TimeInterpolator.DECELERATE);
animator.start();
```

---

## 四、 常见错误与避坑指南

1. **绝对禁止**在 Modern UI 的 `onDraw(Canvas canvas)` 中直接强转 `GuiGraphics`。View 系统采用自己的 2D Canvas 架构；若必须混合 MC 原版渲染，必须在 `MinecraftSurfaceView` 的 `Renderer` 回调中使用提供的 `GuiGraphics`。
2. **像素单位注意**：在布局配置中，直接写数值会被视为物理像素，务必使用 `view.dp(数值)` 转换；字体大小建议使用 `setTextSize(数值)`（Modern UI 会自动适配 `sp` 缩放）。
3. **主线程约定**：`openScreen` 必须在 Minecraft 主线程调用；后台线程或网络包线程回调中请调用 `MuiModApi.postToUiThread(runnable)`。
4. **拦截原版 HUD 必须走 NeoForge 原生事件**：取消原版血条/经验条等操作，使用 `RenderGuiLayerEvent.Pre`，通过匹配 `VanillaGuiLayers.XXXX` 并执行 `event.setCanceled(true)`。
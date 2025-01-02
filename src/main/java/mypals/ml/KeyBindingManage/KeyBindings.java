package mypals.ml.KeyBindingManage;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    // 定义按键绑定
    public static KeyBinding TOGGLE_RENDERER_F3;
    public static KeyBinding TOGGLE_RENDERER_E;

    public static void registerKeyBindings() {
        // 创建按键绑定对象
        TOGGLE_RENDERER_F3 = new KeyBinding(
                "key.explosive.toggle_main_render_F3", // 键绑定名称
                InputUtil.Type.KEYSYM, // 输入类型 (按键类型)
                GLFW.GLFW_KEY_F3, // 默认按键 (M 键)
                "category.explosive_key_bind" // 键绑定类别
        );
        TOGGLE_RENDERER_E = new KeyBinding(
                "key.explosive.toggle_main_render_E", // 键绑定名称
                InputUtil.Type.KEYSYM, // 输入类型 (按键类型)
                GLFW.GLFW_KEY_E, // 默认按键 (M 键)
                "category.explosive_key_bind" // 键绑定类别
        );

        // 使用 KeyBindingHelper 注册按键绑定
        KeyBindingHelper.registerKeyBinding(TOGGLE_RENDERER_F3);
        KeyBindingHelper.registerKeyBinding(TOGGLE_RENDERER_E);
    }
}

package mypals.ml.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static mypals.ml.config.LucidityConfig.showInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "render", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/render/WorldRenderer;checkEmpty(Lnet/minecraft/client/util/math/MatrixStack;)V", ordinal = 0))
    private void render(CallbackInfo ci,
                        @Local MatrixStack matrixStack,
                        @Local RenderTickCounter tickCounter
    ) {
        if(showInfo) {
            /*InfoRenderer.render(
                    matrixStack,
                    tickCounter
            );*/
        }
    }

}
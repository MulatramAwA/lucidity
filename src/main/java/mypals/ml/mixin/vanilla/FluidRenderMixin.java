package mypals.ml.mixin.vanilla;

import mypals.ml.features.selectiveRendering.SelectiveRenderingManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static mypals.ml.features.selectiveRendering.SelectiveRenderingManager.blockRenderMode;
import static mypals.ml.features.selectiveRendering.SelectiveRenderingManager.shouldRenderBlock;
import static net.caffeinemc.mods.sodium.client.render.chunk.ExtendedBlockEntityType.shouldRender;

@Mixin(FluidRenderer.class)
public class FluidRenderMixin {
    @Inject(at = @At("RETURN"),method = "shouldRenderSide(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/fluid/FluidState;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;Lnet/minecraft/fluid/FluidState;)Z"
            ,cancellable = true)
    private static void shouldRenderSide(BlockRenderView world, BlockPos pos, FluidState fluidState, BlockState blockState, Direction direction, FluidState neighborFluidState, CallbackInfoReturnable<Boolean> cir) {
        if(!blockRenderMode.equals(SelectiveRenderingManager.RenderMode.OFF)) {
            if(!shouldRenderBlock(world.getBlockState(pos.offset(direction)),pos.offset(direction)))
                cir.setReturnValue(false);
        }
    }
    @Inject(at = @At("RETURN"),method = "isSideCovered(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/Direction;FLnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"
            ,cancellable = true)
    private static void isSideCovered(BlockView world, Direction direction, float height, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if(!blockRenderMode.equals(SelectiveRenderingManager.RenderMode.OFF)) {
            if(!shouldRenderBlock(world.getBlockState(pos.offset(direction)),pos.offset(direction)))
                cir.setReturnValue(false);
        }
    }
}

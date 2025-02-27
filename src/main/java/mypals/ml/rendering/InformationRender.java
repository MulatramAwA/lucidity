package mypals.ml.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import mypals.ml.features.blockOutline.OutlineManager;
import mypals.ml.features.selectiveRendering.AreaBox;
import mypals.ml.features.selectiveRendering.WandActionsManager;
import mypals.ml.rendering.shapes.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.core.jmx.Server;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static mypals.ml.Lucidity.LOGGER;
import static mypals.ml.Lucidity.getPlayerLookedBlock;
import static mypals.ml.config.LucidityConfig.selectInSpectator;
import static mypals.ml.features.selectiveRendering.SelectiveRenderingManager.selectedAreas;
import static mypals.ml.features.selectiveRendering.SelectiveRenderingManager.wand;
import static mypals.ml.features.selectiveRendering.WandActionsManager.deleteMode;
import static mypals.ml.features.selectiveRendering.WandActionsManager.getAreasToDelete;
import static mypals.ml.rendering.ShapeRender.*;
import static mypals.ml.rendering.shapes.ShineMarker.calculateAlpha;

public class InformationRender {
    public static List<BoxShape> boxes = new CopyOnWriteArrayList<>();
    public static List<CubeShape> cubes = new CopyOnWriteArrayList<>();
    public static List<LineShape> lines = new CopyOnWriteArrayList<>();
    public static List<MultiPointLine> multiPointLines = new CopyOnWriteArrayList<>();
    public static List<ShineMarker> shineMarkers = new CopyOnWriteArrayList<>();
    public static List<Text> texts = new CopyOnWriteArrayList<>();




    public static void addBox(BoxShape box) {
        boxes.add(box);
    }
    public static void addText(Text text) {
        texts.add(text);
    }
    public static void addCube(CubeShape cube) {
        cubes.add(cube);
    }
    public static void addLine(LineShape line) {
        lines.add(line);
    }
    public static void addLine(MultiPointLine line) {multiPointLines.add(line);}

    public static void addBoxs(List<BoxShape> box) {
        boxes.addAll(box);
    }
    public static void addTexts(List<Text> text) {
        texts.addAll(text);
    }
    public static void addCubes(List<CubeShape> cube) {
        cubes.addAll(cube);
    }
    public static void addLines(List<LineShape> line) {
        lines.addAll(line);
    }
    public static void addMultiPointLines(List<MultiPointLine> line) {multiPointLines.addAll(line);}
    public static void addShineMarker(ShineMarker shineMarker,int time){
        shineMarker.lifeTime = time;
        shineMarkers.forEach(marker->{
            if(shineMarker.pos == marker.pos){
                marker.lifeTime = time;
            }
        });
        shineMarkers.add(shineMarker);
    }
    public static void render(MatrixStack matrixStack, RenderTickCounter counter){
        Tessellator tessellator = Tessellator.getInstance();

        if(MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().gameRenderer.getCamera().isReady()) {
            try {
                //renderPicture(matrixStack, Identifier.of(MOD_ID,"textures/DebugPNG.png"),new Vec3d(0,0,0),1,240, OverlayTexture.DEFAULT_UV,counter.getTickDelta(true));
                for (BoxShape box : boxes) {
                    box.draw(matrixStack);
                }
                for (CubeShape cube : cubes) {
                    cube.draw(matrixStack, cube.pos, 0.01f, counter.getTickDelta(true), cube.color, cube.alpha, cube.seeThrough);
                }
                drawLines(matrixStack);
                drawMarkers(matrixStack);
                for (Text text : texts) {
                    text.draw(matrixStack, text.pos, counter.getTickDelta(true), (ArrayList) text.texts, text.color, text.size, text.seeThrough);
                }
                drawSelectedAreas(matrixStack);
            }catch (Exception e){
                LOGGER.info(e.toString());
            }
        }
        //OutlineManager.resolveBlocks();
        //OutlineManager.onRenderWorldLast(matrixStack);
    }
    public static void clear(){
        boxes.clear();
        lines.clear();
        cubes.clear();
        texts.clear();
        multiPointLines.clear();
        shineMarkers.forEach(marker->{
            marker.lifeTime--;
            if(marker.lifeTime <= 0){
                shineMarkers.remove(marker);
            }
        });
    }
    private static void drawSelectedAreas(MatrixStack matrixStack){
        if (MinecraftClient.getInstance().player != null && (MinecraftClient.getInstance().player.getMainHandStack().getItem() == wand || (MinecraftClient.getInstance().player.isSpectator() && selectInSpectator))) {

            BlockHitResult result = getPlayerLookedBlock(MinecraftClient.getInstance().player, MinecraftClient.getInstance().world);
            BlockPos lookingAt = result.getType() == HitResult.Type.BLOCK ? result.getBlockPos() : BlockPos.ofFloored(result.getPos());
            CubeShape.draw(matrixStack, lookingAt, 0.01f, 0, deleteMode ? Color.red : Color.white, 0.2f, false);
            if (WandActionsManager.pos1 != null) {
                renderSelectionBox(matrixStack, MinecraftClient.getInstance().gameRenderer.getCamera(), 0);
            }

            if (deleteMode) {
                for (AreaBox selectedArea : getAreasToDelete(lookingAt, false)) {
                    selectedArea.draw(matrixStack, Color.red, 0.4f, true);
                }
            }
            for (AreaBox selectedArea : selectedAreas) {
                selectedArea.draw(matrixStack, 0.05f, false);
            }
        }

    }
    private static void drawMarkers(MatrixStack matrixStack){
        for (ShineMarker marker : shineMarkers) {
            marker.draw(matrixStack, MinecraftClient.getInstance().cameraEntity.age, Math.round(calculateAlpha(MinecraftClient.getInstance().cameraEntity.getPos(),
                    marker.pos, marker.lifeTime) * 255),marker.seeThrough);
        }
    }
    private static void drawLines(MatrixStack matrixStack){
        for (LineShape line : lines) {
            line.draw(matrixStack, line.start, line.end, line.color, line.alpha,line.seeThrough);
        }
        for (MultiPointLine line : multiPointLines) {
            line.draw(matrixStack, line.points, line.color, line.alpha,line.seeThrough);
        }
    }


}

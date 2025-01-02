package mypals.ml;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mypals.ml.config.Keybinds;
import mypals.ml.config.LucidityConfig;
import mypals.ml.explosionManage.ExplotionAffectdDataManage.ExplosionCastLines.ExplosionCastLine;
import mypals.ml.features.renderKeyPresses.KeyPressesManager;
import mypals.ml.features.sonicBoomDetection.WardenStateResolver;
import mypals.ml.rendering.InformationRender;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import mypals.ml.explosionManage.*;
import mypals.ml.explosionManage.ExplotionAffectdDataManage.DamagedEntityData.EntityToDamage;
import mypals.ml.explosionManage.ExplotionAffectdDataManage.DamagedEntityData.SamplePointsData.SamplePointData;
import mypals.ml.explosionManage.ExplotionAffectdDataManage.ExplosionAffectedObjects;
import mypals.ml.rendering.InfoRenderer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static mypals.ml.config.LucidityConfig.*;
import static mypals.ml.explosionManage.ExplosionSimulateManager.*;
import static mypals.ml.features.safeDigging.DiggingSituationResolver.WARNING_TIME;
import static mypals.ml.features.safeDigging.DiggingSituationResolver.warningTime;
import static mypals.ml.features.selectiveRendering.SelectiveRenderingManager.*;
import static mypals.ml.features.selectiveRendering.SelectiveRenderingManager.wand;
import static mypals.ml.features.selectiveRendering.WandActionsManager.selectCoolDown;
import static mypals.ml.features.selectiveRendering.WandActionsManager.wandActions;
import static mypals.ml.features.worldEaterHelper.oreResolver.scanForMineralsOptimized;
import static mypals.ml.rendering.InfoRenderer.render;

public class Lucidity implements ModInitializer {
	public static final String MOD_ID = "lucidity";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final SimpleOption<Double> GAMMA_BYPASS = new SimpleOption<>("options.gamma", SimpleOption.emptyTooltip(), (optionText, value) -> Text.empty(), SimpleOption.DoubleSliderCallbacks.INSTANCE.withModifier(
			d -> (double) 40, d -> 1
	), 40.0, value -> {
	});
	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
	public static Set<BlockPos> blocksToDestroy = new HashSet<>();
	public static Set<Vec3d> explotionCenters = new HashSet<>();
	public static Set<EntityToDamage> entitysToDamage = new HashSet<>();

	public static  Set<SamplePointData> samplePointDatas = new HashSet<>();

	public static  Set<FakeExplosion> fakeExplosions = new HashSet<>();

	public static Set<ExplosionCastLine> explosionCastedLines = new HashSet<>();

	public static void onConfigUpdated() {
		MinecraftClient client = MinecraftClient.getInstance();
        LucidityConfig.CONFIG_HANDLER.instance();
		updateChunks(client);
		loadConfig();
		resolveSettings();
    }
	private static void resolveSettings(){
		resolveSelectedBlockTypesFromString(LucidityConfig.selectedBlockTypes);
		resolveSelectedEntityTypesFromString(LucidityConfig.selectedEntityTypes);
		resolveSelectedParticleTypesFromString(LucidityConfig.selectedParticleTypes);

		resolveSelectedAreasFromString(LucidityConfig.selectedAreasSaved);
		resolveSelectedWandFromString(LucidityConfig.wand);
		if(!!blockRenderMode.equals(RenderMode.OFF) && MinecraftClient.getInstance().player!=null)
			MinecraftClient.getInstance().player.
					removeStatusEffect(StatusEffects.NIGHT_VISION);
	}
	public static void updateChunks(MinecraftClient client){
		client.worldRenderer.reload();
	}
	public static void loadConfig(){
		var instance = LucidityConfig.CONFIG_HANDLER;
		instance.load();
		LucidityConfig.CONFIG_HANDLER.instance();
		resolveSettings();
	}

    	@Override
	public void onInitialize() {
		loadConfig();
		UpadteSettings();

		Keybinds.initialize();
		HudRenderCallback.EVENT.register((context, tickDelta) -> {
			KeyPressesManager.renderPressed(context);
			//WandTooltipRenderer.renderWandTooltip(context);
		});
		ClientTickEvents.END_CLIENT_TICK.register(client-> {
			InformationRender.clear();
			wandActions(MinecraftClient.getInstance());
			//resolveEntities(client,50);
			resolveEnviroment(client);
			UpdateTimers();
		});
		ClientTickEvents.END_WORLD_TICK.register(t->{
			extraActions();
		});
		AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) -> {
			warningTime = WARNING_TIME;
			if (world.isClient && player.getStackInHand(hand).getItem() == wand && player.isCreative()) {
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});
		WorldRenderEvents.START.register((WorldRenderContext context) -> {

			if(showInfo) {
				RenderSystem.setShader(GameRenderer::getPositionColorProgram);
				RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
				RenderSystem.depthMask(false);
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
				BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
				render(context.matrixStack(), context.tickCounter(), buffer);
				RenderSystem.applyModelViewMatrix();
				RenderSystem.setShaderColor(1, 1, 1, 1);

				RenderSystem.disableBlend();
			}
		});
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			register(dispatcher);
		});
		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
		MinecraftClient client = MinecraftClient.getInstance();
		if(client != null && client.getWindow() != null) {
			if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_F3)) {
				if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_E)) {
					setOnOff(!showInfo);
					assert MinecraftClient.getInstance().player != null;
					MinecraftClient.getInstance().player.sendMessage(Text.of("Explosive object render now set to " + showInfo), false);
					KeyBinding.unpressAll();
				}
			}
		}
	}
	private static void extraActions(){
		LucidityConfig.CONFIG_HANDLER.instance();
		if(!blockRenderMode.equals(RenderMode.OFF) && MinecraftClient.getInstance().player != null){
			MinecraftClient.getInstance().player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,10000,5,true,false,false));
        }
	}
	private static void resolveEntities(MinecraftClient client, double range){
		ClientWorld world = client.world;
		if(world == null){return;}
        assert client.player != null;
        Box searchBox = new Box(
				client.player.getX() - range, client.player.getY() - range, client.player.getZ() - range,
				client.player.getX() + range, client.player.getY() + range, client.player.getZ() + range
		);
		world.getEntitiesByClass(WardenEntity.class,searchBox, warden -> true)
				.forEach(WardenStateResolver::resolveWardenState);
		
	}
	private static void resolveEnviroment(MinecraftClient client){
		ClientWorld world = client.world;
		if(world == null){return;}
		assert client.player != null;
		PlayerEntity player = client.player;
		if(warningTime > 0){
			BlockHitResult blockBreakingRayCast = getPlayerLookedBlock(player,world);
			//resolveBreakingSituation(player,world,blockBreakingRayCast.getBlockPos());
		}
		if(enableWorldEaterHelper) {
			scanForMineralsOptimized(hightLightRange);
		}
	}
	public static void UpdateTimers(){
		warningTime = warningTime <= 0? 0 : warningTime-1;
		selectCoolDown = selectCoolDown <= 0? 0 : selectCoolDown-1;
	}
	public static BlockHitResult getPlayerLookedBlock(PlayerEntity player, World world) {
		Entity camera = MinecraftClient.getInstance().getCameraEntity();

		Vec3d start = camera.getCameraPosVec(1.0F);

		Vec3d end = start.add(camera.getRotationVec(1.0F).multiply(player.isCreative()?5:4));


		RaycastContext context = new RaycastContext(
				start,
				end,
				RaycastContext.ShapeType.OUTLINE,
				RaycastContext.FluidHandling.NONE,
				player
		);
		return world.raycast(context);
	}
	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		dispatcher.register(
				ClientCommandManager.literal("explosionVisualizer")
						.then(ClientCommandManager.literal("mainRender")
								.then(ClientCommandManager.argument("toggle", BoolArgumentType.bool())
										.executes(context -> {
											boolean toggle = BoolArgumentType.getBool(context, "toggle");

											Text coloredMessage = Text.literal(Text.translatable("command.lucidity.main_render", toggle).getString()).formatted(Formatting.GOLD);

											assert player != null;
											player.sendMessage(coloredMessage, false);

											setOnOff(toggle);
											return 1;
										})
								)
						)
						.then(ClientCommandManager.literal("renderEntityDamage")
								.then(ClientCommandManager.argument("toggle", BoolArgumentType.bool())
										.executes(context -> {
											boolean toggle = BoolArgumentType.getBool(context, "toggle");

											Text coloredMessage = Text.literal(Text.translatable("command.explosion-visualizer.entity_damage_render", toggle).getString()).formatted(Formatting.GREEN);

											assert player != null;
											player.sendMessage(coloredMessage, false);

											setDamageOnOff(toggle);
											return 1;
										})
								))
						.then(ClientCommandManager.literal("renderEntityRayCast")
								.then(ClientCommandManager.argument("toggle", BoolArgumentType.bool())
										.executes(context -> {
											boolean toggle = BoolArgumentType.getBool(context, "toggle");

											Text coloredMessage = Text.literal(Text.translatable("command.explosion-visualizer.entity_ray_cast", toggle).getString()).formatted(Formatting.GREEN);

											assert player != null;
											player.sendMessage(coloredMessage, false);

											setRayCastInfoOnOff(toggle);
											return 1;
										})
								)
						)
						.then(ClientCommandManager.literal("renderBlockDestruction")
								.then(ClientCommandManager.argument("toggle", BoolArgumentType.bool())
										.executes(context -> {
											boolean toggle = BoolArgumentType.getBool(context, "toggle");

											Text coloredMessage = Text.literal(Text.translatable("command.explosion-visualizer.block_destruction_render", toggle).getString()).formatted(Formatting.GREEN);

											assert player != null;
											player.sendMessage(coloredMessage, false);

											setBlockDestroyInfoOnOff(toggle);
											return 1;
										})
								))
						.then(ClientCommandManager.literal("renderBlockDetectionRay")
								.then(ClientCommandManager.argument("toggle", BoolArgumentType.bool())
										.executes(context -> {
											boolean toggle = BoolArgumentType.getBool(context, "toggle");

											Text coloredMessage = Text.literal(Text.translatable("command.explosion-visualizer.block_detection_ray_render", toggle).getString()).formatted(Formatting.GREEN);

											assert player != null;
											player.sendMessage(coloredMessage, false);

											setExplosionBlockDamageRayInfoOnOff(toggle);
											return 1;
										})
								))
						.then(ClientCommandManager.literal("blockDamageRayRendererSettings")
								.then(ClientCommandManager.literal("range")
										.then(ClientCommandManager.argument("Xmin", IntegerArgumentType.integer(-1, 16))
												.then(ClientCommandManager.argument("Xmax", IntegerArgumentType.integer(-1, 16))
														.then(ClientCommandManager.argument("Ymin", IntegerArgumentType.integer(-1, 16))
																.then(ClientCommandManager.argument("Ymax", IntegerArgumentType.integer(-1, 16))
																		.then(ClientCommandManager.argument("Zmin", IntegerArgumentType.integer(-1, 16))
																				.then(ClientCommandManager.argument("Zmax", IntegerArgumentType.integer(-1, 16))
																						.executes(context -> {
																							int Xmin = IntegerArgumentType.getInteger(context, "Xmin");
																							int Xmax = IntegerArgumentType.getInteger(context, "Xmax");
																							int Ymin = IntegerArgumentType.getInteger(context, "Ymin");
																							int Ymax = IntegerArgumentType.getInteger(context, "Ymax");
																							int Zmin = IntegerArgumentType.getInteger(context, "Zmin");
																							int Zmax = IntegerArgumentType.getInteger(context, "Zmax");
																							SetDestructionRayRenderRange(Xmin, Xmax, Ymin,Ymax,Zmin,Zmax);

																							Text coloredMessage = Text.literal(Text.translatable("command.explosion-visualizer.block_damage_ray_range_updated").getString()).formatted(Formatting.GREEN);

																							assert player != null;
																							player.sendMessage(coloredMessage, false);

																							return 1;
																						})
																				)
																		)
																)
														)
												)
										)
								)
								.then(ClientCommandManager.literal("layer")
										.then(ClientCommandManager.argument("LayerMin", IntegerArgumentType.integer(0, 114514))
												.then(ClientCommandManager.argument("LayerMax", IntegerArgumentType.integer(0, 114514))
														.executes(context -> {
															int LayerMin = IntegerArgumentType.getInteger(context, "LayerMin");
															int LayerMax = IntegerArgumentType.getInteger(context, "LayerMax");
															SetDestructionRayRenderLayer(LayerMin,LayerMax);

															Text coloredMessage = Text.literal(Text.translatable("command.explosion-visualizer.block_damage_ray_layer_updated").getString()).formatted(Formatting.GREEN);

															assert player != null;
															player.sendMessage(coloredMessage, false);

															return 1;
														})
												)
										)
								)
								.then(ClientCommandManager.literal("resetAll")
										.executes(context -> {
											SetDestructionRayRenderLayer(0,100);
											SetDestructionRayRenderRange(0,16,0,16,0,16);

											Text coloredMessage = Text.literal(Text.translatable("command.explosion-visualizer.block_detection_ray_settings_reset").getString()).formatted(Formatting.RED);

											assert player != null;
											player.sendMessage(coloredMessage, false);

											return 1;
										})
								)
								.then(ClientCommandManager.literal("resetLayer")
										.executes(context -> {
											SetDestructionRayRenderLayer(0,100);

											Text coloredMessage = Text.literal(Text.translatable("command.explosion-visualizer.block_detection_ray_layer_reset").getString()).formatted(Formatting.YELLOW);

											assert player != null;
											player.sendMessage(coloredMessage, false);

											return 1;
										})
								)
								.then(ClientCommandManager.literal("resetRange")
										.executes(context -> {
											SetDestructionRayRenderRange(0,16,0,16,0,16);

											Text coloredMessage = Text.literal(Text.translatable("command.explosion-visualizer.block_detection_ray_range_reset").getString()).formatted(Formatting.YELLOW);

											assert player != null;
											player.sendMessage(coloredMessage, false);

											return 1;
										})
								)

						)
						.then(ClientCommandManager.literal("fakeExplosion")
								.then(ClientCommandManager.literal("add")
										.then(ClientCommandManager.argument("name", StringArgumentType.string())
												.then(ClientCommandManager.argument("x", FloatArgumentType.floatArg())
														.then(ClientCommandManager.argument("y", FloatArgumentType.floatArg())
																.then(ClientCommandManager.argument("z", FloatArgumentType.floatArg())
																		.then(ClientCommandManager.argument("power", FloatArgumentType.floatArg())
																				.then(ClientCommandManager.argument("ignoreBlockInside", BoolArgumentType.bool())
																						.executes(context -> {

																							float x = FloatArgumentType.getFloat(context, "x");
																							float y = FloatArgumentType.getFloat(context, "y");
																							float z = FloatArgumentType.getFloat(context, "z");
																							float p = FloatArgumentType.getFloat(context, "power");
																							boolean ignoreBlockInside = BoolArgumentType.getBool(context,"ignoreBlockInside");
																							String name = StringArgumentType.getString(context, "name");

																							for(FakeExplosion FE : fakeExplosions)
																							{
																								if(Objects.equals(FE.name, name)) {

																									Text coloredMessage = Text.literal(Text.translatable("command.explosion-visualizer.fake_explosion_duplicate",name, new Vec3d(x, y, z).toString()).getString()).formatted(Formatting.RED);

																									assert player != null;
																									player.sendMessage(coloredMessage, false);
																									return 1;
																								}
																							}
																							fakeExplosions.add(new FakeExplosion(x, y, z, p, ignoreBlockInside, name));
																							Text coloredMessage = Text.literal(Text.translatable("command.explosion-visualizer.fake_explosion_add", name, new Vec3d(x, y, z), p).getString()).formatted(Formatting.GREEN);
																							assert player != null;
																							player.sendMessage(coloredMessage, false);
																							return 1;
																						})
																				)
																		)
																)
														)
												)
										)
								)
								.then(ClientCommandManager.literal("remove")
										.then(ClientCommandManager.argument("name", StringArgumentType.string())
												.suggests(suggestFromSet(fakeExplosions))
												.executes(context -> {
													String n = StringArgumentType.getString(context, "name");
													fakeExplosions.removeIf(fe -> Objects.equals(fe.name, n));
													Text coloredMessage = Text.literal(Text.translatable("command.explosion-visualizer.fake_explosion_remove" + n).getString()).formatted(Formatting.YELLOW);

													assert player != null;
													player.sendMessage(coloredMessage, false);
													return 1;
												})
										)
										.then(ClientCommandManager.literal("all")
												.executes(context -> {
													fakeExplosions.clear();
													Text coloredMessage = Text.literal(Text.translatable("command.explosion-visualizer.fake_explosion_clear").getString()).formatted(Formatting.RED);

													assert player != null;
													player.sendMessage(coloredMessage, false);
													return 1;
												})
										)
								)
						)
		);
	}
	private static SuggestionProvider<FabricClientCommandSource> suggestFromSet(Set<FakeExplosion> explosions) {
		return (context, builder) -> {

			Set<String> names = explosions.stream()
					.map(fakeExplosion -> fakeExplosion.name)
					.collect(Collectors.toSet());
			return CommandSource.suggestMatching(names, builder);
		};
	}

	public static void FixRangeIssue()
	{
		if(CONFIG_HANDLER.instance().Xmax < CONFIG_HANDLER.instance().Xmin)
		{
			CONFIG_HANDLER.instance().Xmax = CONFIG_HANDLER.instance().Xmin;
			CONFIG_HANDLER.save();
			UpadteSettings();
		}
		if(CONFIG_HANDLER.instance().Ymax < CONFIG_HANDLER.instance().Ymin)
		{
			CONFIG_HANDLER.instance().Ymax = CONFIG_HANDLER.instance().Ymin;
			CONFIG_HANDLER.save();
			UpadteSettings();
		}
		if(CONFIG_HANDLER.instance().Zmax < CONFIG_HANDLER.instance().Zmin)
		{
			CONFIG_HANDLER.instance().Zmax = CONFIG_HANDLER.instance().Zmin;
			CONFIG_HANDLER.save();
			UpadteSettings();
		}
		if(CONFIG_HANDLER.instance().LayerMax < CONFIG_HANDLER.instance().LayerMin)
		{
			CONFIG_HANDLER.instance().LayerMax = CONFIG_HANDLER.instance().LayerMin + 1;
			CONFIG_HANDLER.save();
			UpadteSettings();
		}


	}
	public static void SetDestructionRayRenderRange(int XMin, int XMax,int YMin, int YMax,int ZMin, int ZMax)
	{
		CONFIG_HANDLER.instance().Xmin = XMin;
		CONFIG_HANDLER.instance().Xmax = XMax;
		CONFIG_HANDLER.instance().Ymin = YMin;
		CONFIG_HANDLER.instance().Ymax = YMax;
		CONFIG_HANDLER.instance().Zmin = ZMin;
		CONFIG_HANDLER.instance().Zmax = ZMax;
		CONFIG_HANDLER.save();
		UpadteSettings();
	}
	public static void SetDestructionRayRenderLayer(int min, int max)
	{
		CONFIG_HANDLER.instance().LayerMax = max;
		CONFIG_HANDLER.instance().LayerMin = min;
		CONFIG_HANDLER.save();
		UpadteSettings();
	}
	public static void setOnOff(boolean toggle)
	{
		CONFIG_HANDLER.instance().showInfo = toggle;
		if(toggle)
			CONFIG_HANDLER.instance().showBlockDestroyInfo = true;
		CONFIG_HANDLER.save();
		UpadteSettings();
	}
	public static void setRayCastInfoOnOff(boolean toggle)
	{
		CONFIG_HANDLER.instance().showRayCastInfo = toggle;
		if(toggle)
			CONFIG_HANDLER.instance().showInfo = true;
		CONFIG_HANDLER.save();
		UpadteSettings();
	}
	public static void setBlockDestroyInfoOnOff(boolean toggle)
	{
		CONFIG_HANDLER.instance().showBlockDestroyInfo = toggle;
		if(toggle)
			CONFIG_HANDLER.instance().showInfo = true;
		CONFIG_HANDLER.save();
		UpadteSettings();
	}
	public static void setDamageOnOff(boolean toggle)
	{
		CONFIG_HANDLER.instance().showDamageInfo = toggle;
		if(toggle)
			CONFIG_HANDLER.instance().showInfo = true;
		CONFIG_HANDLER.save();
		UpadteSettings();
	}
	public static void setExplosionBlockDamageRayInfoOnOff(boolean toggle)
	{
		CONFIG_HANDLER.instance().showExplosionBlockDamageRayInfo = toggle;
		if(toggle)
			CONFIG_HANDLER.instance().showInfo = true;
		CONFIG_HANDLER.save();
		UpadteSettings();
	}
	public static void UpadteSettings()
	{
		var instance = CONFIG_HANDLER;
		instance.load();

		showInfo = instance.instance().showInfo;
		showDamageInfo = instance.instance().showDamageInfo;
		showBlockDestroyInfo = instance.instance().showBlockDestroyInfo;
		showRayCastInfo = instance.instance().showRayCastInfo;
		showExplosionBlockDamageRayInfo = instance.instance().showExplosionBlockDamageRayInfo;

		Xmin = instance.instance().Xmin;
		Xmax = instance.instance().Xmax;
		Ymin = instance.instance().Ymin;
		Ymax = instance.instance().Ymax;
		Zmin = instance.instance().Zmin;
		Zmax = instance.instance().Zmax;

		LayerMin = instance.instance().LayerMin;
		LayerMax = instance.instance().LayerMax;


	}
	private void onClientTick(MinecraftClient client) {
		FixRangeIssue();
		assert MinecraftClient.getInstance() != null;
		//createGlowingBlockDisplay(MinecraftClient.getInstance().world, new BlockPos(0, 0, 0));
		if (showInfo) {
			try {
				explosionCastedLines.clear();
				blocksToDestroy.clear();
				entitysToDamage.clear();
				explotionCenters.clear();
				samplePointDatas.clear();
				if (client.world != null && client.player != null) {
					World world = client.world;
					BlockPos playerPos = client.player.getBlockPos();


					List<ExplosionData> exBlockPos = ExplosiveObjectFinder.findExplosiveBlocksInRange(world, playerPos);
					List<ExplosionData> exEntityPos = ExplosiveObjectFinder.findCrystlesInRange(world, playerPos);
					for (ExplosionData explotion : exBlockPos) {
						Vec3d p_d = new Vec3d(explotion.getPosition().toVector3f());
						Vec3i p_i = new Vec3i((int) p_d.x, (int) p_d.y, (int) p_d.z);
						ExplosionAffectedObjects EAO = simulateExplosiveBlocks(world, new BlockPos(p_i), explotion.getStrength());
						explosionCastedLines.addAll(EAO.getExplotionCastedLines());
						blocksToDestroy.addAll(EAO.getBlocksToDestriy());
						entitysToDamage.addAll(EAO.getEntitysToDamage());
						explotionCenters.addAll(EAO.getExplotionCenters());
						samplePointDatas.addAll(EAO.getSamplePointData());

					}
					for (ExplosionData explosion : exEntityPos) {
						ExplosionAffectedObjects EAO = simulateExplosiveEntitys(world, explosion.getPosition(), explosion.getStrength());
						explosionCastedLines.addAll(EAO.getExplotionCastedLines());
						blocksToDestroy.addAll(EAO.getBlocksToDestriy());
						entitysToDamage.addAll(EAO.getEntitysToDamage());
						explotionCenters.addAll(EAO.getExplotionCenters());
						samplePointDatas.addAll(EAO.getSamplePointData());
					}
					for(FakeExplosion fe: fakeExplosions)
					{
						ExplosionAffectedObjects EAO = simulateFakeExplosions(world, new Vec3d(fe.x, fe.y, fe.z), fe.power, fe.ignorBlockInside);
						explosionCastedLines.addAll(EAO.getExplotionCastedLines());
						blocksToDestroy.addAll(EAO.getBlocksToDestriy());
						entitysToDamage.addAll(EAO.getEntitysToDamage());
						explotionCenters.addAll(EAO.getExplotionCenters());
						samplePointDatas.addAll(EAO.getSamplePointData());
					}
					InfoRenderer.setCastedLines(explosionCastedLines);
					InfoRenderer.setBlocksToDamage(blocksToDestroy);
					InfoRenderer.setEntitysToDamage(entitysToDamage);
					InfoRenderer.setExplotionCenters(explotionCenters);
					InfoRenderer.setSamplePointData(samplePointDatas);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
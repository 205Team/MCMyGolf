package net.fabricmc.mygolf;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.mygolf.blockEntity.render.FlagstickEntityRenderer;
import net.fabricmc.mygolf.blockEntity.render.FlagstickScreen;
import net.fabricmc.mygolf.blockEntity.render.FlagstickScreenHandler;
import net.fabricmc.mygolf.entity.model.GolfBallEntityModel;
import net.fabricmc.mygolf.entity.renderer.GolfBallEntityRenderer;
import net.fabricmc.mygolf.global.CommonStr;
import net.fabricmc.mygolf.registry.RegisterBlockEntities;
import net.fabricmc.mygolf.registry.RegisterBlocks;
import net.fabricmc.mygolf.registry.RegisterEntities;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class MyGolfModClient implements ClientModInitializer {
    public static final EntityModelLayer MODEL_CUBE_LAYER = new EntityModelLayer(new Identifier(CommonStr.modId, "golf_ball"), "main");
    public static final EntityModelLayer MODEL_FLAGSTICK_LAYER = new EntityModelLayer(new Identifier(CommonStr.modId, "flagstick"), "main");
    public static final ScreenHandlerType<FlagstickScreenHandler> FLAGSTICK_SCREEN_HANDLER = new ScreenHandlerType<>(FlagstickScreenHandler::new, FeatureSet.of(FeatureFlags.VANILLA));

    @Override
    public void onInitializeClient() {
        /*
         * Registers our Cube Entity's renderer, which provides a model and texture for the entity.
         *
         * Entity Renderers can also manipulate the model before it renders based on entity context (EndermanEntityRenderer#render).
         */

        // In 1.17, use EntityRendererRegistry.register (seen below) instead of EntityRendererRegistry.INSTANCE.register (seen above)
        EntityRendererRegistry.register(RegisterEntities.GOLF_BALL, (context) -> new GolfBallEntityRenderer(context));

        EntityModelLayerRegistry.registerModelLayer(MODEL_CUBE_LAYER, GolfBallEntityModel::getTexturedModelData);

        EntityModelLayerRegistry.registerModelLayer(MODEL_FLAGSTICK_LAYER, FlagstickEntityRenderer::getTexturedModelData);

        BlockEntityRendererRegistry.register(RegisterBlockEntities.FLAGSTICK_ENTITY, FlagstickEntityRenderer::new);

        Registry.register(Registries.SCREEN_HANDLER, new Identifier(CommonStr.modId, "flagstick"), FLAGSTICK_SCREEN_HANDLER);

        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutoutMipped(), RegisterBlocks.GOLF_HOLE);

        ColorProviderRegistry.BLOCK.register(
                (state, view, pos, tintIndex) -> view != null ? BiomeColors.getGrassColor(view, pos) : GrassColors.getColor(0.5D, 1.0D),
                RegisterBlocks.GOLF_HOLE
        );

        HandledScreens.register(FLAGSTICK_SCREEN_HANDLER, FlagstickScreen::new);
    }
}

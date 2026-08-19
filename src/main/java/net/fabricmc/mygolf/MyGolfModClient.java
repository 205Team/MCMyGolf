package net.fabricmc.mygolf;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.fabricmc.mygolf.blockEntity.render.FlagstickEntityRenderer;
import net.fabricmc.mygolf.blockEntity.render.FlagstickScreenHandler;
import net.fabricmc.mygolf.entity.model.GolfBallEntityModel;
import net.fabricmc.mygolf.entity.renderer.GolfBallEntityRenderer;
import net.fabricmc.mygolf.events.client.GolfHudOverlay;
import net.fabricmc.mygolf.global.CommonStr;
import net.fabricmc.mygolf.items.GolfClubItem;
import net.fabricmc.mygolf.registry.RegisterBlockEntities;
import net.fabricmc.mygolf.registry.RegisterBlocks;
import net.fabricmc.mygolf.registry.RegisterEntities;
import net.fabricmc.mygolf.registry.RegisterItems;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.item.DyeableItem;
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

        HudRenderCallback.EVENT.register(new GolfHudOverlay());

        Registry.register(Registries.SCREEN_HANDLER, new Identifier(CommonStr.modId, "flagstick"), FLAGSTICK_SCREEN_HANDLER);

        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutoutMipped(), RegisterBlocks.GOLF_HOLE);

        // Golf hole grass color change like grass blocks
        ColorProviderRegistry.BLOCK.register(
                (state, view, pos, tintIndex) -> view != null ? BiomeColors.getGrassColor(view, pos) : GrassColors.getColor(0.5D, 1.0D),
                RegisterBlocks.GOLF_HOLE
        );

        // Give dye color and goaled color effect to golf ball item
        ColorProviderRegistry.ITEM.register(
                (stack, tintIndex) -> {
                    // tintIndex == 0 applies color to the primary layer (#0 in the model JSON)
                    if (tintIndex == 0 && stack.getItem() instanceof DyeableItem dyeable) {
                        return dyeable.getColor(stack);
                    }
                    return 0xFFFFFF; // Default fallback (White)
                },
                RegisterItems.GOLF_BALL
        );

        // Ball item flag icon toggle
        ModelPredicateProviderRegistry.register(
                RegisterItems.GOLF_BALL,
                new Identifier(CommonStr.modId, "goaled"),
                (stack, world, entity, seed) -> {
                    // Read NBT (1.20.4 or below) or Custom Data Component (1.20.5+)
                    if (stack.hasNbt() && stack.getNbt().getBoolean("IsGoaled")) {
                        return 1.0F;
                    }
                    return 0.0F;
                }
        );

    }
}

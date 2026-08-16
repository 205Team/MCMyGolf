package net.fabricmc.mygolf.blockEntity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.mygolf.MyGolfModClient;
import net.fabricmc.mygolf.blockEntity.FlagstickEntity;
import net.fabricmc.mygolf.blocks.FlagstickBlock;
import net.fabricmc.mygolf.global.CommonStr;
import net.minecraft.block.BlockState;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public class FlagstickEntityRenderer implements BlockEntityRenderer<FlagstickEntity> {
    public static final Identifier textureID = new Identifier(CommonStr.modId, "textures/block/flagstick_block.png");
    public static final Identifier FLAGSTICK_BEAM_TEXTURE = new Identifier(CommonStr.modId, "textures/entity/flagstick_beam.png");
    public final ModelPart ironStick;
    public final ModelPart flag;
    public FlagstickEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        ModelPart modelPart = ctx.getLayerModelPart(MyGolfModClient.MODEL_FLAGSTICK_LAYER);
        this.ironStick = modelPart.getChild("iron_stick");
        this.flag = modelPart.getChild("flag");
    }

    @Override
    public void render(FlagstickEntity flagstickEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        World world = flagstickEntity.getWorld();
        if (world == null) return;

        BlockState blockState = flagstickEntity.getCachedState();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntitySolid(textureID));

        // --- Render Flagstick Model ---
        matrices.push();
        matrices.translate(0.5, 0.0, 0.5);
        float rotation = -((float) (blockState.get(FlagstickBlock.ROTATION) * 360) / 16.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        this.ironStick.render(matrices, vertexConsumer, light, overlay);

        long time = world.getTime();
        BlockPos blockPos = flagstickEntity.getPos();
        float k = ((float)Math.floorMod((long)(blockPos.getX() * 7 + blockPos.getY() * 9 + blockPos.getZ() * 13) + time, 100L) + tickDelta) / 100.0F;
        this.flag.yaw = (-0.0125F + 0.05F * MathHelper.cos(6.2831855F * k)) * 3.1415927F;
        this.flag.render(matrices, vertexConsumer, light, overlay);
        matrices.pop();

        // --- Render Beacon Beam ---
        // RGB Color Float Array (Red, Green, Blue) -> Default White
        matrices.push();
        float speedFactor = 0.05f; // 20x slower
        float continuousTime = (time + tickDelta) * speedFactor;
        long customTime = (long) Math.floor(continuousTime);
        float customTickDelta = continuousTime - customTime;
        float[] color = new float[]{1.0F, 1.0F, 1.0F};
        int beamHeight = 256; // How high the beam shoots up
        BeaconBlockEntityRenderer.renderBeam(
                matrices,
                vertexConsumers,
                FLAGSTICK_BEAM_TEXTURE,
                customTickDelta,
                0.05F,         // Height scale
                customTime,
                0,            // Y offset starting point
                beamHeight,   // Maximum Y height
                color,
                0,        // Inner beam radius
                0.375F         // Outer glow radius
        );
        matrices.pop();

    }

    // Made with Blockbench 4.5.2
    // Exported for Minecraft version 1.17+ for Yarn
    // Paste this class into your mod and generate all required imports
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        modelPartData.addChild("iron_stick",
                ModelPartBuilder.create().uv(0, 0).cuboid(-0.5F, -14.0F, -0.5F, 1.0F, 46.0F, 1.0F),
                ModelTransform.NONE);
        modelPartData.addChild("flag",
                ModelPartBuilder.create().uv(8, 0).cuboid(-6.5F, 26.0F, -0.5F, 6.0F, 6.0F, 1.0F),
                ModelTransform.NONE);
        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public boolean rendersOutsideBoundingBox(FlagstickEntity blockEntity) {
        // Prevents the beam from disappearing when looking up at the sky or away from the base block
        return true;
    }

    @Override
    public int getRenderDistance() {
        // Extends the max rendering distance from the default 64 blocks to 256 blocks
        return 256;
    }
}

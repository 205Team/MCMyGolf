package net.fabricmc.mygolf.blockEntity.render;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.mygolf.MyGolfModClient;
import net.fabricmc.mygolf.blockEntity.FlagstickEntity;
import net.fabricmc.mygolf.blocks.Flagstick;
import net.fabricmc.mygolf.global.CommonStr;
import net.minecraft.block.BlockState;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;

@Environment(EnvType.CLIENT)
public class FlagstickEntityRenderer implements BlockEntityRenderer<FlagstickEntity> {
    private static final Identifier textureID = new Identifier("mygolf:textures/block/flagstick.png");
    public final ModelPart ironStick;
    public final ModelPart flag;
    public FlagstickEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        ModelPart modelPart = ctx.getLayerModelPart(MyGolfModClient.MODEL_FLAGSTICK_LAYER);
        this.ironStick = modelPart.getChild("iron_stick");
        this.flag = modelPart.getChild("flag");
    }

    @Override
    public void render(FlagstickEntity flagstickEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        BlockState blockState = flagstickEntity.getCachedState();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntitySolid(textureID));

        matrices.push();
        matrices.translate(0.5, 0.0, 0.5);
        float rotation = -((float) (blockState.get(Flagstick.ROTATION) * 360) / 16.0F);
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(rotation));
        this.ironStick.render(matrices, vertexConsumer, light, overlay);

        long time = flagstickEntity.getWorld().getTime();
        BlockPos blockPos = flagstickEntity.getPos();
        float k = ((float)Math.floorMod((long)(blockPos.getX() * 7 + blockPos.getY() * 9 + blockPos.getZ() * 13) + time, 100L) + tickDelta) / 100.0F;
        this.flag.yaw = (-0.0125F + 0.03F * MathHelper.cos(6.2831855F * k)) * 3.1415927F;
        this.flag.render(matrices, vertexConsumer, light, overlay);

        matrices.pop();
    }

    // Made with Blockbench 4.5.2
    // Exported for Minecraft version 1.17+ for Yarn
    // Paste this class into your mod and generate all required imports
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        modelPartData.addChild("iron_stick",
                ModelPartBuilder.create().uv(0, 0).cuboid(-1.0F, -14.0F, -1.0F, 2.0F, 46.0F, 2.0F),
                ModelTransform.NONE);
        modelPartData.addChild("flag",
                ModelPartBuilder.create().uv(8, 0).cuboid(-8.0F, 26.0F, -0.5F, 7.0F, 6.0F, 1.0F),
                ModelTransform.NONE);
        return TexturedModelData.of(modelData, 64, 64);
    }
}

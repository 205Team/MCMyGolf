package net.fabricmc.mygolf.blockEntity.render;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.mygolf.RegisterBlocks;
import net.fabricmc.mygolf.blockEntity.FlagstickEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SignBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3f;

@Environment(EnvType.CLIENT)
public class FlagstickEntityRenderer implements BlockEntityRenderer<FlagstickEntity> {

    public FlagstickEntityRenderer(BlockEntityRendererFactory.Context ctx){

    }

    @Override
    public void render(FlagstickEntity flagstickEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        BlockState blockState = flagstickEntity.getCachedState();
        matrices.push();
        matrices.translate(0.5, 0.0, 0.5);
        float h = -((float) (blockState.get(SignBlock.ROTATION) * 360) / 16.0F);
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(h));
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(Blocks.GLASS.getDefaultState(), matrices, vertexConsumers, light, overlay);
        matrices.pop();
    }
}

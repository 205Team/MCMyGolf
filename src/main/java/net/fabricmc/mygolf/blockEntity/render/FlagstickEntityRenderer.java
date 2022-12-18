package net.fabricmc.mygolf.blockEntity.render;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.mygolf.blockEntity.FlagstickEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;

@Environment(EnvType.CLIENT)
public class FlagstickEntityRenderer implements BlockEntityRenderer<FlagstickEntity> {
    @Override
    public void render(FlagstickEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {

    }
}

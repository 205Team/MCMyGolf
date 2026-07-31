package net.fabricmc.mygolf.entity.model;

import com.google.common.collect.ImmutableList;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.util.math.MatrixStack;

public class GolfBallEntityModel extends EntityModel<GolfBallEntity> {
    private final ModelPart base;

    public GolfBallEntityModel(ModelPart modelPart) {
        this.base = modelPart.getChild("base");
    }
    
    // You can use BlockBench, make your model and export it to get this method for your entity model.
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData bone = modelPartData.addChild("base", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F, new Dilation(0.0F))
                .uv(1, 4).cuboid(-2.0F, 3.0F, -2.0F, 4.0F, 1.0F, 4.0F, new Dilation(0.0F))
                .uv(4, 2).cuboid(-2.0F, -4.0F, -2.0F, 4.0F, 1.0F, 4.0F, new Dilation(0.0F))
                .uv(1, 1).cuboid(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 1.0F, new Dilation(0.0F))
                .uv(0, 1).cuboid(-2.0F, -2.0F, 3.0F, 4.0F, 4.0F, 1.0F, new Dilation(0.0F))
                .uv(0, 4).cuboid(3.0F, -2.0F, -2.0F, 1.0F, 4.0F, 4.0F, new Dilation(0.0F))
                .uv(4, 5).cuboid(-4.0F, -2.0F, -2.0F, 1.0F, 4.0F, 4.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        return TexturedModelData.of(modelData, 32, 32);
    }

    @Override
    public void setAngles(GolfBallEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        ImmutableList.of(this.base).forEach((modelRenderer) -> {
            modelRenderer.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        });
    }
}

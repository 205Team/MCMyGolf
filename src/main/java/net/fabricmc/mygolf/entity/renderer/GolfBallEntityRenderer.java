package net.fabricmc.mygolf.entity.renderer;

import com.jme3.math.Quaternion;
import dev.lazurite.rayon.impl.bullet.math.Convert;
import net.fabricmc.mygolf.EntityTestingClient;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.entity.model.GolfBallEntityModel;
import net.fabricmc.mygolf.global.CommonStr;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class GolfBallEntityRenderer extends EntityRenderer<GolfBallEntity> {
    private final GolfBallEntityModel model;
    public GolfBallEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new GolfBallEntityModel(context.getPart(EntityTestingClient.MODEL_CUBE_LAYER));
        this.shadowRadius = 0.75f;
    }

    public void render(GolfBallEntity ballEntity, float yaw, float delta, MatrixStack matrixStack, VertexConsumerProvider multiBufferSource, int i) {
        final var rot = Convert.toMinecraft(ballEntity.getPhysicsRotation(new Quaternion(), delta));
        final var box = ballEntity.getBoundingBox();

        matrixStack.push();
        matrixStack.multiply(rot);
        matrixStack.translate(box.getXLength() * -0.5, box.getYLength() * -0.5, box.getZLength() * -0.5);

        final var vertexConsumer = multiBufferSource.getBuffer(model.getLayer(this.getTexture(ballEntity)));
        model.render(matrixStack, vertexConsumer, i, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStack.pop();

        super.render(ballEntity, yaw, delta, matrixStack, multiBufferSource, i);
    }

    @Override
    public boolean shouldRender(GolfBallEntity entity, Frustum frustum, double d, double e, double f) {
        return true;
    }
    @Override
    public Identifier getTexture(GolfBallEntity entity) {
        return new Identifier(CommonStr.modId, "textures/entity/cube/golf_ball_spawn_egg.png");
    }
}

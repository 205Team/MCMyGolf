package net.fabricmc.mygolf.entity.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.mygolf.MyGolfModClient;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.entity.model.GolfBallEntityModel;
import net.fabricmc.mygolf.global.CommonStr;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public class GolfBallEntityRenderer extends EntityRenderer<GolfBallEntity> {
    private final GolfBallEntityModel model;
    private static final Identifier BEACON_BEAM_TEXTURE = new Identifier("minecraft", "textures/entity/beacon_beam.png");

    public GolfBallEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new GolfBallEntityModel(context.getPart(MyGolfModClient.MODEL_CUBE_LAYER));
        this.shadowRadius = 0.2f;
    }

    public void render(GolfBallEntity ballEntity, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light) {
        super.render(ballEntity, yaw, tickDelta, matrixStack, vertexConsumers, light);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // 1. Render ball model
        renderModel(ballEntity, tickDelta, matrixStack, vertexConsumers, light);

        // 2. Render Vanishing Trail Ribbon (Option 2)
        renderTrail(ballEntity, tickDelta, matrixStack, vertexConsumers);

        // 3. Render arrow or beam
        double distanceSq = client.player.squaredDistanceTo(ballEntity);
        double minDistanceSq = 6.0 * 6.0;   // 10 blocks
        double maxDistanceSq = 256.0 * 256.0; // 256 blocks

        if (distanceSq <= minDistanceSq) {
            renderArrow(ballEntity, matrixStack, vertexConsumers, light);
        } else if (distanceSq <= maxDistanceSq)
            renderBeaconBeam(ballEntity, tickDelta, matrixStack, vertexConsumers);

    }

    private void renderModel(GolfBallEntity ballEntity, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light) {
        Quaternionf rotation = ballEntity.getPhysicsRotation(tickDelta);
        matrixStack.push();
        matrixStack.multiply(rotation);

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(model.getLayer(this.getTexture(ballEntity)));
        model.render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);

        matrixStack.pop();
    }

    private void renderTrail(GolfBallEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        if (entity.trailPositions.size() < 2) return;

        // Line buffer layer
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getLeash());

        // Interpolate visual position to offset world coordinates relative to matrixStack origin
        Vec3d entityPos = entity.getLerpedPos(tickDelta);

        matrices.push();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (int i = 0; i < entity.trailPositions.size() - 1; i++) {
            Vec3d current = entity.trailPositions.get(i).subtract(entityPos);
            Vec3d next = entity.trailPositions.get(i + 1).subtract(entityPos);

            // Calculate fading alpha from 1.0 (head) to 0.0 (tail)
            float alpha = 1.0f - ((float) i / entity.trailPositions.size());

            // Draw line segment
            buffer.vertex(matrix, (float) current.x, (float) current.y + 0.05f, (float) current.z)
                    .color(1.0f, 1.0f, 1.0f, alpha)
                    .light(255)
                    .next();

            buffer.vertex(matrix, (float) next.x, (float) next.y + 0.05f, (float) next.z)
                    .color(1.0f, 1.0f, 1.0f, alpha)
                    .light(255)
                    .next();
        }

        matrices.pop();
    }

    private void renderArrow(GolfBallEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        // Translate above the ball (unaffected by ball spin)
        double offsetY = entity.getHeight() + 0.35D;
        matrices.translate(0.0D, offsetY, 0.0D);

        // Billboard toward player camera
        matrices.multiply(this.dispatcher.getRotation());

        // Scale down world-space text
        float scale = 0.025F;
        matrices.scale(-scale, -scale, scale);

        TextRenderer textRenderer = this.getTextRenderer();
        String arrowStr = "▲";
        float xOffset = -textRenderer.getWidth(arrowStr) / 2.0F;

        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        textRenderer.draw(
                arrowStr,
                xOffset,
                0,
                0xFFFF00, // Yellow (RGB)
                false,
                positionMatrix,
                vertexConsumers,
                TextRenderer.TextLayerType.NORMAL,
                0,
                light
        );

        matrices.pop();
    }

    private void renderBeaconBeam(GolfBallEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        // RGB Color values (float 0.0f - 1.0f) -> e.g. White: {1.0f, 1.0f, 1.0f}, Red: {1.0f, 0.2f, 0.2f}
        float[] color = new float[]{1.0f, 1.0f, 1.0f};
        long time = entity.getWorld().getTime();

        matrices.push();
        // Center the beam over the ball
        matrices.translate(-0.5, 0.0, -0.5);

        // Vanilla beacon beam helper
        BeaconBlockEntityRenderer.renderBeam(
                matrices,
                vertexConsumers,
                BEACON_BEAM_TEXTURE,
                tickDelta,
                1.0F,                  // Height scale factor
                time,                  // World time (animates beam movement)
                0,                     // Y-offset starting height
                256,                   // Total beam height (up to build limit)
                color,
                0.15F,                 // Inner beam radius
                0.25F                  // Outer beam radius
        );

        matrices.pop();
    }

    @Override
    public boolean shouldRender(GolfBallEntity entity, Frustum frustum, double d, double e, double f) {
        return true;
    }

    @Override
    public Identifier getTexture(GolfBallEntity entity) {
        return new Identifier(CommonStr.modId, "textures/entity/cube/golf_ball.png");
    }
}

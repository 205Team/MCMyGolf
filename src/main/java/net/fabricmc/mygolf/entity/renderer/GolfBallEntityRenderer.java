package net.fabricmc.mygolf.entity.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.mygolf.MyGolfModClient;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.entity.model.GolfBallEntityModel;
import net.fabricmc.mygolf.global.CommonStr;
import net.fabricmc.mygolf.items.GolfClubItem;
import net.fabricmc.mygolf.physics.GolfPhysicsEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

@Environment(EnvType.CLIENT)
public class GolfBallEntityRenderer extends EntityRenderer<GolfBallEntity> {
    private final GolfBallEntityModel model;
    public static final Identifier BLANK_BEAM_TEXTURE = new Identifier(CommonStr.modId, "textures/entity/blank_beam.png");
    private static final Identifier FLAG_ICON_TEXTURE = new Identifier(CommonStr.modId, "textures/item/flag_overlay.png");
    private static final Identifier ARROW_TEXTURE = new Identifier(CommonStr.modId, "textures/misc/arrow_bw.png");
    private static final Identifier NUM_FONT = new Identifier(CommonStr.modId, "num_font");
    private static final double SLOW_SPEED_THRESHOLD_SQ = 0.1 * 0.1; // Ball speed upper limit for beam rendering
    private static final int MAX_TRAJECTORY_STEPS = 15; // * 0.05 seconds of trajectory predicted
    private static float lastLoft = Float.NaN;
    private static long loftChangeTime = 0;
    private static final long DISPLAY_DURATION_MS = 1100; // 3 seconds total duration
    private static final long FADE_DURATION_MS = 500;

    public GolfBallEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new GolfBallEntityModel(context.getPart(MyGolfModClient.MODEL_CUBE_LAYER));
        this.shadowRadius = 0.15F;
    }

    @Override
    public void render(GolfBallEntity ballEntity, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light) {

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        // Render 2D textured aiming arrow
        renderAimArrow(ballEntity, player, tickDelta, matrixStack, vertexConsumers, light);
        // Render trajectory preview while charging club
        renderTrajectoryPreview(ballEntity, player, tickDelta, matrixStack, vertexConsumers, light);

        matrixStack.push();

        // Render ball jumping animation
        renderJumpy(ballEntity, tickDelta, matrixStack);

        // Render ball model
        renderModel(ballEntity, tickDelta, matrixStack, vertexConsumers, light);

        matrixStack.pop();

        // Super
        super.render(ballEntity, yaw, tickDelta, matrixStack, vertexConsumers, light);

        // Render arrow or beam
        double distanceSq = player.squaredDistanceTo(ballEntity);
        if (distanceSq <= GolfBallEntity.MIN_DISTANCE_SQ) {
        } else if (distanceSq <= GolfBallEntity.MID_DISTANCE_SQ) {
            renderCone(ballEntity, matrixStack, vertexConsumers, light, tickDelta);
        } else if (distanceSq <= GolfBallEntity.MAX_DISTANCE_SQ) {
            if (ballEntity.getVelocity().lengthSquared() <= SLOW_SPEED_THRESHOLD_SQ) {
                renderBeaconBeam(ballEntity, tickDelta, matrixStack, vertexConsumers);
            }
        }

        // Render flag icon if goaled
        if (ballEntity.isGoaled()) {
            renderFlagIcon(matrixStack, vertexConsumers, light);
        }

    }

    private void renderModel(GolfBallEntity ballEntity, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light) {
        float radius = GolfBallEntity.BALL_HEIGHT / 2;

        matrixStack.translate(0.0D, radius, 0.0D);

        Quaternionf rotation = ballEntity.getPhysicsRotation(tickDelta);
        matrixStack.multiply(rotation);

        int colorInt = ballEntity.getColor();
        float red   = (float) (colorInt >> 16 & 255) / 255.0F;
        float green = (float) (colorInt >> 8 & 255)  / 255.0F;
        float blue  = (float) (colorInt & 255)       / 255.0F;

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(model.getLayer(this.getTexture(ballEntity)));
        model.render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV, red, green, blue, 1.0F);
    }

    private void renderJumpy(GolfBallEntity ballEntity, float tickDelta, MatrixStack matrixStack) {

        float remainingTicks = Math.max(0.0F, (float) ballEntity.getJumpTicks() - tickDelta);
        if (remainingTicks > 0.0F) {
            float hopDuration = 8.0F;

            // Modulo prevents negative progress when ticks exceed 8
            float ticksInCurrentHop = remainingTicks % hopDuration;
            if (ticksInCurrentHop == 0.0F) {
                ticksInCurrentHop = hopDuration; // Handle exact boundary frames
            }

            float progress = 1.0F - (ticksInCurrentHop / hopDuration);

            float maxHop = ballEntity.getHopHeight();
            float yOffset = MathHelper.sin(progress * (float) Math.PI) * maxHop;

            matrixStack.translate(0.0D, yOffset, 0.0D);
        }

    }

    private void renderCone(GolfBallEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float tickDelta) {
        matrices.push();

        // 1. Translate above the ball in world space
        double offsetY = entity.getHeight() + 0.25D + Math.sin((entity.age + tickDelta) * 0.18D) * 0.1D;
        matrices.translate(0.0D, offsetY, 0.0D);

        // 2. Continuous rotation on Y-axis
        float animationAngle = (entity.age + tickDelta) * 4.0F;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(animationAngle));

        // 3. Setup VertexConsumer
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        float radius = 0.10F;
        float height = 0.15F;
        int segments = 6; // Increase for smoother cone, decrease for performance/blocky style
        int r = 255, g = 70, b = 70, a = 255; // Red color

        // 4. Render Cone Side Walls (Tip at 0,0,0 pointing DOWN)
        for (int i = 0; i < segments; i++) {
            double angle1 = (2 * Math.PI / segments) * i;
            double angle2 = (2 * Math.PI / segments) * (i + 1);

            float x1 = (float) (Math.cos(angle1) * radius);
            float z1 = (float) (Math.sin(angle1) * radius);
            float x2 = (float) (Math.cos(angle2) * radius);
            float z2 = (float) (Math.sin(angle2) * radius);

            // Tip -> Point 1 -> Point 2 -> Point 2 (Duplicated 4th vertex for quad buffer)
            buffer.vertex(posMatrix, 0, 0, 0).color(r, g, b, a).next();
            buffer.vertex(posMatrix, x1, height, z1).color(r, g, b, a).next();
            buffer.vertex(posMatrix, x2, height, z2).color(r, g, b, a).next();
            buffer.vertex(posMatrix, x2, height, z2).color(r, g, b, a).next();
        }

        // 5. Render Top Circular Cap
        for (int i = 0; i < segments; i++) {
            double angle1 = (2 * Math.PI / segments) * i;
            double angle2 = (2 * Math.PI / segments) * (i + 1);

            float x1 = (float) (Math.cos(angle1) * radius);
            float z1 = (float) (Math.sin(angle1) * radius);
            float x2 = (float) (Math.cos(angle2) * radius);
            float z2 = (float) (Math.sin(angle2) * radius);

            // Center -> Point 2 -> Point 1 -> Point 1
            buffer.vertex(posMatrix, 0, height, 0).color(r, g, b, a).next();
            buffer.vertex(posMatrix, x2, height, z2).color(r, g, b, a).next();
            buffer.vertex(posMatrix, x1, height, z1).color(r, g, b, a).next();
            buffer.vertex(posMatrix, x1, height, z1).color(r, g, b, a).next();
        }

        matrices.pop();
    }

    private void renderBeaconBeam(GolfBallEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        int rgb = entity.getColor();
        float[] color = new float[] {
                ((rgb >> 16) & 0xFF) / 255.0f, // Red
                ((rgb >> 8) & 0xFF) / 255.0f,  // Green
                (rgb & 0xFF) / 255.0f          // Blue
        };
        long time = entity.getWorld().getTime();
        double heightOffset = GolfBallEntity.BALL_HEIGHT;

        matrices.push();
        // Center the beam over the ball
        matrices.translate(-0.5, heightOffset, -0.5);

        // Vanilla beacon beam helper
        BeaconBlockEntityRenderer.renderBeam(
                matrices,
                vertexConsumers,
                BLANK_BEAM_TEXTURE,
                tickDelta,
                1.0F,                  // Height scale factor
                time,                  // World time (animates beam movement)
                0,                     // Y-offset starting height
                256,                   // Total beam height (up to build limit)
                color,
                0.07F,                 // Inner beam radius
                0.18F                  // Outer beam radius
        );

        matrices.pop();
    }

    private void renderTrajectoryPreview(GolfBallEntity ballEntity, PlayerEntity player, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light) {
        // Check if player is actively holding/charging an item
        if (!player.isUsingItem()) return;

        ItemStack activeStack = player.getActiveItem();
        if (!(activeStack.getItem() instanceof GolfClubItem)) return;

        // Verify this entity is the closest ball to the player
        GolfBallEntity closestBall = GolfBallEntity.getClosestBall(player.getWorld(), player, GolfBallEntity.MIN_RADIUS, true);
        if (closestBall != ballEntity) return;

        // Calculate charge power ratio
        int heldTicks = activeStack.getMaxUseTime() - player.getItemUseTimeLeft();
        if (heldTicks < GolfClubItem.MIN_CHARGE_TICKS) return;

        // Sawtooth Ramp & 3-Loop Capping matching onStoppedUsing
        int chargeTicks = heldTicks - GolfClubItem.MIN_CHARGE_TICKS;
        float currentLoft = GolfClubItem.getSelectedLoft(activeStack);
        int maxAllowedTicks = GolfClubItem.MAX_CHARGE_TICKS * GolfClubItem.MAX_LOOPS;

        float powerRatio;
        if (chargeTicks >= maxAllowedTicks) {
            // Capped: Exceeded max loops -> force power to 0.0f
            powerRatio = 0.0f;
        } else {
            // Sawtooth Ramp: Continuously resets from 0.0f to 1.0f on each loop completion
            powerRatio = GolfClubItem.calculatePowerRatio(chargeTicks, currentLoft);
        }

        // Hide trajectory if power is 0 (e.g. capped out or at exact loop start)
        if (powerRatio <= 0.0f) return;

        // --- PHYSICS SIMULATION ---
        Vec3d launchDir = Vec3d.fromPolar(currentLoft, player.getYaw());
        double finalSpeed = powerRatio * GolfClubItem.MAX_SHOT_POWER;
        Vec3d impulse = launchDir.multiply(finalSpeed);
        Vec3d initialVelocity = GolfPhysicsEngine.applyImpulse(
                ballEntity.getVelocity(),
                impulse,
                GolfPhysicsEngine.Config.STANDARD_BALL.mass()
        );

        Vec3d initialSpin = Vec3d.ZERO;
        if (GolfBallEntity.ENABLE_MAGNUS_EFFECT) {
            float yawRad = (float) Math.toRadians(player.getYaw());
            double loftMagnitude = Math.abs(currentLoft);
            double backspinIntensity = finalSpeed * Math.sin(Math.toRadians(loftMagnitude)) * 1.5;
            initialSpin = new Vec3d(-Math.cos(yawRad) * backspinIntensity, 0.0, -Math.sin(yawRad) * backspinIntensity);
        }

        List<Vec3d> trajectoryPoints = GolfPhysicsEngine.predictTrajectory(
                player.getWorld(),
                ballEntity,
                ballEntity.getLerpedPos(tickDelta),
                initialVelocity,
                initialSpin,
                GolfPhysicsEngine.Config.STANDARD_BALL,
                MAX_TRAJECTORY_STEPS
        );

        // --- SPHERE RENDERING ---
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(this.getTexture(ballEntity)));
        Vec3d entityPos = ballEntity.getLerpedPos(tickDelta);

        int color = ballEntity.getColor();
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        float radius = GolfBallEntity.BALL_HEIGHT / 2.0f;
        int totalPoints = trajectoryPoints.size();

        for (int i = 1; i < totalPoints; i++) {
            Vec3d point = trajectoryPoints.get(i);
            Vec3d offset = point.subtract(entityPos);

            float progress = (float) i / totalPoints;

            // Quadratic drop-off: stays brighter near the start, fades out fast towards the end
            float alpha = (float) Math.pow(1.0F - progress, 2.0) * 0.7F;

            if (alpha <= 0.01F) continue; // Skip rendering practically invisible spheres

            matrixStack.push();
            matrixStack.translate(offset.x, offset.y + radius, offset.z);
            matrixStack.scale(0.3F, 0.3F, 0.3F);

            this.model.render(matrixStack, buffer, 0xF000F0, OverlayTexture.DEFAULT_UV, red, green, blue, alpha);

            matrixStack.pop();
        }
    }

    private void renderAimArrow(GolfBallEntity ballEntity, PlayerEntity player, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (player.isUsingItem()) return;

        ItemStack heldStack = player.getMainHandStack();
        if (!(heldStack.getItem() instanceof GolfClubItem)) {
            heldStack = player.getOffHandStack();
            if (!(heldStack.getItem() instanceof GolfClubItem)) return;
        }

        // Only render for the ball closest to the player
        GolfBallEntity closestBall = GolfBallEntity.getClosestBall(player.getWorld(), player, GolfBallEntity.MIN_RADIUS, true);
        if (closestBall != ballEntity) return;

        // Tilt arrow upward according to club loft angle
        float loft = GolfClubItem.getSelectedLoft(heldStack);

        matrices.push();

        // 1. Position slightly above ground at the base of the ball to prevent Z-fighting
        matrices.translate(0.0, 0.02, 0.0);

        // 2. Rotate arrow to align with the player's aiming direction (Yaw)
        // -player.getYaw() aligns +Z forward along the shot angle
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-player.getYaw()));

        // 3. Obtain VertexConsumer using translucent or cutout layer (no backface culling)
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(ARROW_TEXTURE));

        // Arrow dimensions (in blocks)
        float width = 0.5f;
        float length = 0.5f;
        float halfWidth = width / 2.0f;
        float startOffset = 0.25f; // Offset slightly in front of ball center

        double loftRad = Math.toRadians(loft);
        float cosLoft = (float) Math.cos(loftRad);

        // Shadow
        float shadowStartOffset = startOffset * cosLoft;
        float shadowEndOffset = (startOffset + length) * cosLoft;
        matrices.push();
        MatrixStack.Entry shadowEntry = matrices.peek();
        drawVertex(shadowEntry, buffer, -halfWidth, 0.0f, shadowEndOffset, 0.0f, 0.0f, 0, 0, 0, 95, light);
        drawVertex(shadowEntry, buffer,  halfWidth, 0.0f, shadowEndOffset, 1.0f, 0.0f, 0, 0, 0, 95, light);
        drawVertex(shadowEntry, buffer,  halfWidth, 0.0f, shadowStartOffset,          1.0f, 1.0f, 0, 0, 0, 95, light);
        drawVertex(shadowEntry, buffer, -halfWidth, 0.0f, shadowStartOffset,          0.0f, 1.0f, 0, 0, 0, 95, light);
        matrices.pop();

        // Arrow
        float yOffset = GolfBallEntity.BALL_HEIGHT / 2;
        float t = MathHelper.clamp(-loft / 60, 0.0f, 1.0f);
        int redR = 220,  redG = 96, redB = 96;  // Grass Green (#4CAF50)
        int blueR  = 100, blueG  = 160, blueB  = 255; // Sky Blue    (#64B4FF)
        int r = (int) (redR + t * (blueR - redR));
        int g = (int) (redG + t * (blueG - redG));
        int b = (int) (redB + t * (blueB - redB));
        int alpha = 180; // Opacity (0-255)
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(loft));
        MatrixStack.Entry arrowEntry = matrices.peek();
        drawVertex(arrowEntry, buffer, -halfWidth, yOffset, startOffset + length, 0.0f, 0.0f, r, g, b, alpha, light); // Bottom-Left
        drawVertex(arrowEntry, buffer,  halfWidth, yOffset, startOffset + length, 1.0f, 0.0f, r, g, b, alpha, light); // Bottom-Right
        drawVertex(arrowEntry, buffer,  halfWidth, yOffset, startOffset,          1.0f, 1.0f, r, g, b, alpha, light); // Top-Right
        drawVertex(arrowEntry, buffer, -halfWidth, yOffset, startOffset,          0.0f, 1.0f, r, g, b, alpha, light); // Top-Left

        // Text
        long currentTime = System.currentTimeMillis();
        if (loft != lastLoft) {
            lastLoft = loft;
            loftChangeTime = currentTime;
        }
        long elapsed = currentTime - loftChangeTime;
        if (elapsed < DISPLAY_DURATION_MS) {
            // 1. Calculate Fade Alpha
            float alphaFactor = 1.0f;
            long fadeStart = DISPLAY_DURATION_MS - FADE_DURATION_MS;
            if (elapsed > fadeStart) {
                alphaFactor = 1.0f - ((float) (elapsed - fadeStart) / FADE_DURATION_MS);
            }
            int textAlpha = (int) (alphaFactor * 255);
            if (textAlpha > 4) {
                int textColor = (textAlpha << 24) | 0xE1E9E1; // White text with dynamic alpha
                // 2. Position Text on Arrow Surface
                matrices.push();
                float midZ = startOffset + 0.11f;
                matrices.translate(0.0D, yOffset + 0.002D, midZ);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
                float textScale = 0.03f;
                matrices.scale(textScale, -textScale, textScale);
                // 3. Center and Draw Text
                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                String loftText = Math.round(Math.abs(loft)) + "°";
                float xOffset = 2.0f;
                float yTextOffset = -4.5f;
                Text formattedText = Text.literal(loftText)
                        .setStyle(Style.EMPTY.withFont(NUM_FONT));
                textRenderer.draw(
                        formattedText,
                        xOffset,
                        yTextOffset,
                        textColor,
                        false,
                        matrices.peek().getPositionMatrix(),
                        vertexConsumers,
                        TextRenderer.TextLayerType.NORMAL,
                        0,
                        light
                );
                matrices.pop();
            }
        }
        matrices.pop();

        matrices.pop();
    }

    private void renderFlagIcon(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        // 1. ROTATE FIRST: Align transformation axes to camera view
        // Now: +X = Screen Right, +Y = Screen Up, -Z = Toward Camera
        matrices.multiply(this.dispatcher.getRotation());

        // 2. TRANSLATE IN CAMERA/SCREEN SPACE
        double ballRadius = GolfBallEntity.BALL_HEIGHT; // Adjust based on your ball size
        double offsetFront = -ballRadius - 0.01; // Push toward camera in front of ball surface
        matrices.translate(0.0D, 0.05D, offsetFront);

        // 3. SCALE: Keep icon small relative to the ball
        float scale = 0.5F;
        matrices.scale(scale, scale, scale);

        // 4. DRAW QUAD
        int fullbright = 200;
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(FLAG_ICON_TEXTURE));
        MatrixStack.Entry entry = matrices.peek();

        drawVertex(entry, buffer, -0.5F,  0.5F, 0.0F, 1.0F, 0.0F, 255, 255, 255, 255, fullbright); // Top-Left
        drawVertex(entry, buffer,  0.5F,  0.5F, 0.0F, 0.0F, 0.0F, 255, 255, 255, 255, fullbright); // Top-Right
        drawVertex(entry, buffer,  0.5F, -0.5F, 0.0F, 0.0F, 1.0F, 255, 255, 255, 255, fullbright); // Bottom-Right
        drawVertex(entry, buffer, -0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 255, 255, 255, 255, fullbright); // Bottom-Left

        matrices.pop();
    }

    private static void drawVertex(MatrixStack.Entry entry, VertexConsumer buffer, float x, float y, float z, float u, float v, int r, int g, int b, int alpha, int light) {
        buffer.vertex(entry.getPositionMatrix(), x, y, z)
                .color(r, g, b, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry.getNormalMatrix(), 0.0F, 1.0F, 0.0F)
                .next();
    }

    /// Overrides
    @Override
    public boolean shouldRender(GolfBallEntity entity, Frustum frustum, double d, double e, double f) {
        return true;
    }

    @Override
    public Identifier getTexture(GolfBallEntity entity) {
        return new Identifier(CommonStr.modId, "textures/entity/cube/golf_ball.png");
    }

    @Override
    protected boolean hasLabel(GolfBallEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null) {
            return false;
        }

        boolean isCharging = player.isUsingItem() && player.getActiveItem().getItem() instanceof GolfClubItem;
        if (isCharging) {
            return false;
        }

        GolfBallEntity closestBall = GolfBallEntity.getClosestBall(player.getWorld(),player,GolfBallEntity.MIN_RADIUS, false);

        return entity == closestBall;
    }

    @Override
    protected void renderLabelIfPresent(GolfBallEntity entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        double squaredDistance = this.dispatcher.getSquaredDistanceToCamera(entity);
        if (squaredDistance > 4096.0D) {
            return;
        }

        MutableText customLabel = entity.hasCustomName()
                ? entity.getCustomName().copy()
                : Text.translatable("entity.mygolf.golf_ball_entity");
        if (entity.hasOwner()) {
            PlayerEntity localPlayer = MinecraftClient.getInstance().player;
            if (localPlayer != null && entity.isOwner(localPlayer)) {
                customLabel.setStyle(customLabel.getStyle().withColor(0x82e782));//green
            } else {
                customLabel.setStyle(customLabel.getStyle().withColor(0xD96C2A));//red
            }
        } else {
            customLabel.setStyle(customLabel.getStyle().withColor(0xC5E2E4));//mint
        }

        int hits = entity.getHitCount();
        int hitColor = entity.isGoaled() ? 0xECA758 : 0xC5E2E4;//gold/mint
        String prefix = "[";
        String suffix = entity.isGoaled() ? "]⛳" : "]";
        Text dotSeparator = Text.literal("•").setStyle(Style.EMPTY.withColor(0xFFFFFF));
        Text counterText = Text.literal(prefix + hits + suffix).setStyle(Style.EMPTY.withColor(hitColor));
        Text hitText = Text.empty().append(dotSeparator).append(counterText);


        Text finalText = customLabel.append(hitText);

        matrices.push();

        // Position label height above the ball
        double offsetY = entity.getHeight() + 0.12D;
        matrices.translate(0.0D, offsetY, 0.0D);

        matrices.multiply(this.dispatcher.getRotation());

        // Custom text scale (Vanilla default is -0.025F)
        float scale = -0.010F;
        matrices.scale(scale, scale, Math.abs(scale));

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        float backgroundAlpha = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
        int backgroundColor = (int) (backgroundAlpha * 255.0F) << 24;

        TextRenderer textRenderer = this.getTextRenderer();
        float xOffset = -textRenderer.getWidth(finalText) / 2.0F;

        textRenderer.draw(
                finalText,
                xOffset,
                0,
                -1,
                false,
                matrix4f,
                vertexConsumers,
                TextRenderer.TextLayerType.SEE_THROUGH,
                backgroundColor,
                light
        );

        // 4. Render label text
        textRenderer.draw(
                finalText,
                xOffset,
                0,
                0xDFFFFFFF, // White text with full opacity
                false,
                matrix4f,
                vertexConsumers,
                TextRenderer.TextLayerType.POLYGON_OFFSET,
                0,
                light
        );

        matrices.pop();
    }
}

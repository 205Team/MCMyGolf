package net.fabricmc.mygolf.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.mygolf.physics.GolfPhysicsEngine;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Quaternionf;

import java.util.LinkedList;

public class GolfBallEntity extends Entity {

    private Vec3d spinVector = Vec3d.ZERO;
    private final GolfPhysicsEngine.Config physicsConfig = GolfPhysicsEngine.Config.STANDARD_BALL;
    ///被击打次数
    private int hitCount = 0;
    private boolean isGoaled = false;
    private float lastRoll = 0.0F;
    private float roll = 0.0F;
    private static final int MAX_TRAIL_POINTS = 16; // Length of the trail
    private static final double SPEED_THRESHOLD_SQ = 0.25;

    // Client-side list of trail points. Not synchronized to server.
    @Environment(EnvType.CLIENT)
    public final LinkedList<Vec3d> trailPositions = new LinkedList<>();

    public GolfBallEntity(EntityType<? extends GolfBallEntity> entityType, World level) {
        super(entityType, level);
    }

    public void applyImpulse(Vec3d impulse) {
        this.setVelocity(GolfPhysicsEngine.applyImpulse(
                this.getVelocity(), impulse, physicsConfig.mass()
        ));
    }

    public Vec3d getSpin() {
        return this.spinVector;
    }

    public void setSpin(Vec3d spin) {
        this.spinVector = spin;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void incrementHitCount() {
        if (!this.isGoaled)
            hitCount++;
    }

    public boolean isGoaled() {
        return isGoaled;
    }

    public void setGoaled() {
        this.isGoaled = true;
    }

    public void restHitCount() {
        hitCount = 0;
        this.isGoaled = true;
    }

    //Calculates smooth client-side rolling rotation using JOML Quaternionf.
    public Quaternionf getPhysicsRotation(float tickDelta) {
        // Smoothly interpolate rolling angle across frames (prevents jitter)
        float interpolatedRoll = MathHelper.lerp(tickDelta, this.lastRoll, this.roll);

        Vec3d vel = this.getVelocity();

        // If moving, rotate in the direction of the velocity vector
        if (vel.lengthSquared() > 0.0001) {
            float yaw = (float) Math.atan2(vel.z, vel.x);
            return new Quaternionf()
                    .rotateY(-yaw)
                    .rotateZ(interpolatedRoll);
        }

        return new Quaternionf().rotateZ(interpolatedRoll);
    }

    @Override
    public void tick() {
        super.tick();

        // Client-side visual rotation accumulator
        if (this.getWorld().isClient()) {
            this.lastRoll = this.roll;
            double speed = this.getVelocity().length();
            double ballRadius = GolfPhysicsEngine.Config.STANDARD_BALL.radius(); // meters

            // Accumulate rotation angle (theta = distance / radius)
            this.roll += (float) (speed / ballRadius);

            // Handle trail
            if (this.getVelocity().lengthSquared() > SPEED_THRESHOLD_SQ) {
                // Add current position to history
                trailPositions.addFirst(this.getPos());
                if (trailPositions.size() > MAX_TRAIL_POINTS) {
                    trailPositions.removeLast();
                }
            } else if (!trailPositions.isEmpty()) {
                // Fade out trail when ball slows down
                trailPositions.removeLast();
            }
            return;
        }

        // Server-side physics
        // 1. Pack current entity state
        GolfPhysicsEngine.State currentState = new GolfPhysicsEngine.State(
                this.getPos(),
                this.getVelocity(),
                this.spinVector,
                this.isOnGround()
        );

        // 2. Delegate ALL kinematic math to the isolated physics engine
        GolfPhysicsEngine.State newState = GolfPhysicsEngine.step(
                this.getWorld(), this, currentState, this.physicsConfig
        );

        // 3. Apply results back to Minecraft Entity
        this.setPosition(newState.pos());
        this.setVelocity(newState.vel());
        this.setSpin(newState.spin());
        this.setOnGround(newState.onGround());

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            // Keeps the chunk loaded at the ball's current position while it is moving
            if (this.getVelocity().lengthSquared() > 0.01) {
                BlockPos pos = this.getBlockPos();

                // Force load the 3x3 chunks around the ball
                serverWorld.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
            } else {
                // Once the ball comes to a complete rest, release the forced chunk
                BlockPos pos = this.getBlockPos();
                serverWorld.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
            }
        }

        this.velocityModified = true;
    }

    @Override
    public void setPosition(double x, double y, double z) {
        this.setPos(x, y, z);
        //this.setBoundingBox(this.calculateBoundingBox().offset(0D, -this.getBoundingBox().getYLength() / 2, 0D)); //碰撞箱偏移
    }

//    @Override
//    public boolean isSilent() {
//        return true;
//    }

    // Protect from environmental destruction (fire, cactus, explosions, etc.)
    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        if (damageSource.isOf(DamageTypes.IN_FIRE) ||
                damageSource.isOf(DamageTypes.ON_FIRE) ||
                damageSource.isOf(DamageTypes.CACTUS) ||
                damageSource.isOf(DamageTypes.EXPLOSION) ||
                damageSource.isOf(DamageTypes.DROWN)) {
            return true;
        }

        return super.isInvulnerableTo(damageSource);
    }

    @Override
    protected void fall(double d, boolean bl, BlockState blockState, BlockPos blockPos) {
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        // Safety check: Release forced chunk if the ball is destroyed or despawned
        if (!this.getWorld().isClient() && this.getWorld() instanceof ServerWorld serverWorld) {
            BlockPos pos = this.getBlockPos();
            serverWorld.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
        }
    }

    // --- Required Entity Overrides ---

    @Override
    protected void initDataTracker() {
        // Register entity data parameters here if needed for client-side rendering
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        // Load custom fields (spin, mass) from NBT here
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        // Save custom fields to NBT here
    }
}
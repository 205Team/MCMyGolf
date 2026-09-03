package net.fabricmc.mygolf.entity;

import net.fabricmc.mygolf.global.ModConfig;
import net.fabricmc.mygolf.items.GolfBall;
import net.fabricmc.mygolf.physics.GolfPhysicsEngine;
import net.fabricmc.mygolf.registry.RegisterItems;
import net.fabricmc.mygolf.tools.DebugUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GolfBallEntity extends Entity {

    private final GolfPhysicsEngine.Config physicsConfig = GolfPhysicsEngine.Config.STANDARD_BALL;
    private ChunkPos lastForcedChunk = null;    // Last forced load chunk for traveling ball
    public final Quaternionf prevWorldRotation = new Quaternionf(); // For rendering rotation
    public final Quaternionf worldRotation = new Quaternionf(); // For rendering rotation
    public static boolean ENABLE_MAGNUS_EFFECT = ModConfig.INSTANCE.enableMagnusEffect;  // Toggle for Magnus effect(curveballs)
    private static final double SPEED_THRESHOLD_SQ = 0.5; // Minimum speed squared to spawn trail
    private static final double FADE_WINDOW_SQ = 0.25;     // Speed range above threshold over which fading occurs
    public static final EntityDimensions BALL_DIMENSIONS = EntityDimensions.fixed(0.25f, 0.25f); // Minecraft's collision box dimensions of ball
    public static final float BALL_HEIGHT = BALL_DIMENSIONS.height; // Jumpy animation ball height
    public static final float DROP_THRESHOLD = 1.5F * BALL_HEIGHT;  // Ball item-drop height
    private static final TrackedData<Vector3f> SPIN_VECTOR = DataTracker.registerData(GolfBallEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Boolean> IS_GOALED = DataTracker.registerData(GolfBallEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> HIT_COUNT = DataTracker.registerData(GolfBallEntity.class, TrackedDataHandlerRegistry.INTEGER); // Counts how many times the ball has got hit
    private static final TrackedData<Integer> COLOR = DataTracker.registerData(GolfBallEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> JUMP_TICKS = DataTracker.registerData(GolfBallEntity.class, TrackedDataHandlerRegistry.INTEGER);    // DataTracker keys to sync hit response across the network
    private static final TrackedData<Float> HOP_HEIGHT = DataTracker.registerData(GolfBallEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Optional<UUID>> OWNER_UUID = DataTracker.registerData(GolfBallEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Boolean> IS_SLEEPING = DataTracker.registerData(GolfBallEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    // Base radiuses in blocks
    public static final double MIN_RADIUS = 3.0D;   // Close range (Name tag / direct focus)
    public static final double MID_RADIUS = 16.0D;   // Medium range (3D Arrow)
    public static final double MAX_RADIUS = 256.0D;  // Far range (Beacon Beam)
    // Pre-squared distances for fast performance checks (r^2)
    public static final double MIN_DISTANCE_SQ = MIN_RADIUS * MIN_RADIUS;
    public static final double MID_DISTANCE_SQ = MID_RADIUS * MID_RADIUS;
    public static final double MAX_DISTANCE_SQ = MAX_RADIUS * MAX_RADIUS;
    private int mountCooldown = 0;  // Vehicle mounting wait time
    private long startTime = 0L;    // First hit time
    private int sleepCheckTimer = 0;    // For sleep state
    private int stuckTicks = 0;
    private Vec3d lastSafePos = Vec3d.ZERO;

    public GolfBallEntity(EntityType<? extends GolfBallEntity> entityType, World level) {
        super(entityType, level);
        this.noClip = true;
    }

    // --- Getters & Setters  ---
    public int getHitCount() {
        return this.dataTracker.get(HIT_COUNT);
    }
    public void setHitCount(int count) {
        this.dataTracker.set(HIT_COUNT, count);
    }
    public void incrementHitCount() {
        if (!this.isGoaled()){
            this.setHitCount(this.getHitCount() + 1);
            if (this.getHitCount() == 0 && this.startTime == 0L) {
                this.startTime = this.getWorld().getTime();
            }
        }
    }

    public Vec3d getSpin() {
        Vector3f vec = this.dataTracker.get(SPIN_VECTOR);
        return new Vec3d(vec.x(), vec.y(), vec.z());
    }
    public void setSpin(Vec3d spin) {
        Vec3d current = this.getSpin();
        // Only issue a DataTracker network sync packet if the spin delta > 0.001
        if (current.squaredDistanceTo(spin) > 0.000001D) {
            this.dataTracker.set(SPIN_VECTOR, new Vector3f((float) spin.x, (float) spin.y, (float) spin.z));
        }
    }

    public boolean isGoaled() {
        return this.dataTracker.get(IS_GOALED);
    }
    public void setGoaled(boolean goaled) {
        this.dataTracker.set(IS_GOALED, goaled);
    }

    public int getJumpTicks() { return this.dataTracker.get(JUMP_TICKS); }
    public void setJumpTicks(int ticks) { this.dataTracker.set(JUMP_TICKS, ticks); }

    public float getHopHeight() { return this.dataTracker.get(HOP_HEIGHT); }
    public void setHopHeight(float height) { this.dataTracker.set(HOP_HEIGHT, height); }

    public int getColor() {
        return this.dataTracker.get(COLOR);
    }
    public void setColor(int color) {
        this.dataTracker.set(COLOR, color);
    }

    public Optional<UUID> getOwnerUuid() {return this.dataTracker.get(OWNER_UUID);}
    public void setOwnerUuid(@Nullable UUID uuid) {this.dataTracker.set(OWNER_UUID, Optional.ofNullable(uuid));}
    public boolean hasOwner() {return this.getOwnerUuid().isPresent();}
    public boolean isOwner(PlayerEntity player) {return this.getOwnerUuid().map(uuid -> uuid.equals(player.getUuid())).orElse(true);}

    public boolean isSleeping() {return this.dataTracker.get(IS_SLEEPING);}
    public void setSleeping(boolean isSleeping) {this.dataTracker.set(IS_SLEEPING, isSleeping);}

    public long getStartTime() {return this.startTime;}
    public void setStartTime(long startTime) {this.startTime = startTime;}

    @Override
    public void tick() {
        super.tick();

        // 1. Tick state timers
        this.tickTimers();

        // 2. Handle Vehicle States
        if (this.handleVehicleState()) {
            this.wakeUp();
            return;
        }

        // 3. Sleeping State Bypass
        if (this.isSleeping()) {
            this.sleeper();
            return;
        }

        // 4. Clipping Detection & Emergency Recovery
        if (this.checkAndHandleClipping()) {
            return;
        }

        // 5. Deterministic Physics Execution (Both Client and Server)
        this.tickPhysicsEngine();

        // 6. Side-Specific Post-Physics Logic
        if (this.getWorld().isClient()) {
            this.tickClientVisuals();
        } else {
            this.tickServerLogic();
        }

        //Debug
        if(!this.getWorld().isClient())
            DebugUtil.logOnChange("ball_" + this.getId() + "_isSleeping", this.isSleeping());
    }

    private void tickTimers() {
        if (this.mountCooldown > 0) {
            this.mountCooldown--;
        }
        if (this.getJumpTicks() > 0) {
            this.setJumpTicks(this.getJumpTicks() - 1);
        }
        if (this.getJumpTicks() == 0 && this.getHopHeight() > 0.0F) {
            this.setHopHeight(0.0F);
        }
    }

    private boolean handleVehicleState() {
        // Case A: Ball is currently mounted
        if (this.hasVehicle()) {
            this.setSpin(Vec3d.ZERO);

            if (this.getWorld().isClient()) {
                Entity vehicle = this.getVehicle();
                this.prevWorldRotation.set(this.worldRotation);

                float yawRad = (float) Math.toRadians(-vehicle.getYaw());
                float pitchRad = (float) Math.toRadians(vehicle.getPitch());

                this.worldRotation.identity()
                        .rotateY(yawRad)
                        .rotateX(pitchRad);
            }
            return true;
        }

        // Case B: Ball is unmounted – Scan for new vehicles (Server side only)
        if (!this.getWorld().isClient() && this.mountCooldown == 0) {
            List<Entity> vehicles = this.getWorld().getOtherEntities(
                    this,
                    this.getBoundingBox().expand(0.05),
                    e -> (e instanceof BoatEntity boat && boat.getPassengerList().size() < 2) ||
                            (e instanceof AbstractMinecartEntity cart && !cart.hasPassengers())
            );

            if (!vehicles.isEmpty()) {
                this.startRiding(vehicles.get(0), true);
                this.setSpin(Vec3d.ZERO);
                return true;
            }
        }

        return false;
    }

    private void sleeper() {
        if (this.getWorld().isClient()) {
            // Keep previous rotation aligned while resting to prevent render snapping on wake-up
            this.prevWorldRotation.set(this.worldRotation);
        } else {
            double radius = this.physicsConfig.radius();
            Vec3d centerPos = this.getPos().add(0, radius, 0);

            // Instant ground check every tick (prevents midair floating delay on block break)
            if (!GolfPhysicsEngine.hasGroundSupport(this.getWorld(), centerPos, radius)) {
                this.wakeUp();
                return;
            }

            // Low-frequency check (every 5 ticks) instead of 20 sub-steps per tick
            if (++this.sleepCheckTimer % 5 == 0) {
                if (!this.getWorld().getFluidState(this.getBlockPos()).isEmpty()) {
                    this.wakeUp();
                    return;
                }
                this.tickServerLogic();
            }
        }
    }

    private boolean checkAndHandleClipping() {
        if (this.getWorld().isClient()) return false;

        if (GolfPhysicsEngine.isClipping(this.getWorld(), this, this.getBoundingBox())) {
            this.stuckTicks++;
            // 1. Grace Period (Ticks 1-2): Let physics try to bounce/push it out naturally
            if (this.stuckTicks <= 2) {
                return false;
            }
            // 2. Soft Reset (Tick 3): Snap once back to last known SAFE position
            if (this.stuckTicks == 3 && !this.lastSafePos.equals(Vec3d.ZERO)) {
                this.setVelocity(Vec3d.ZERO);
                this.setPosition(this.lastSafePos.x, this.lastSafePos.y + 0.05, this.lastSafePos.z);
                return true; // Skip this tick's physics step
            }
            // 3. Emergency Cleanup (Tick 10+): Unrecoverable clipping, drop item safely
            if (this.stuckTicks > 10) {
                this.dropItem(RegisterItems.GOLF_BALL);
                this.discard();
                return true;
            }
        } else {
            // Ball is in valid open air: update safe position and clear counter
            this.lastSafePos = this.getPos();
            this.stuckTicks = 0;
        }
        return false;
    }

    private void tickPhysicsEngine() {
        // Pack current entity state
        double radius = this.physicsConfig.radius();
        Vec3d centerPos = this.getPos().add(0, radius, 0);
        GolfPhysicsEngine.State currentState = new GolfPhysicsEngine.State(
                centerPos,
                this.getVelocity(),
                this.getSpin(),
                this.isOnGround()
        );

        // Run deterministic physics engine step
        GolfPhysicsEngine.State newState = GolfPhysicsEngine.step(
                this.getWorld(), this, currentState, this.physicsConfig, true
        );

        // Offset reverse back to entity feet position
        Vec3d entityFeetPos = newState.pos().subtract(0, radius, 0);

        // Apply results back to Minecraft Entity
        this.prevX = this.getX();
        this.prevY = this.getY();
        this.prevZ = this.getZ();
        this.setPosition(entityFeetPos);
        this.setVelocity(newState.vel());
        this.setSpin(newState.spin());
        this.setOnGround(newState.onGround());
        this.velocityDirty = true;
        this.velocityModified = true;

        if (newState.onGround() && newState.vel().lengthSquared() < 1e-6 && newState.spin().lengthSquared() < 1e-4) {
            this.setSleeping(true);
        }

        // Triggers block interactions
        this.checkBlockCollision();
    }

    private void tickClientVisuals() {
        double radius = this.physicsConfig.radius();
        Vec3d vel = this.getVelocity();
        Vec3d activeSpin = this.getSpin();
        this.prevWorldRotation.set(this.worldRotation);
        double horizontalSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);

        if (this.isOnGround() && horizontalSpeed > 0.005) {
            float rollAngle = (float) (horizontalSpeed / radius);
            float axisX = (float) (vel.z / horizontalSpeed);
            float axisZ = (float) (-vel.x / horizontalSpeed);
            Quaternionf deltaRotation = new Quaternionf().rotationAxis(rollAngle, axisX, 0.0f, axisZ);
            deltaRotation.mul(this.worldRotation, this.worldRotation);
        } else if (activeSpin.lengthSquared() > 0.0001) {
            double spinSpeed = activeSpin.length();
            Vec3d spinAxis = activeSpin.normalize();
            Quaternionf deltaRotation = new Quaternionf().rotationAxis(
                    (float) spinSpeed, (float) spinAxis.x, (float) spinAxis.y, (float) spinAxis.z
            );
            deltaRotation.mul(this.worldRotation, this.worldRotation);
        }

        // Handle trail
        spawnTrailParticles();

        // Debug
        if(this.age % 1 == 0) {
            Vec3d centerPos = this.getPos().add(0, radius, 0);
            System.out.printf("[Tick %d] Vel: [X: %.4f, Y: %.4f, Z: %.4f] | centerPos [X: %.4f, Y: %.4f, Z: %.4f] | Rot [X: %.4f, Y: %.4f, Z: %.4f, W: %.4f]%n",
                    this.age, vel.x, vel.y, vel.z, centerPos.x, centerPos.y, centerPos.z, this.worldRotation.x, this.worldRotation.y, this.worldRotation.z, this.worldRotation.w);
        }
        DebugUtil.logOnChange("ball_" + this.getId() + "_is_on_ground", this.isOnGround());
    }

    private void tickServerLogic() {
        double radius = this.physicsConfig.radius();
        BlockPos ballCenterPos = BlockPos.ofFloored(this.getPos().add(0, radius, 0));
        BlockState stateAtBall = this.getWorld().getBlockState(ballCenterPos);

        BlockPos hopperPos = ballCenterPos;
        BlockEntity blockEntity = this.getWorld().getBlockEntity(hopperPos);

        if (!(blockEntity instanceof HopperBlockEntity)) {
            hopperPos = this.getBlockPos().down();
            blockEntity = this.getWorld().getBlockEntity(hopperPos);
        }

        if (blockEntity instanceof HopperBlockEntity hopper) {
            // Check if hopper is currently locked via Redstone
            if (!this.getWorld().getBlockState(hopperPos).get(HopperBlock.ENABLED)) {
                // Hopper is locked, skip collection
            } else if (this.tryInsertIntoHopper(hopper)) {
                // Play hopper suction sound
                this.getWorld().playSound(
                        null,
                        this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_ITEM_PICKUP,
                        SoundCategory.BLOCKS,
                        0.5F, 1.5F
                );
                this.discard();
                return;
            }
        }

        if (!stateAtBall.isAir()&& stateAtBall.getFluidState().isEmpty()) {
            if (stateAtBall.shouldSuffocate(this.getWorld(), ballCenterPos)) {
                // Drop as item if the ball is buried/inside a placed block
                this.dropStack(this.createStackFromEntity());
                this.discard();
                return;
            }
        }

        // Chunk loading logic
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            boolean isMoving = this.getVelocity().lengthSquared() > 0.01;
            ChunkPos currentChunk = new ChunkPos(this.getBlockPos());

            if (isMoving) {
                if (!currentChunk.equals(lastForcedChunk)) {
                    if (lastForcedChunk != null) {
                        serverWorld.setChunkForced(lastForcedChunk.x, lastForcedChunk.z, false);
                    }
                    serverWorld.getChunkManager().addTicket(
                            ChunkTicketType.POST_TELEPORT, currentChunk, 2, this.getId()
                    );
                    lastForcedChunk = currentChunk;
                }
            } else if (lastForcedChunk != null) {
                serverWorld.setChunkForced(lastForcedChunk.x, lastForcedChunk.z, false);
                lastForcedChunk = null;
            }
        }
    }

    private boolean tryInsertIntoHopper(HopperBlockEntity hopper) {
        ItemStack stackToInsert = this.createStackFromEntity();

        for (int i = 0; i < hopper.size(); i++) {
            ItemStack slotStack = hopper.getStack(i);

            if (slotStack.isEmpty()) {
                hopper.setStack(i, stackToInsert);
                hopper.markDirty();
                return true;
            } else if (ItemStack.canCombine(slotStack, stackToInsert) && slotStack.getCount() < slotStack.getMaxCount()) {
                slotStack.increment(1);
                hopper.markDirty();
                return true;
            }
        }
        return false; // Hopper is full
    }

    private boolean isOccludedByBlock(PlayerEntity player) {
        Vec3d start = player.getEyePos();
        Vec3d end = this.getBoundingBox().getCenter();

        RaycastContext context = new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.OUTLINE, // Only checks solid block collisions
                RaycastContext.FluidHandling.NONE,
                player
        );

        BlockHitResult hitResult = this.getWorld().raycast(context);
        return hitResult.getType() == HitResult.Type.BLOCK;
    }

    private void spawnTrailParticles() {
        double speedSq = this.getVelocity().lengthSquared();

        // 1. Instant cutoff if at or below minimum threshold
        if (speedSq <= SPEED_THRESHOLD_SQ) return;

        // 2. Calculate speed factor (0.0f when near threshold -> 1.0f at normal flight speed)
        float speedFactor = (float) MathHelper.clamp(
                (speedSq - SPEED_THRESHOLD_SQ) / FADE_WINDOW_SQ, 0.0, 1.0
        );

        // Hard stop if speed factor reaches zero
        if (speedFactor <= 0.0f) return;

        // Center trail at ball origin
        double radius = this.physicsConfig.radius();
        Vec3d currentPos = this.getPos().add(0, radius, 0);
        Vec3d prevPos = new Vec3d(this.prevX, this.prevY + radius, this.prevZ);

        double distance = currentPos.distanceTo(prevPos);
        if (distance <= 0.001) return;

        // 3. Sub-step interpolation: 12 steps per block moved guarantees no visual gaps
        int steps = Math.max(1, (int) Math.ceil(distance * 12));

        // 4. Match particle color to entity dye color
        int color = this.getColor();
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        // Scale size down rapidly near speed threshold
        float particleScale = 0.65f * speedFactor;
        DustParticleEffect particleEffect = new DustParticleEffect(new Vector3f(red, green, blue), particleScale);

        for (int i = 0; i < steps; i++) {
            // 5. Probabilistic spawn drop: density drops off sharply near threshold
            if (this.random.nextFloat() > speedFactor) continue;

            double delta = (double) i / steps;
            Vec3d p = prevPos.lerp(currentPos, delta);

            // Subtle random offset for natural dispersion
            double jitter = 0.015 * speedFactor;
            double offsetX = (this.random.nextDouble() - 0.5) * jitter;
            double offsetY = (this.random.nextDouble() - 0.5) * jitter;
            double offsetZ = (this.random.nextDouble() - 0.5) * jitter;

            this.getWorld().addParticle(
                    particleEffect,
                    p.x + offsetX,
                    p.y + offsetY,
                    p.z + offsetZ,
                    0.0, 0.0, 0.0
            );
        }
    }

    /**
     * public methods
     */
    //Calculates smooth client-side rolling rotation using JOML Quaternionf.
    public Quaternionf getPhysicsRotation(float tickDelta) {
        // Smoothly interpolate rolling angle across frames (prevents jitter)
        return new Quaternionf(this.prevWorldRotation).slerp(this.worldRotation, tickDelta);
    }

    public void applyImpulse(Vec3d impulse) {
        if (this.hasVehicle()) {
            this.mountCooldown = 4;
            this.stopRiding(); // Detach from boat/minecart before launching
        }

        this.setVelocity(
                GolfPhysicsEngine.applyImpulse(this.getVelocity(), impulse, physicsConfig.mass())
        );

        this.wakeUp();
        this.setOnGround(false);
        this.velocityModified = true;
    }

    public static GolfBallEntity getClosestBall(World world, PlayerEntity player, double radius, boolean checkOwnership) {
        double radiusSq = radius * radius;
        Box searchBox = player.getBoundingBox().expand(radius);

        return world.getEntitiesByClass(
                        GolfBallEntity.class,
                        searchBox,
                        ball -> ball.isAlive()
                                && ball.squaredDistanceTo(player) <= radiusSq
                                && (!checkOwnership || ball.isOwner(player))
                )
                .stream()
                .min(Comparator.comparingDouble(ball -> ball.squaredDistanceTo(player)))
                .orElse(null);
    }

    //Entity → Item
    public ItemStack createStackFromEntity() {
        ItemStack ballStack = new ItemStack(RegisterItems.GOLF_BALL);

        // Pass name
        if (this.hasCustomName()) {
            ballStack.setCustomName(this.getCustomName());
        }
        // Pass first strike time
        if (this.startTime > 0L) {
            ballStack.getOrCreateNbt().putLong("StartTime", this.startTime);
        }
        // Pass UUID
        if (this.hasOwner()) {
            ballStack.getOrCreateNbt().putUuid("OwnerUUID", this.getOwnerUuid().get());
        }
        // Pass isgoal
        if (this.isGoaled()) {
            ballStack.getOrCreateNbt().putBoolean("IsGoaled", true);
        }
        // Pass hitcount
        if (this.getHitCount() > 0) {
            ballStack.getOrCreateNbt().putInt("HitCount", this.getHitCount());
        }
        // Pass color
        int ballColor = this.getColor();
        if (ballColor != -1) {
            RegisterItems.GOLF_BALL.setColor(ballStack, ballColor);
        }
        // Wipe empty NBT compound
        GolfBall.sanitizeNbt(ballStack);
        return ballStack;
    }

    public void wakeUp() {
        this.setSleeping(false);
        this.sleepCheckTimer = 0;
    }

    /**
     *  Overrides
     */
    // Protect from environmental destruction (fire, cactus, explosions, etc.)
    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        if (damageSource.isOf(DamageTypes.CACTUS) ||
                damageSource.isOf(DamageTypes.EXPLOSION) ||
                damageSource.isOf(DamageTypes.DROWN)) {
            return true;
        }

        return super.isInvulnerableTo(damageSource);
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        return BALL_DIMENSIONS;
    }

    @Override
    public boolean canHit() {
        // Allows the player's crosshair to target and hit the ball (left-click or right-click)
        return !this.isRemoved();
    }

    @Override
    public void move(MovementType type, Vec3d movement) {
        super.move(type, movement);
        this.wakeUp();
        if (type == MovementType.PISTON) {
            if (movement.lengthSquared() > 1e-6) {
                Vec3d pushDir = movement.normalize();
                this.applyImpulse(pushDir.multiply(0.8D));
            }
        }
    }

    @Override
    protected void fall(double d, boolean bl, BlockState blockState, BlockPos blockPos) {
    }

    // Drop when hit 3 times (Entity → Item)
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.getWorld().isClient() || this.isRemoved()) return false;
        if (this.isInvulnerableTo(source)) return false;

        // Destroy the ball immediately without dropping an item if burned
        if (source.isIn(DamageTypeTags.IS_FIRE)) {
            this.getWorld().playSound(
                    null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_GENERIC_BURN, this.getSoundCategory(), 0.8F, 1.0F
            );
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                        ParticleTypes.SMOKE,
                        this.getX(), this.getY() + 0.1, this.getZ(),
                        8, 0.05, 0.05, 0.05, 0.02
                );
            }
            this.discard();
            return true;
        }

        // Each hit stacks +0.5 ball height
        float nextHeight = this.getHopHeight() + (0.5F * BALL_HEIGHT);

        // Break entity & drop item if reaching/exceeding ball height threshold
        if (nextHeight >= DROP_THRESHOLD) {
            ItemStack ballStack = new ItemStack(RegisterItems.GOLF_BALL);
            // Pass name
            if (this.hasCustomName()) {
                ballStack.setCustomName(this.getCustomName());
            }
            // Pass UUID
            if (this.hasOwner()) {
                ballStack.getOrCreateNbt().putUuid("OwnerUUID", this.getOwnerUuid().get());
            }
            // Pass color
            int ballColor = this.getColor();
            if (ballColor != -1) {
                RegisterItems.GOLF_BALL.setColor(ballStack, ballColor);
            } else {
                RegisterItems.GOLF_BALL.removeColor(ballStack); // Ensures clean, stackable item
            }

            ItemEntity itemEntity = this.dropStack(ballStack);
            if (itemEntity != null) {
                itemEntity.setVelocity(0, 0.3, 0);
                itemEntity.velocityModified = true; // Notifies client of velocity change
            }
            this.discard();
            return true;
        }

        // Update height stack and restart hop animation timer
        this.setHopHeight(nextHeight);
        this.setJumpTicks(8); // 8-tick hop duration
        this.scheduleVelocityUpdate();

        return true;
    }

    // Give Name Tag
    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        // Check if player is right-clicking with a renamed Name Tag
        if (stack.isOf(Items.NAME_TAG) && stack.hasCustomName()) {
            if (!this.getWorld().isClient()) {
                this.setCustomName(stack.getName());
                this.setCustomNameVisible(true);
                this.setOwnerUuid(player.getUuid());

                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
            }
            return ActionResult.success(this.getWorld().isClient());
        }

        return super.interact(player, hand);
    }

    @Override
    public void addVelocity(double deltaX, double deltaY, double deltaZ) {
        super.addVelocity(deltaX, deltaY, deltaZ);
        if (deltaX != 0 || deltaY != 0 || deltaZ != 0) {
            this.wakeUp(); // Wake up on explosions
        }
    }

    @Override
    public void setVelocity(Vec3d velocity) {
        super.setVelocity(velocity);
        if (velocity != null && velocity.lengthSquared() > 1e-6) {
            this.wakeUp();
        }
    }

    // Glowing effect control
    @Override
    public boolean isGlowing() {
        if (this.getWorld().isClient()) {
            // Gets the client player currently loaded in this world without touching MinecraftClient
            PlayerEntity clientPlayer = this.getWorld().getClosestPlayer(this, 64.0D);
            if (clientPlayer != null) {
                return this.getVelocity().lengthSquared() <= SPEED_THRESHOLD_SQ
                        && isOccludedByBlock(clientPlayer);
            }
        }
        return super.isGlowing();
    }

    @Override
    protected boolean canStartRiding(Entity vehicle) {
        return vehicle instanceof BoatEntity || vehicle instanceof AbstractMinecartEntity;
    }
    @Override
    public double getHeightOffset() {
        // Elevates the ball's origin out of the vehicle floor
        if (this.getVehicle() instanceof BoatEntity) {
            return 0.30D; // Adjust up/down as needed for boat seats
        } else if (this.getVehicle() instanceof AbstractMinecartEntity) {
            return 0.30D; // Adjust up/down for minecarts
        }
        return super.getHeightOffset();
    }
    @Override
    public float getTargetingMargin() {
        // Inflates the crosshair raycast box while mounted so the ray hits the ball
        // before or equal to the surrounding vehicle's hull box
        return this.hasVehicle() ? 0.2F : 0.05F;
    }

    @Override
    public int getTeamColorValue() {
        return this.getColor(); // Returns RGB integer (e.g., 0xFF0000 for red)
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        // Safety check: Release forced chunk if the ball is destroyed or despawned
        if (!this.getWorld().isClient() && lastForcedChunk != null && this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.setChunkForced(lastForcedChunk.x, lastForcedChunk.z, false);
            lastForcedChunk = null;
        }
    }

    // --- DataTracker methods ---
    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(JUMP_TICKS, 0);
        this.dataTracker.startTracking(HOP_HEIGHT, 0.0F);
        this.dataTracker.startTracking(COLOR, 0xFFFFFF);
        this.dataTracker.startTracking(HIT_COUNT, 0);
        this.dataTracker.startTracking(IS_GOALED, false);
        this.dataTracker.startTracking(SPIN_VECTOR, new Vector3f(0.0f, 0.0f, 0.0f));
        this.dataTracker.startTracking(OWNER_UUID, Optional.empty());
        this.dataTracker.startTracking(IS_SLEEPING, false);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("Color")) {
            this.setColor(nbt.getInt("Color"));
        }
        if (nbt.contains("HitCount")) {
            this.setHitCount(nbt.getInt("HitCount"));
        }
        if (nbt.contains("IsGoaled")) {
            this.setGoaled(nbt.getBoolean("IsGoaled"));
        }
        if (nbt.contains("SpinX")) {
            this.setSpin(new Vec3d(nbt.getDouble("SpinX"), nbt.getDouble("SpinY"), nbt.getDouble("SpinZ")));
        }
        if (nbt.containsUuid("OwnerUUID")) {
            this.setOwnerUuid(nbt.getUuid("OwnerUUID"));
        }if (nbt.contains("StartTime")) {
            this.startTime = nbt.getLong("StartTime");
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Color", this.getColor());
        nbt.putInt("HitCount", this.getHitCount());
        nbt.putBoolean("IsGoaled", this.isGoaled());
        Vec3d spin = this.getSpin();
        nbt.putDouble("SpinX", spin.x);
        nbt.putDouble("SpinY", spin.y);
        nbt.putDouble("SpinZ", spin.z);
        this.getOwnerUuid().ifPresent(uuid -> nbt.putUuid("OwnerUUID", uuid));
        nbt.putLong("StartTime", this.startTime);
    }
}
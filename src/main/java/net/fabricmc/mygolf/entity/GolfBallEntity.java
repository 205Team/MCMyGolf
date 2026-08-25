package net.fabricmc.mygolf.entity;

import net.fabricmc.mygolf.global.ModConfig;
import net.fabricmc.mygolf.items.GolfBall;
import net.fabricmc.mygolf.physics.GolfPhysicsEngine;
import net.fabricmc.mygolf.registry.RegisterItems;
import net.fabricmc.mygolf.tools.DebugUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.Comparator;

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
    // Base radiuses in blocks
    public static final double MIN_RADIUS = 3.0D;   // Close range (Name tag / direct focus)
    public static final double MID_RADIUS = 16.0D;   // Medium range (3D Arrow)
    public static final double MAX_RADIUS = 256.0D;  // Far range (Beacon Beam)
    // Pre-squared distances for fast performance checks (r^2)
    public static final double MIN_DISTANCE_SQ = MIN_RADIUS * MIN_RADIUS;
    public static final double MID_DISTANCE_SQ = MID_RADIUS * MID_RADIUS;
    public static final double MAX_DISTANCE_SQ = MAX_RADIUS * MAX_RADIUS;

    public GolfBallEntity(EntityType<? extends GolfBallEntity> entityType, World level) {
        super(entityType, level);
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
        }

    }

    public Vec3d getSpin() {
        Vector3f vec = this.dataTracker.get(SPIN_VECTOR);
        return new Vec3d(vec.x(), vec.y(), vec.z());
    }
    public void setSpin(Vec3d spin) {
        this.dataTracker.set(SPIN_VECTOR, new Vector3f((float) spin.x, (float) spin.y, (float) spin.z));
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

    @Override
    public void tick() {
        super.tick();

        // 1. Pack current entity state (Run on BOTH Client and Server for perfect sync)
        double radius = this.physicsConfig.radius();
        Vec3d centerPos = this.getPos().add(0, radius, 0);

        GolfPhysicsEngine.State currentState = new GolfPhysicsEngine.State(
                centerPos,
                this.getVelocity(),
                this.getSpin(),
                this.isOnGround()
        );

        // 2. Run deterministic physics engine step
        GolfPhysicsEngine.State newState = GolfPhysicsEngine.step(
                this.getWorld(), this, currentState, this.physicsConfig, true
        );

        // Offset reverse back to entity feet position
        Vec3d entityFeetPos = newState.pos().subtract(0, radius, 0);

        // 3. Apply results back to Minecraft Entity
        this.setPosition(entityFeetPos);
        this.setVelocity(newState.vel());
        this.setSpin(newState.spin());
        this.setOnGround(newState.onGround());

        // Triggers block interactions
        this.checkBlockCollision();

        // 4. Jump animation handling
        if (this.getJumpTicks() > 0) {
            this.setJumpTicks(this.getJumpTicks() - 1);
        }
        if (this.isOnGround() && this.getJumpTicks() == 0) {
            this.setHopHeight(0.0F);
        }

        // 5. Split side-specific logic (Visuals vs Server Management)
        if (this.getWorld().isClient()) {
            /**
             * Client-side Visuals
             */
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
//            if(this.age % 1 == 0) {
//                System.out.printf("[Tick %d] Vel: [X: %.4f, Y: %.4f, Z: %.4f] | Pos [X: %.4f, Y: %.4f, Z: %.4f] | Rot [X: %.4f, Y: %.4f, Z: %.4f, W: %.4f]%n",
//                        this.age, vel.x, vel.y, vel.z, entityFeetPos.x, entityFeetPos.y, entityFeetPos.z, this.worldRotation.x, this.worldRotation.y, this.worldRotation.z, this.worldRotation.w);
//            }
            DebugUtil.logOnChange("ball is on ground", this.isOnGround());

        } else {
            /**
             * Server-side Logic
             */
            BlockPos ballCenterPos = BlockPos.ofFloored(this.getPos().add(0, radius, 0));
            BlockState stateAtBall = this.getWorld().getBlockState(ballCenterPos);

            if (!stateAtBall.isAir()&& stateAtBall.getFluidState().isEmpty()) {
                if (stateAtBall.isOf(Blocks.MOVING_PISTON)) {
                    // Apply impulse if piston pushes
                    if (this.getWorld().getBlockEntity(ballCenterPos) instanceof PistonBlockEntity piston) {
                        Direction pushDir = piston.getMovementDirection();
                        double launchSpeed = 0.8D;

                        Vec3d impulse = Vec3d.of(pushDir.getVector()).multiply(launchSpeed);
                        this.applyImpulse(impulse);
                        return;
                    }
                } else if (stateAtBall.shouldSuffocate(this.getWorld(), ballCenterPos)) {
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
                        serverWorld.setChunkForced(currentChunk.x, currentChunk.z, true);
                        lastForcedChunk = currentChunk;
                    }
                } else if (lastForcedChunk != null) {
                    serverWorld.setChunkForced(lastForcedChunk.x, lastForcedChunk.z, false);
                    lastForcedChunk = null;
                }
            }
        }
    }

    //Calculates smooth client-side rolling rotation using JOML Quaternionf.
    public Quaternionf getPhysicsRotation(float tickDelta) {
        // Smoothly interpolate rolling angle across frames (prevents jitter)
        return new Quaternionf(this.prevWorldRotation).slerp(this.worldRotation, tickDelta);
    }

    public void applyImpulse(Vec3d impulse) {
        this.setVelocity(
                GolfPhysicsEngine.applyImpulse(this.getVelocity(), impulse, physicsConfig.mass())
        );

        this.setOnGround(false);
        this.velocityModified = true;
    }

    public static GolfBallEntity getClosestBall(World world, Entity origin, double radius) {
        double radiusSq = radius * radius;
        Box searchBox = origin.getBoundingBox().expand(radius);
        return world.getEntitiesByClass(GolfBallEntity.class, searchBox, b -> b.isAlive() && b.squaredDistanceTo(origin) <= radiusSq)
                .stream()
                .min(Comparator.comparingDouble(b -> b.squaredDistanceTo(origin)))
                .orElse(null);
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

    //    @Override
    //    public boolean isSilent() {
    //        return true;
    //    }

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
            // Pass color
            int ballColor = this.getColor();
            if (ballColor != -1 && ballColor != 0xFFFFFF && ballColor != 0xF9FFFE) {
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

    //Entity → Item
    public ItemStack createStackFromEntity() {
        ItemStack ballStack = new ItemStack(RegisterItems.GOLF_BALL);

        // Pass name
        if (this.hasCustomName()) {
            ballStack.setCustomName(this.getCustomName());
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
        if (ballColor != -1 && ballColor != 0xFFFFFF && ballColor != 0xF9FFFE) {
            RegisterItems.GOLF_BALL.setColor(ballStack, ballColor);
        }
        // Wipe empty NBT compound
        GolfBall.sanitizeNbt(ballStack);
        return ballStack;
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

                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
            }
            return ActionResult.success(this.getWorld().isClient());
        }

        return super.interact(player, hand);
    }

    @Override
    public Text getDisplayName() {
        int hits = this.getHitCount();

        // Create the grey hit count text: "(Hits: 3)"
        Text hitText;
        if (this.isGoaled()) {
            hitText = Text.literal(" (Hits: " + hits + ")").formatted(Formatting.GOLD);
        } else{
            hitText = Text.literal(" (Hits: " + hits + ")").formatted(Formatting.GRAY);
        }

        if (this.hasCustomName()) {
            // If name-tagged: "CustomName (Hits: 3)"
            return Text.empty().append(this.getCustomName()).append(hitText);
        } else {
            // Default: "Golf Ball (Hits: 3)"
            return Text.translatable("entity.mygolf.golf_ball").append(hitText);
        }
    }

    // Glowing effect control
    @Override
    public boolean isGlowing() {
        if (this.getWorld().isClient()) {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            if (client.player != null) {
                double distance = this.squaredDistanceTo(client.player);

                if (distance <= 64.0 * 64.0) {
                    boolean isSlow = this.getVelocity().lengthSquared() <= SPEED_THRESHOLD_SQ;
                    return isSlow && isOccludedByBlock(client.player);
                }
            }
        }
        return super.isGlowing();
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
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Color", this.getColor());
        nbt.putInt("HitCount", this.getHitCount());
        nbt.putBoolean("IsGoaled", this.isGoaled());
    }
}
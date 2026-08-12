package net.fabricmc.mygolf.physics;

import net.fabricmc.mygolf.blocks.GolfHole;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.events.GolfBallEntityEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class GolfPhysicsEngine {

    // Immutable snapshot of the ball's physical state
    public record State(Vec3d pos, Vec3d vel, Vec3d spin, boolean onGround) {
    }

    // Physical constants configuration
    public record Config(
            double mass,             // kg
            double radius,           // meters
            double restitution,      // bounciness (e)
            double dragCoefficient,  // air resistance per tick
            double gravity,          // gravity per tick
            double liftCoefficient   // Magnus effect intensity
    ) {
        public static final Config STANDARD_BALL = new Config(
                0.045, 0.125, 0.6, 0.985, 0.035, 0.01
        );
    }

    public record SphereCollision(Vec3d resolvedPos, Vec3d normal, boolean hit) {}

    /**
     * Integrates one tick of flight, drag, Magnus forces, and continuous collision response.
     */
    public static State step(World world, GolfBallEntity ball, State current, Config config) {
        Vec3d vel = current.vel();
        Vec3d spin = current.spin();
        Vec3d pos = current.pos();

        /**
         * Ground Sliding
         */
        if (current.onGround()) {
            // Beneath Block collision check directly under the ball's center
            if (!hasGroundSupport(world, pos, config.radius())) {
                current = new State(pos, vel, spin, false);
            } else {
                BlockPos groundPos = BlockPos.ofFloored(pos.x, pos.y - config.radius() - 0.05, pos.z);
                double groundFriction = getSurfaceFriction(world, groundPos);

                // Decelerate horizontal sliding velocity using ground friction
                Vec3d newVel = new Vec3d(vel.x * groundFriction, 0, vel.z * groundFriction);

                if (vel.lengthSquared() < 0.0001) {
                    // Check hole entry when stationary
                    checkHoleEntry(world, ball);
                    return new State(pos, Vec3d.ZERO, Vec3d.ZERO, true); // Rest state
                }

                // Perform sphere sweep on ground to detect upcoming ledge/wall impacts cleanly
                Vec3d targetPos = pos.add(newVel);
                SphereCollision groundSweep = resolveSphereCollision(world, targetPos, config.radius());
                if (groundSweep.hit() && groundSweep.normal().y < 0.5) { // Hit a wall
                    Vec3d normal = groundSweep.normal();
                    Vec3d reflectedVel = newVel.subtract(normal.multiply(2 * newVel.dotProduct(normal))).multiply(0.5);

                    GolfBallEntityEvents.ON_COLLISION.invoker().onCollision(
                            world, ball, BlockPos.ofFloored(groundSweep.resolvedPos()), world.getBlockState(BlockPos.ofFloored(groundSweep.resolvedPos())), normal, newVel.length()
                    );
                    return new State(groundSweep.resolvedPos(), reflectedVel, spin, true);
                }

                return new State(targetPos, newVel, spin.multiply(0.98), true);
            }
        }

        /**
         * In air
         */
        // Re-bind current values in case we fell through from ground mode above
        vel = current.vel();
        spin = current.spin();
        pos = current.pos();

        // Aerodynamics Integration
        Vec3d magnusForce = spin.crossProduct(vel).multiply(config.liftCoefficient());
        Vec3d newVel = vel
                .multiply(config.dragCoefficient())
                .add(0, -config.gravity(), 0)
                .add(magnusForce);

        // Continuous Collision Detection (CCD) Sweep
        double velocityMagnitude = newVel.length();
        double maxStep = config.radius() * 0.5;
        int substeps = Math.max(1, (int) Math.ceil(velocityMagnitude / maxStep));
        substeps = Math.min(substeps, 20); // Cap substeps to prevent lag spikes

        Vec3d stepVel = newVel.multiply(1.0 / substeps);
        Vec3d currentCenter = pos;
        SphereCollision finalCollision = null;

        // Sweep the sphere incrementally along the velocity vector
        for (int i = 0; i < substeps; i++) {
            Vec3d nextCenter = currentCenter.add(stepVel);
            SphereCollision collision = resolveSphereCollision(world, nextCenter, config.radius());

            if (collision.hit()) {
                finalCollision = collision;
                break; // Catch spatial overlap immediately
            }
            currentCenter = nextCenter;
        }

        // Multi-Axis Dynamic Friction Impact Resolution
        if (finalCollision != null) {
            Vec3d normal = finalCollision.normal();
            double impactSpeed = newVel.length();

            // Infer the struck block by backing up slightly into the surface normal
            BlockPos impactBlockPos = BlockPos.ofFloored(
                    finalCollision.resolvedPos().subtract(normal.multiply(config.radius() + 0.05))
            );

            // Vector Decomposition: Split into Normal and Tangential components
            double normalDot = newVel.dotProduct(normal);
            Vec3d normalVel = normal.multiply(Math.min(0, normalDot));
            Vec3d tangentVel = newVel.subtract(normalVel);

            // Fetch surface properties of the block hit
            double surfaceFriction = getSurfaceFriction(world, impactBlockPos);
            double surfaceBounciness = getSurfaceRestitution(world, impactBlockPos);

            // Impulse-Based Normal & Tangential Resolution
            Vec3d reflectedNormal = normalVel.multiply(-surfaceBounciness);

            double normalImpulse = -(1.0 + surfaceBounciness) * normalDot;
            double maxFrictionImpulse = surfaceFriction * normalImpulse;
            double tangentSpeed = tangentVel.length();

            Vec3d reflectedTangent= tangentVel;
            if (tangentSpeed > 0.0001 && normalDot < 0) {
                double frictionFactor = Math.max(0.65, 1.0 - (maxFrictionImpulse / tangentSpeed));
                reflectedTangent = tangentVel.multiply(frictionFactor);
            }

            Vec3d reflectedVel = reflectedNormal.add(reflectedTangent);
            Vec3d newSpin = spin.multiply(0.98);
            // Add 0.001 offset along normal to prevent depth-buffer overlap on consecutive ticks
            Vec3d adjustedPos = finalCollision.resolvedPos().add(normal.multiply(0.001));

            // Check if there is true solid ground under the ball's center
            boolean hasGround = hasGroundSupport(world, adjustedPos, config.radius());
            // Transition to ground state if normal is pointing up AND there is actual ground support underneath
            boolean landsOnGround = normalVel.y <= 0
                    && normal.y > 0.7
                    && hasGround;
            if (landsOnGround) {
                double incomingHorizontalSpeed = Math.hypot(newVel.x, newVel.z);
                if (incomingHorizontalSpeed < 0.0001) {
                    reflectedVel = Vec3d.ZERO;
                } else {
                    reflectedVel = new Vec3d(newVel.x * 0.95, 0, newVel.z * 0.95);
                }
            }

            GolfBallEntityEvents.ON_COLLISION.invoker().onCollision(
                    world, ball, impactBlockPos, world.getBlockState(impactBlockPos), normal, impactSpeed
            );

            return new State(adjustedPos, reflectedVel, newSpin, landsOnGround);
        } else {
            // No collision detected during sweep; apply full velocity
            return new State(pos.add(newVel), newVel, spin.multiply(0.99), false);
        }
    }

    /**
     * Checks if there are any solid collision shapes supporting the bottom of the ball.
     */
    private static boolean hasGroundSupport(World world, Vec3d centerPos, double radius) {
        double probeWidth = 0.02;

        Box groundCheckArea = new Box(
                centerPos.x - probeWidth, centerPos.y - radius - 0.05, centerPos.z - probeWidth,
                centerPos.x + probeWidth, centerPos.y - radius + 0.01, centerPos.z + probeWidth
        );

        // Returns true if there is at least one solid collision shape in this area
        return world.getBlockCollisions(null, groundCheckArea).iterator().hasNext();
    }

    /**
     * Collision helper
     */
    public static SphereCollision resolveSphereCollision(World world, Vec3d intendedCenter, double radius) {
        // 1. Broadphase: Create an AABB encompassing the sphere to fetch blocks
        Box searchBox = new Box(
                intendedCenter.x - radius, intendedCenter.y - radius, intendedCenter.z - radius,
                intendedCenter.x + radius, intendedCenter.y + radius, intendedCenter.z + radius
        );

        Iterable<VoxelShape> shapes = world.getBlockCollisions(null, searchBox);

        Vec3d currentCenter = intendedCenter;
        Vec3d accumulatedNormal = Vec3d.ZERO;
        boolean collided = false;

        // 2. Narrowphase: Exact Sphere-vs-AABB Math
        for (VoxelShape shape : shapes) {
            for (Box box : shape.getBoundingBoxes()) {
                // Find the closest point P on the AABB to the sphere center C
                double closestX = Math.max(box.minX, Math.min(currentCenter.x, box.maxX));
                double closestY = Math.max(box.minY, Math.min(currentCenter.y, box.maxY));
                double closestZ = Math.max(box.minZ, Math.min(currentCenter.z, box.maxZ));

                // Calculate distance vector from closest point on box to sphere center
                double dx = currentCenter.x - closestX;
                double dy = currentCenter.y - closestY;
                double dz = currentCenter.z - closestZ;

                double distanceSq = dx * dx + dy * dy + dz * dz;

                // If distance is less than radius squared, the sphere is penetrating the box
                if (distanceSq < radius * radius) {
                    collided = true;
                    Vec3d pushNormal;
                    double penetrationDepth;

                    if (distanceSq > 0.00001) {
                        double distance = Math.sqrt(distanceSq);
                        pushNormal = new Vec3d(dx / distance, dy / distance, dz / distance);
                        penetrationDepth = radius - distance;
                    } else {
                        // Sphere center is inside the AABB: Push out along the axis of shallowest penetration
                        double minX = intendedCenter.x - box.minX;
                        double maxX = box.maxX - intendedCenter.x;
                        double minY = intendedCenter.y - box.minY;
                        double maxY = box.maxY - intendedCenter.y;
                        double minZ = intendedCenter.z - box.minZ;
                        double maxZ = box.maxZ - intendedCenter.z;

                        double min = Math.min(Math.min(Math.min(minX, maxX), Math.min(minY, maxY)), Math.min(minZ, maxZ));

                        if (min == maxY) pushNormal = new Vec3d(0, 1, 0);
                        else if (min == minY) pushNormal = new Vec3d(0, -1, 0);
                        else if (min == maxX) pushNormal = new Vec3d(1, 0, 0);
                        else if (min == minX) pushNormal = new Vec3d(-1, 0, 0);
                        else if (min == maxZ) pushNormal = new Vec3d(0, 0, 1);
                        else pushNormal = new Vec3d(0, 0, -1);

                        penetrationDepth = radius + min;
                    }

                    accumulatedNormal = accumulatedNormal.add(pushNormal);
                    currentCenter = currentCenter.add(pushNormal.multiply(penetrationDepth));
                }
            }
        }

        return new SphereCollision(
                currentCenter,
                collided ? accumulatedNormal.normalize() : Vec3d.ZERO,
                collided
        );
    }

    /**
     * Calculates new velocity from an applied impulse vector: Δv = J / m
     */
    public static Vec3d applyImpulse(Vec3d currentVel, Vec3d impulse, double mass) {
        // 1. Calculate velocity change in m/s: Δv = J / m
        Vec3d deltaVelocityMetersPerSec = impulse.multiply(1.0 / mass);
        // 2. Convert m/s to blocks/tick (divide by 20 ticks per second)
        Vec3d deltaVelocityBlocksPerTick = deltaVelocityMetersPerSec.multiply(1.0 / 20.0);

        return currentVel.add(deltaVelocityBlocksPerTick);
    }

    /**
     * Give different frictions to different blocks
     */
    public static double getSurfaceFriction(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        if (state.isIn(BlockTags.SAND)) return 0.30;       // Bunkers kill horizontal momentum
        if (state.isIn(BlockTags.LEAVES)) return 0.20;   // More likely to get stuck on trees
        if (state.isOf(Blocks.GREEN_CARPET)) return 0.95;  // Smooth greens allow long slides
        if (state.isOf(Blocks.SLIME_BLOCK)) return 0.10;   // Slime sticks and absorbs slide energy

        // Map native MC slipperiness (0.60 default -> 0.92 rolling friction)
        double mcSlipperiness = state.getBlock().getSlipperiness();
        return 0.80 + (mcSlipperiness * 0.20);
    }

    /**
     * Give different restitution(bounce absorption) to different blocks
     */
    public static double getSurfaceRestitution(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        if (state.isOf(Blocks.SLIME_BLOCK)) return 0.95;    // Trampoline-like bounce
        if (state.isOf(Blocks.ICE) || state.isOf(Blocks.PACKED_ICE)) return 0.75; // Hard & elastic
        if (state.isIn(BlockTags.SAND)) return 0.10;         // Sand absorbs almost all kinetic energy
        if (state.isIn(BlockTags.LEAVES)) return 0.10;         // Leaves absorb almost all kinetic energy
        if (state.isIn(BlockTags.WOOL)) return 0.25;         // Soft wool dampens energy
        if (state.isOf(Blocks.GREEN_CARPET)) return 0.35;    // Fairway/Green absorbs moderate bounce

        // 3. Default Hard Blocks (Stone, Wood, Concrete, Metals)
        return 0.60;
    }

    /**
     * Check physically if ball enters hole
     */
    public static void checkHoleEntry(World world, GolfBallEntity ball) {
        if (world.isClient()) return;

        BlockPos golfBallPos = ball.getBlockPos();
        BlockState golfBallBlockState = world.getBlockState(golfBallPos);
        boolean isHole = golfBallBlockState.getBlock() instanceof GolfHole;

        if (isHole) {
            GolfBallEntityEvents.ON_HOLE_ENTER.invoker().onBallEnterHole(world, ball, golfBallPos);
        }
    }

    /**
     * Simulates projectile trajectory steps from a starting point.
     */
    public static List<Vec3d> predictTrajectory(World world, GolfBallEntity ball, Vec3d startPos, Vec3d initialVelocity, Vec3d initialSpin, Config config, int maxSteps) {
        List<Vec3d> points = new ArrayList<>();

        // Start exactly at the physics center of the ball
        Vec3d simPos = startPos.add(0.0, config.radius(), 0.0);
        State current = new State(simPos, initialVelocity, initialSpin, false);

        // Convert back to feet position for the renderer
        points.add(current.pos().subtract(0, config.radius(), 0));

        for (int i = 0; i < maxSteps; i++) {
            current = step(world, ball, current, config);

            // Convert all predicted steps back to feet positions
            points.add(current.pos().subtract(0, config.radius(), 0));

            // Stop predicting if the ball lands and stops moving
            if (current.onGround() && current.vel().lengthSquared() < 0.0001) {
                break;
            }
        }

        return points;
    }
}
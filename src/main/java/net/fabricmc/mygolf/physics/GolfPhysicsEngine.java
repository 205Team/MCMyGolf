package net.fabricmc.mygolf.physics;

import net.fabricmc.mygolf.blocks.GolfHole;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.events.GolfBallEntityEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

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
                0.04593, 0.021335, 0.6, 0.985, 0.035, 0.01
        );
    }

    /**
     * Integrates one tick of flight, drag, Magnus forces, and continuous collision response.
     */
    public static State step(World world, GolfBallEntity ball, State current, Config config) {
        Vec3d vel = current.vel();
        Vec3d spin = current.spin();
        Vec3d pos = current.pos();

        // 1. Continuous Ground Sliding (If already rolling on ground)
        if (current.onGround()) {
            BlockPos groundPos = BlockPos.ofFloored(pos.x, pos.y - 0.1, pos.z);
            double groundFriction = getSurfaceFriction(world, groundPos);

            // Decelerate horizontal sliding velocity using ground friction
            vel = new Vec3d(vel.x * groundFriction, 0, vel.z * groundFriction);

            if (vel.lengthSquared() < 0.0001) {
                // Check hole entry when stationary
                checkHoleEntry(world, ball);
                return new State(pos, Vec3d.ZERO, Vec3d.ZERO, true); // Rest state
            }
        }

        // 2. Aerodynamics Integration
        Vec3d magnusForce = spin.crossProduct(vel).multiply(config.liftCoefficient());
        Vec3d newVel = vel
                .multiply(config.dragCoefficient())
                .add(0, -config.gravity(), 0)
                .add(magnusForce);

        // 3. Raycast for Impact
        Vec3d targetPos = pos.add(newVel);
        BlockHitResult hit = world.raycast(new RaycastContext(
                pos,
                targetPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                ball
        ));

        // 5. Multi-Axis Dynamic Friction Impact Resolution
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3d normal = Vec3d.of(hit.getSide().getVector());
            double impactSpeed = newVel.length();

            /**
             * Reflection physics math
             */
            // Vector Decomposition: Split into Normal and Tangential components
            double normalDot = newVel.dotProduct(normal);
            Vec3d normalVel = normal.multiply(normalDot);
            Vec3d tangentVel = newVel.subtract(normalVel);

            // Fetch surface properties of the block hit
            double surfaceFriction = getSurfaceFriction(world, hit.getBlockPos());
            double surfaceBounciness = getSurfaceRestitution(world, hit.getBlockPos());

            // Apply material properties independently
            Vec3d reflectedNormal = normalVel.multiply(-surfaceBounciness); // Scaled bounce
            Vec3d reflectedTangent = tangentVel.multiply(surfaceFriction);   // Scaled slide

            Vec3d reflectedVel = reflectedNormal.add(reflectedTangent);
            Vec3d newSpin = spin.multiply(surfaceFriction * surfaceBounciness);

            Vec3d adjustedPos = hit.getPos().add(normal.multiply(config.radius() + 0.01));

            boolean onGround = Math.abs(reflectedVel.y) < 0.02 && normal.y > 0.8;
            if (onGround) {
                reflectedVel = new Vec3d(reflectedVel.x, 0, reflectedVel.z);
            }

            /**
             * End of reflection physics
             */
            // Call collision listeners
            GolfBallEntityEvents.ON_COLLISION.invoker().onCollision(world, ball, hit.getBlockPos(), world.getBlockState(hit.getBlockPos()), normal, impactSpeed);

            return new State(adjustedPos, reflectedVel, newSpin, onGround);
        } else {
            return new State(targetPos, newVel, spin.multiply(0.99), false);
        }
    }

    /**
     * Calculates new velocity from an applied impulse vector: Δv = J / m
     */
    public static Vec3d applyImpulse(Vec3d currentVel, Vec3d impulse, double mass) {
        return currentVel.add(impulse.multiply(1.0 / mass));
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

        return state.getBlock().getSlipperiness(); // Native MC fallback (Ice = 0.98, Normal = 0.6)
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
}
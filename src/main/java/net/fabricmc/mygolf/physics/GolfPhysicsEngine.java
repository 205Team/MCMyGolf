package net.fabricmc.mygolf.physics;

import net.fabricmc.mygolf.blocks.GolfHole;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.events.GolfBallEntityEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class GolfPhysicsEngine {

    public record State(Vec3d pos, Vec3d vel, Vec3d spin, boolean onGround) {}

    public record Config(
            double mass,
            double radius,
            double restitution,
            double dragCoefficient,
            double gravity,
            double liftCoefficient,
            double buoyancy
    ) {
        public static final Config STANDARD_BALL = new Config(
                0.045,
                0.125,
                0.6,
                0.985,
                0.035,
                0.01,
                0.028
        );
    }

    public record SphereCollision(Vec3d resolvedPos, Vec3d normal, boolean hit) {}

    /**
     * Integrates one tick of flight, drag, Magnus forces, and continuous collision response.
     */
    public static State step(World world, GolfBallEntity ball, State current, Config config, boolean isRealTick) {
        Vec3d vel = current.vel();
        Vec3d spin = current.spin();
        Vec3d pos = current.pos();
        boolean onGround = current.onGround();

        // 1. Verify ground support before applying velocity
        if (onGround && (vel.y > 0.01 || !hasGroundSupport(world, pos, config.radius()))) {
            onGround = false;
        }

        // 2. Calculate new tick velocity
        Vec3d newVel;
        if (onGround) {
            BlockPos groundPos = BlockPos.ofFloored(pos.x, pos.y - config.radius() - 0.05, pos.z);
            double groundFriction = getSurfaceFriction(world, groundPos);

            // Decelerate horizontal sliding velocity using ground friction
            newVel = new Vec3d(vel.x * groundFriction, 0, vel.z * groundFriction);

            if (newVel.lengthSquared() < 0.0001) {
                // Check hole entry when stationary
                if (isRealTick) {
                    checkHoleEntry(world, ball);
                }
                return new State(pos, Vec3d.ZERO, Vec3d.ZERO, true);
            }
        } else {
            // Check if the center of the ball is submerged in a liquid
            BlockPos currentPos = BlockPos.ofFloored(pos);
            boolean isInFluid = !world.getFluidState(currentPos).isEmpty();

            if (isInFluid) {
                // Fluid Physics: Heavy drag + Buoyancy lift
                double fluidDrag = 0.82;     // High resistance (slows down fast entries)

                // Effective gravity in water = downward gravity + upward buoyancy
                double netGravity = -config.gravity() + config.buoyancy(); // Default: -0.035 + 0.028 = -0.007

                newVel = vel.multiply(fluidDrag).add(0, netGravity, 0);
            } else {
                // Air Physics: Standard drag + Gravity + Magnus force
                Vec3d magnusForce = GolfBallEntity.ENABLE_MAGNUS_EFFECT
                        ? spin.crossProduct(vel).multiply(config.liftCoefficient())
                        : Vec3d.ZERO;
                newVel = vel.multiply(config.dragCoefficient()).add(0, -config.gravity(), 0).add(magnusForce);
            }
        }

        // 3. Substep movement (prevents fast balls from skipping over block borders)
        double velocityMagnitude = newVel.length();
        double maxStep = config.radius() * 0.4;
        int substeps = Math.max(1, (int) Math.ceil(velocityMagnitude / maxStep));
        substeps = Math.min(substeps, 20);

        Vec3d stepVel = newVel.multiply(1.0 / substeps);
        Vec3d currentCenter = pos;
        Vec3d currentVel = newVel;
        Vec3d currentSpin = spin;
        boolean currentlyOnGround = onGround;

        // 4. Ray/Sphere sweep movement tick by tick
        for (int i = 0; i < substeps; i++) {
            Vec3d nextCenter = currentCenter.add(stepVel);
            SphereCollision collision = resolveSphereCollision(world, nextCenter, currentCenter, config.radius());

            if (collision.hit()) {
                Vec3d normal = collision.normal();
                double impactSpeed = currentVel.length();
                BlockPos impactBlockPos = BlockPos.ofFloored(collision.resolvedPos().subtract(normal.multiply(config.radius() + 0.05)));

                // Separate velocity into Normal (perpendicular) and Tangent (parallel) vectors
                double normalDot = currentVel.dotProduct(normal);
                Vec3d normalVel = normal.multiply(Math.min(0, normalDot));
                Vec3d tangentVel = currentVel.subtract(normalVel);

                double surfaceFriction = getSurfaceFriction(world, impactBlockPos);
                double surfaceBounciness = getSurfaceRestitution(world, impactBlockPos);

                // Apply restitution to normal velocity (bounce)
                Vec3d reflectedNormal = normalVel.multiply(-surfaceBounciness);
                // Apply friction to tangent velocity (skid reduction)
                double normalImpulse = -(1.0 + surfaceBounciness) * normalDot;
                double maxFrictionImpulse = surfaceFriction * normalImpulse;
                double tangentSpeed = tangentVel.length();

                Vec3d reflectedTangent = tangentVel;
                if (tangentSpeed > 0.0001 && normalDot < 0) {
                    double frictionFactor = Math.max(0.65, 1.0 - (maxFrictionImpulse / tangentSpeed));
                    reflectedTangent = tangentVel.multiply(frictionFactor);
                }

                // Combine normal and tangent responses to form total bounce direction
                Vec3d reflectedVel = reflectedNormal.add(reflectedTangent);

                // Push ball slightly out of block skin (0.001) to prevent getting stuck in face
                Vec3d adjustedPos = collision.resolvedPos().add(normal.multiply(0.001));

                // Determine if landing hit converts airborne state to ground rolling state
                boolean hasGround = hasGroundSupport(world, adjustedPos, config.radius());
                double verticalImpactSpeed = Math.abs(normalVel.y);
                boolean lowVelocityImpact = verticalImpactSpeed < 0.08;

                boolean landsOnGround = normal.y > 0.7 && hasGround && lowVelocityImpact;

                if (landsOnGround) {
                    // Ball is moving slowly enough vertically to settle into a ground roll
                    reflectedVel = new Vec3d(reflectedVel.x * surfaceFriction, 0, reflectedVel.z * surfaceFriction);
                } else {
                    // Preserve calculated vertical bounce from normalVel * -surfaceBounciness
                    currentlyOnGround = false;
                }

                // Trigger collision events
                if(isRealTick){
                    GolfBallEntityEvents.ON_COLLISION.invoker().onCollision(
                            world, ball, impactBlockPos, world.getBlockState(impactBlockPos), normal, impactSpeed
                    );
                }

                // Update intermediate variables for next substep iteration
                currentCenter = adjustedPos;
                currentVel = reflectedVel;
                currentSpin = currentSpin.multiply(0.98);

                // Stop remaining substeps if ball came to a halt
                if (currentlyOnGround && currentVel.lengthSquared() < 0.0001) {
                    currentVel = Vec3d.ZERO;
                    break;
                }
                stepVel = currentVel.multiply(1.0 / (substeps - i));
            } else {
                // Free movement: No hit detected, move forward
                currentCenter = nextCenter;
                // Check if ball rolled off edge during mid-step
                if (currentlyOnGround && !hasGroundSupport(world, currentCenter, config.radius())) {
                    currentlyOnGround = false;
                }
            }
        }

        // 5. Post-step ground re-check
        currentlyOnGround = hasGroundSupport(world, currentCenter, config.radius());

        // Return new state with updated position, velocity, decay spin, and ground boolean
        return new State(
                currentCenter,
                currentVel,
                currentlyOnGround ? currentSpin.multiply(0.98) : currentSpin.multiply(0.99),
                currentlyOnGround
        );
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
    public static SphereCollision resolveSphereCollision(World world, Vec3d intendedCenter, Vec3d previousCenter, double radius) {
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
                        // Sphere center inside box: calculate push normal opposed to movement vector
                        double minX = currentCenter.x - box.minX;
                        double maxX = box.maxX - currentCenter.x;
                        double minY = currentCenter.y - box.minY;
                        double maxY = box.maxY - currentCenter.y;
                        double minZ = currentCenter.z - box.minZ;
                        double maxZ = box.maxZ - currentCenter.z;

                        Vec3d moveDir = intendedCenter.subtract(previousCenter);
                        double absX = Math.abs(moveDir.x);
                        double absY = Math.abs(moveDir.y);
                        double absZ = Math.abs(moveDir.z);

                        if (absX >= absY && absX >= absZ && absX > 1e-6) {
                            if (moveDir.x > 0) {
                                pushNormal = new Vec3d(-1, 0, 0);
                                penetrationDepth = radius + minX;
                            } else {
                                pushNormal = new Vec3d(1, 0, 0);
                                penetrationDepth = radius + maxX;
                            }
                        } else if (absY >= absX && absY >= absZ && absY > 1e-6) {
                            if (moveDir.y > 0) {
                                pushNormal = new Vec3d(0, -1, 0);
                                penetrationDepth = radius + minY;
                            } else {
                                pushNormal = new Vec3d(0, 1, 0);
                                penetrationDepth = radius + maxY;
                            }
                        } else if (absZ > 1e-6) {
                            if (moveDir.z > 0) {
                                pushNormal = new Vec3d(0, 0, -1);
                                penetrationDepth = radius + minZ;
                            } else {
                                pushNormal = new Vec3d(0, 0, 1);
                                penetrationDepth = radius + maxZ;
                            }
                        } else {
                            pushNormal = new Vec3d(0, 1, 0);
                            penetrationDepth = radius + maxY;
                        }
                    }

                    accumulatedNormal = accumulatedNormal.add(pushNormal);
                    currentCenter = currentCenter.add(pushNormal.multiply(penetrationDepth));
                }
            }
        }
        return new SphereCollision(currentCenter, collided ? accumulatedNormal.normalize() : Vec3d.ZERO, collided);
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

        // Grass & Turf (Fairway / Greens)
        if (state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.MOSS_BLOCK) ||
                state.isOf(Blocks.MYCELIUM) || state.isIn(BlockTags.WOOL_CARPETS)) {
            return 0.55;
        }

        // Dirt & Mud (Rough) - Absorbs more kinetic energy than fairway grass
        if (state.isIn(BlockTags.DIRT) || state.isOf(Blocks.MUD) || state.isOf(Blocks.FARMLAND)) {
            return 0.35;
        }

        // Ice (Very Slick & Elastic) - Covers Ice, Packed Ice, Blue Ice
        if (state.isIn(BlockTags.ICE)) return 0.80;

        // Hard Surfaces (Stone, Concrete, Metal, Wood, Bricks)
        if (state.isIn(BlockTags.STONE_BRICKS) || state.isIn(BlockTags.BASE_STONE_OVERWORLD) ||
                state.isIn(BlockTags.PLANKS) || state.isOf(Blocks.COPPER_BLOCK) || state.isOf(Blocks.IRON_BLOCK)) {
            return 0.70;
        }

        // Extreme Bounce (Arcade/Special)
        if (state.isOf(Blocks.SLIME_BLOCK)) return 0.95;

        // Soft Dampeners (Wool, Snow)
        if (state.isIn(BlockTags.WOOL) || state.isIn(BlockTags.SNOW)) {
            return 0.25;
        }

        // Bunkers & Tree Canopies (Absorbs nearly all kinetic energy)
        if (state.isIn(BlockTags.SAND) || state.isIn(BlockTags.LEAVES)) {
            return 0.10;
        }

        // Default Fallback (General Hard Blocks)
        return 0.70;
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
            current = step(world, ball, current, config, false);

            // Convert all predicted steps back to feet positions
            points.add(current.pos().subtract(0, config.radius(), 0));

            // Stop predicting if the ball lands and stops moving
            if (current.onGround() && current.vel().lengthSquared() < 0.0001) {
                break;
            }
        }

        return resampleTrajectory(points, 0.5);
    }

    /**
     * Trajectory resampling helper
     */
    public static List<Vec3d> resampleTrajectory(List<Vec3d> rawPoints, double spacing) {
        if (rawPoints.size() < 2 || spacing <= 0) return rawPoints;

        List<Vec3d> resampled = new ArrayList<>();
        resampled.add(rawPoints.get(0));

        double accumulatedDist = 0.0;

        for (int i = 0; i < rawPoints.size() - 1; i++) {
            Vec3d p1 = rawPoints.get(i);
            Vec3d p2 = rawPoints.get(i + 1);
            double segmentLength = p1.distanceTo(p2);

            if (segmentLength <= 0.0001) continue;

            double segmentOffset = 0.0;

            while (accumulatedDist + (segmentLength - segmentOffset) >= spacing) {
                double needed = spacing - accumulatedDist;
                segmentOffset += needed;

                double t = segmentOffset / segmentLength;
                Vec3d interpolated = p1.add(p2.subtract(p1).multiply(t));
                resampled.add(interpolated);

                accumulatedDist = 0.0;
            }

            accumulatedDist += (segmentLength - segmentOffset);
        }

        return resampled;
    }
}
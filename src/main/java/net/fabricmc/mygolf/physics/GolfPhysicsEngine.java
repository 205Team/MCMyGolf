package net.fabricmc.mygolf.physics;

import net.fabricmc.mygolf.blocks.GolfHole;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.events.GolfBallEntityEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
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

    public record SphereCollision(Vec3d resolvedPos, Vec3d normal, boolean hit, boolean isStepUp, double stepHeight) {}

    /**
     * Integrates one tick of flight, drag, Magnus forces, and continuous collision response.
     */
    public static State step(World world, GolfBallEntity ball, State current, Config config, boolean isRealTick) {
        Vec3d vel = current.vel();
        Vec3d spin = current.spin();
        Vec3d pos = current.pos();
        boolean onGround = current.onGround();

        // 1. Determine ground support status before velocity calculations
        boolean hasGround = hasGroundSupport(world, pos, config.radius());
        if (onGround) {
            if (vel.y > 0.01 || vel.y < -0.025 || !hasGround) {
                onGround = false;
                spin = spin.multiply(0.3D);
            }
        } else {
            // Promote freshly spawned or landing entities to ground state if supported
            if (hasGround && Math.abs(vel.y) < 0.025) {
                onGround = true;
            }
        }

        // 2. Calculate new tick velocity
        Vec3d newVel;
        if (onGround) {
            BlockPos groundPos = BlockPos.ofFloored(pos.x, pos.y - config.radius() - 0.01, pos.z);
            double groundFriction = getSurfaceFriction(world, groundPos);

            // Decelerate horizontal sliding velocity using ground friction
            newVel = new Vec3d(vel.x * groundFriction, 0, vel.z * groundFriction);

            // Convert linear ground velocity into angular velocity vector (spinVector)
            double radius = config.radius();
            Vec3d targetSpin = new Vec3d(newVel.z / radius, 0, -newVel.x / radius);
            double gripFactor = 0.35D;
            spin = spin.add(targetSpin.subtract(spin).multiply(gripFactor));

            if (newVel.lengthSquared() < 1e-6) {
                return new State(pos, Vec3d.ZERO, Vec3d.ZERO, true);
            }
        } else {
            // Check if the center of the ball is submerged in a liquid
            BlockPos currentPos = BlockPos.ofFloored(pos);
            boolean isInFluid = !world.getFluidState(currentPos).isEmpty();
            BlockState currentBlockState = world.getBlockState(currentPos);
            boolean isInCobweb = currentBlockState.isOf(Blocks.COBWEB);

            if (isInCobweb) {
                // Cobweb Physics: High velocity dampening and low gravity trickle
                double cobwebDrag = 0.25; // Heavily cuts horizontal & vertical momentum per tick
                double cobwebGravity = config.gravity() * 0.05; // Slow downward fall

                newVel = vel.multiply(cobwebDrag).subtract(0, cobwebGravity, 0);
                spin = spin.multiply(0.2D); // Rapidly kill rotational energy
            } else if (isInFluid) {
                // Fluid Physics: Heavy drag + Buoyancy lift
                double fluidDrag = 0.82;     // High resistance (slows down fast entries)
                double netGravity = -config.gravity() + config.buoyancy(); // Default: -0.035 + 0.028 = -0.007

                newVel = vel.multiply(fluidDrag).add(0, netGravity, 0);
                spin = spin.multiply(0.5D); // Rapidly kill rotational energy
            } else {
                // Air Physics: Standard drag + Gravity + Magnus force
                spin = spin.multiply(0.98D);
                double maxAirSpin = 1.5D; // Maximum rad/tick
                if (spin.lengthSquared() > maxAirSpin * maxAirSpin) {
                    spin = spin.normalize().multiply(maxAirSpin);
                }
                Vec3d magnusForce = Vec3d.ZERO;
                if (GolfBallEntity.ENABLE_MAGNUS_EFFECT && vel.lengthSquared() > 1e-6) {
                    magnusForce = spin.crossProduct(vel.normalize()).multiply(config.liftCoefficient() * 0.3D);

                    // Limit positive +Y lift force to max 80% of gravity so the ball always descends
                    double maxUpwardLift = config.gravity() * 0.8D;
                    if (magnusForce.y > maxUpwardLift) {
                        magnusForce = new Vec3d(magnusForce.x, maxUpwardLift, magnusForce.z);
                    }
                }
                newVel = vel.multiply(config.dragCoefficient()).add(0, -config.gravity(), 0).add(magnusForce);
            }
        }

        // 3. Substep movement (prevents fast balls from skipping over block borders)
        double velocityMagnitude = newVel.length();
        double maxStep = config.radius() * 0.4;
        int substeps = Math.max(1, (int) Math.ceil(velocityMagnitude / maxStep));
        substeps = Math.min(substeps, 20);

        double invSubsteps = 1.0 / substeps;
        Vec3d currentCenter = pos;
        Vec3d currentVel = newVel;
        Vec3d currentSpin = spin;
        boolean currentlyOnGround = onGround;
        boolean stepUppedInThisTick = false;

        // 4. Ray/Sphere sweep movement tick by tick
        for (int i = 0; i < substeps; i++) {
            Vec3d stepVel = currentVel.multiply(invSubsteps);
            Vec3d nextCenter = currentCenter.add(stepVel);
            SphereCollision collision = resolveSphereCollision(world, nextCenter, currentCenter, config.radius());

            if (collision.hit()) {
                if (isRealTick) {
                    logSurroundingBlocks(world, collision.resolvedPos(), 1); //
                }
                // --- Step-Up Response Logic ---
                if (collision.isStepUp()) {
                    currentCenter = collision.resolvedPos();

                    if(!stepUppedInThisTick) {
                        stepUppedInThisTick = true;

                        double velX = currentVel.x;
                        double velZ = currentVel.z;
                        double horizSpeedSq = velX * velX + velZ * velZ;

                        if (horizSpeedSq > 1e-6) {
                            double horizSpeed = Math.sqrt(horizSpeedSq);
                            double stepHeight = collision.stepHeight(); // e.g. box.maxY - previousBottomY
                            double gravity = config.gravity(); // 0.035 blocks/tick^2

                            // Minimum speed needed to overcome potential energy of step height: v = sqrt(2 * g * h)
                            double requiredSpeed = Math.sqrt(2.0 * gravity * stepHeight);

                            if (horizSpeed >= requiredSpeed) {
                                // --- HIGH SPEED: Convert horizontal speed to vertical climb ---
                                double conversionFactor = 0.35;// Transfer ~35% of horizontal speed into vertical velocity
                                double addedVelY = horizSpeed * conversionFactor;
                                // Conserve kinetic energy
                                double remainingHorizSpeedSq = Math.max(0.0, horizSpeedSq - (addedVelY * addedVelY));
                                double scale = Math.sqrt(remainingHorizSpeedSq) / horizSpeed;

                                currentVel = new Vec3d(velX * scale, currentVel.y + addedVelY, velZ * scale);
                            } else {
                                // --- LOW SPEED: Rebound off step face and roll back under gravity ---
                                double reboundFactor = 0.25; // Energy retained on horizontal bounce
                                double smallVerticalPop = horizSpeed * 0.15; // Minor upward nudge

                                // Reverse horizontal direction so the ball rolls backward
                                currentVel = new Vec3d(-velX * reboundFactor, currentVel.y + smallVerticalPop, -velZ * reboundFactor);
                            }
                        }
                    }
                    continue;
                }

                // --- Regular Collision Response (Walls, Bounces, Slopes) ---
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
                double rollingRetention = getSurfaceFriction(world, impactBlockPos);
                double dynamicFrictionCoeff = (1.0 - rollingRetention) * 1.5;
                double normalImpulse = -(1.0 + surfaceBounciness) * normalDot;
                double maxFrictionImpulse = dynamicFrictionCoeff * normalImpulse;
                double tangentSpeed = tangentVel.length();
                Vec3d reflectedTangent = tangentVel;
                if (tangentSpeed > 0.0001 && normalDot < 0 && !currentlyOnGround) {
                    double frictionFactor = Math.max(0.0, 1.0 - (maxFrictionImpulse / tangentSpeed));
                    reflectedTangent = tangentVel.multiply(frictionFactor);
                }

                // Combine normal and tangent responses to form total bounce direction
                Vec3d reflectedVel = reflectedNormal.add(reflectedTangent);
                Vec3d adjustedPos = collision.resolvedPos();

                // Determine if landing hit converts airborne state to ground rolling state
                hasGround = hasGroundSupport(world, adjustedPos, config.radius());
                double verticalImpactSpeed = Math.abs(normalVel.y);
                boolean lowVelocityImpact = verticalImpactSpeed < 0.025;
                boolean landsOnGround = normal.y > 0.7 && hasGround && lowVelocityImpact;

                if (landsOnGround) {
                    // Ball is moving slowly enough vertically to settle into a ground roll
                    currentlyOnGround = true;
                    reflectedVel = new Vec3d(reflectedVel.x, 0.0, reflectedVel.z);
                    if (!onGround) {
                        Vec3d targetSpin = new Vec3d(reflectedVel.z / config.radius(), 0, -reflectedVel.x / config.radius());
                        currentSpin = currentSpin.add(targetSpin.subtract(currentSpin).multiply(0.35D));
                    }
                } else {
                    // Preserve calculated vertical bounce from normalVel * -surfaceBounciness
                    currentlyOnGround = false;

                    // Convert tangential sliding friction into rotational torque
                    double spinRestitution = 0.35D;
                    currentSpin = currentSpin.multiply(spinRestitution * (1.0D - surfaceFriction * 0.5D));
                    if (tangentSpeed > 0.001) {
                        Vec3d impactTorque = normal.crossProduct(tangentVel).multiply(surfaceFriction / config.radius());
                        currentSpin = currentSpin.add(impactTorque.multiply(0.25D));
                    }
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

                // Stop remaining substeps if ball came to a halt
                if (currentlyOnGround && currentVel.lengthSquared() < 1e-6) {
                    currentVel = Vec3d.ZERO;
                    break;
                }
            } else {
                // Free movement: No hit detected, move forward
                currentCenter = nextCenter;
                // Check if ball rolled off edge during mid-step
                if (currentlyOnGround && !hasGroundSupport(world, currentCenter, config.radius())) {
                    currentlyOnGround = false;
                    currentSpin = currentSpin.multiply(0.3);
                }
            }
        }

        // 5. Post-step ground re-check
        boolean supported = hasGroundSupport(world, currentCenter, config.radius());
        currentlyOnGround = supported && Math.abs(currentVel.y) < 0.025;

        // Return new state with updated position, velocity, decay spin, and ground boolean
        return new State(
                currentCenter,
                currentVel,
                currentlyOnGround ? currentSpin.multiply(0.98) : currentSpin.multiply(0.99),
                currentlyOnGround
        );
    }public static void logSurroundingBlocks(World world, Vec3d collisionPoint, int radius) {
        BlockPos centerPos = BlockPos.ofFloored(collisionPoint);
        System.out.printf("[SURROUNDING BLOCKS] Center: %s | Exact: [%.4f, %.4f, %.4f]%n",
                centerPos.toShortString(), collisionPoint.x, collisionPoint.y, collisionPoint.z);

        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos targetPos = centerPos.add(dx, dy, dz);
                    BlockState state = world.getBlockState(targetPos);

                    if (!state.isAir()) {
                        VoxelShape shape = state.getCollisionShape(world, targetPos);
                        System.out.printf("  Offset [%+d, %+d, %+d] -> %s | MaxY: %.2f | Boxes: %s%n",
                                dx, dy, dz,
                                state.getBlock().getName().getString(),
                                targetPos.getY() + (shape.isEmpty() ? 0 : shape.getMax(Direction.Axis.Y)),
                                shape.isEmpty() ? "NONE" : shape.getBoundingBoxes());
                    }
                }
            }
        }
    }

    /**
     * Checks if there are any solid collision shapes supporting the bottom of the ball.
     */
    public static boolean hasGroundSupport(World world, Vec3d centerPos, double radius) {
        double probeWidth = 0.01;

        Box groundCheckArea = new Box(
                centerPos.x - probeWidth, centerPos.y - radius - 0.01, centerPos.z - probeWidth,
                centerPos.x + probeWidth, centerPos.y - radius + 0.01, centerPos.z + probeWidth
        );

        // Returns true if there is at least one solid collision shape in this area
        return world.getBlockCollisions(null, groundCheckArea).iterator().hasNext();
    }

    /**
     * Collision helper
     */
    public static SphereCollision resolveSphereCollision(World world, Vec3d intendedCenter, Vec3d previousCenter, double radius) {
        Box searchBox = new Box(
                intendedCenter.x - radius, intendedCenter.y - radius, intendedCenter.z - radius,
                intendedCenter.x + radius, intendedCenter.y + radius, intendedCenter.z + radius
        );
        Iterable<VoxelShape> shapes = world.getBlockCollisions(null, searchBox);

        final double radiusSq = radius * radius;
        final double previousBottomY = previousCenter.y - radius;
        final double previousTopY = previousCenter.y + radius;
        double currentX = intendedCenter.x;
        double currentY = intendedCenter.y;
        double currentZ = intendedCenter.z;
        double accNormX = 0.0;
        double accNormY = 0.0;
        double accNormZ = 0.0;

        boolean collided = false;
        boolean stepUpOccurred = false;
        double recordedStepHeight = 0.0;

        for (VoxelShape shape : shapes) {
            for (Box box : shape.getBoundingBoxes()) {
                double closestX = Math.max(box.minX, Math.min(currentX, box.maxX));
                double closestY = Math.max(box.minY, Math.min(currentY, box.maxY));
                double closestZ = Math.max(box.minZ, Math.min(currentZ, box.maxZ));

                double dx = currentX - closestX;
                double dy = currentY - closestY;
                double dz = currentZ - closestZ;
                double distanceSq = dx * dx + dy * dy + dz * dz;

                boolean centerInsideX = currentX >= box.minX && currentX <= box.maxX;
                boolean centerInsideZ = currentZ >= box.minZ && currentZ <= box.maxZ;

                // 1. Y-height check for top surface contact
                boolean isTopSurfaceContact = false;
                if (currentY >= box.maxY - 0.025
                        && currentY - radius <= box.maxY + 0.01
                        && previousTopY >= box.maxY - 0.05) {

                    if (centerInsideX && centerInsideZ) {
                        isTopSurfaceContact = true;
                    } else {
                        // Only query world state if ball is vertically in range
                        isTopSurfaceContact = checkEdgeSupport(world, box, currentX, currentZ, closestX, closestZ, centerInsideX, centerInsideZ);
                    }
                }

                // 2. Step-up check using squared horizontal speed
                boolean isStepLedge = false;
                if (!isTopSurfaceContact) {
                    double velX = currentX - previousCenter.x;
                    double velZ = currentZ - previousCenter.z;
                    double horizSpeedSq = velX * velX + velZ * velZ;

                    if (horizSpeedSq > 0.000025) { // Equivalent to horizSpeed > 0.005
                        double stepHeight = box.maxY - previousBottomY;
                        if (stepHeight > 0.001 && stepHeight <= 0.08 && (currentY - radius) < box.maxY) {
                            boolean isMovingTowardBlock =
                                    (previousCenter.x < box.minX && velX > 0.001) ||
                                            (previousCenter.x > box.maxX && velX < -0.001) ||
                                            (previousCenter.z < box.minZ && velZ > 0.001) ||
                                            (previousCenter.z > box.maxZ && velZ < -0.001);
                            if (isMovingTowardBlock) {
                                isStepLedge = true;
                            }
                        }
                    }
                }

                // 3. Resolve collision response
                if (distanceSq < radiusSq || isTopSurfaceContact || isStepLedge) {
                    collided = true;
                    double pushX, pushY, pushZ;
                    double penetrationDepth;

                    if (isTopSurfaceContact) {
                        pushX = 0.0; pushY = 1.0; pushZ = 0.0;
                        penetrationDepth = Math.max(0.0, (box.maxY + radius) - currentY);
                    } else if (isStepLedge) {
                        pushX = 0.0; pushY = 1.0; pushZ = 0.0;
                        penetrationDepth = Math.max(0.0, (box.maxY + radius) - currentY);
                        stepUpOccurred = true;
                        recordedStepHeight = box.maxY - previousBottomY;
                    } else if (distanceSq > 0.00001) {
                        double distance = Math.sqrt(distanceSq);
                        pushX = dx / distance;
                        pushY = dy / distance;
                        pushZ = dz / distance;
                        penetrationDepth = radius - distance;
                    } else {
                        pushX = 0.0; pushY = 1.0; pushZ = 0.0;
                        penetrationDepth = radius + (box.maxY - currentY);
                    }

                    accNormX += pushX;
                    accNormY += pushY;
                    accNormZ += pushZ;
                    currentX += pushX * penetrationDepth;
                    currentY += pushY * penetrationDepth;
                    currentZ += pushZ * penetrationDepth;

                    //debug
                    System.out.printf("[COLLISION DEBUG] Box X:[%.2f..%.2f] Y_max:%.2f | Center: [%.4f, %.4f] | centerInsideX: %b | topContact: %b | stepUp: %b | Pen: %.4f | stepHeight: %.4f%n",
                            box.minX, box.maxX, box.maxY,
                            currentX, currentY,
                            centerInsideX, isTopSurfaceContact, isStepLedge,
                            penetrationDepth, recordedStepHeight);
                }
            }
        }

        Vec3d finalNormal = Vec3d.ZERO;
        if (collided) {
            double lenSq = accNormX * accNormX + accNormY * accNormY + accNormZ * accNormZ;
            if (lenSq > 1e-6) {
                double invLen = 1.0 / Math.sqrt(lenSq);
                finalNormal = new Vec3d(accNormX * invLen, accNormY * invLen, accNormZ * invLen);
            } else {
                finalNormal = new Vec3d(0, 1, 0);
            }
        }

        return new SphereCollision(new Vec3d(currentX, currentY, currentZ), finalNormal, collided, stepUpOccurred, recordedStepHeight);
    }

    /**
     * Helper: edge support evaluation
     */
    private static boolean checkEdgeSupport(World world, Box box, double currentX, double currentZ, double closestX, double closestZ, boolean centerInsideX, boolean centerInsideZ) {
        if (!centerInsideX) {
            boolean isWestOuterBoundary = Math.abs(box.minX - Math.floor(box.minX)) < 0.001;
            boolean isEastOuterBoundary = Math.abs(box.maxX - Math.ceil(box.maxX)) < 0.001;

            if (currentX < box.minX && isWestOuterBoundary) {
                BlockPos westNeighbor = BlockPos.ofFloored(box.minX - 0.5, box.maxY - 0.05, closestZ);
                if (hasSurfaceSupportAt(world, westNeighbor, box.maxY)) return true;
            } else if (currentX > box.maxX && isEastOuterBoundary) {
                BlockPos eastNeighbor = BlockPos.ofFloored(box.maxX + 0.5, box.maxY - 0.05, closestZ);
                if (hasSurfaceSupportAt(world, eastNeighbor, box.maxY)) return true;
            }
        }
        if (!centerInsideZ) {
            boolean isNorthOuterBoundary = Math.abs(box.minZ - Math.floor(box.minZ)) < 0.001;
            boolean isSouthOuterBoundary = Math.abs(box.maxZ - Math.ceil(box.maxZ)) < 0.001;

            if (currentZ < box.minZ && isNorthOuterBoundary) {
                BlockPos northNeighbor = BlockPos.ofFloored(closestX, box.maxY - 0.05, box.minZ - 0.5);
                if (hasSurfaceSupportAt(world, northNeighbor, box.maxY)) return true;
            } else if (currentZ > box.maxZ && isSouthOuterBoundary) {
                BlockPos southNeighbor = BlockPos.ofFloored(closestX, box.maxY - 0.05, box.maxZ + 0.5);
                if (hasSurfaceSupportAt(world, southNeighbor, box.maxY)) return true;
            }
        }
        return false;
    }

    /**
     * Helper: checks if a neighbor block position has a collision surface matching target Y height.
     */
    private static boolean hasSurfaceSupportAt(World world, BlockPos pos, double targetMaxY) {
        VoxelShape shape = world.getBlockState(pos).getCollisionShape(world, pos);
        if (shape.isEmpty()) return false;
        for (Box b : shape.getBoundingBoxes()) {
            double worldMaxY = pos.getY() + b.maxY;
            if (Math.abs(worldMaxY - targetMaxY) < 0.05) {
                return true;
            }
        }
        return false;
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

        if (state.isIn(BlockTags.SAND)) return 0.62;       // Bunkers kill horizontal momentum
        if (state.isIn(BlockTags.LEAVES)) return 0.50;   // More likely to get stuck on trees
        if (state.isOf(Blocks.SLIME_BLOCK)) return 0.30;   // Slime sticks and absorbs slide energy
        if (state.isOf(Blocks.GREEN_CARPET) || state.isOf(Blocks.LIME_CARPET)
                || state.isOf(Blocks.MOSS_CARPET) || state.isOf(Blocks.MOSS_BLOCK)) {
            return 0.955; // Long, smooth green roll
        }

        // Map native MC slipperiness (0.60 default -> 0.92 rolling friction)
        double mcSlipperiness = state.getBlock().getSlipperiness();
        return 0.80 + (mcSlipperiness * 0.19);
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
            if (current.onGround() && current.vel().lengthSquared() < 1e-6) {
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

    /**
     * Queries block geometry to detect bounding box overlap.
     */
    public static boolean isClipping(World world, GolfBallEntity entity, Box box) {
        Box contractedBox = box.contract(0.04);
        return world.getBlockCollisions(entity, contractedBox).iterator().hasNext();
    }
}
package net.fabricmc.mygolf.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.mygolf.MyGolfMod;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.items.GolfBall;
import net.fabricmc.mygolf.items.GolfClubItem;
import net.fabricmc.mygolf.registry.RegisterItems;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class GolfBallEntityEvents {

    /**
     * 1. COLLISION EVENT DEFINITION
     */
    @FunctionalInterface
    public interface Collision {
        void onCollision(World world, GolfBallEntity ball, BlockPos pos, BlockState state, Vec3d normal, double speed);
    }

    public static final Event<Collision> ON_COLLISION = EventFactory.createArrayBacked(
            Collision.class,
            (listeners) -> (world, ball, pos, state, normal, speed) -> {
                for (Collision listener : listeners) {
                    listener.onCollision(world, ball, pos, state, normal, speed);
                }
            }
    );

    /**
     * 2. HOLE ENTER EVENT DEFINITION
     */
    @FunctionalInterface
    public interface BallEnterHole {
        void onBallEnterHole(World world, GolfBallEntity ball, BlockPos holePos);
    }

    public static final Event<BallEnterHole> ON_HOLE_ENTER = EventFactory.createArrayBacked(
            BallEnterHole.class,
            (listeners) -> (world, ball, holePos) -> {
                for (BallEnterHole listener : listeners) {
                    listener.onBallEnterHole(world, ball, holePos);
                }
            }
    );

    /**
     * 3. EVENT REGISTRATION & HANDLERS
     */
    public static void registerEvents() {

        // 1. LEFT-CLICK (Hit Ball)
        //        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
        //            // Only intervene if the player is attacking a GolfBallEntity
        //            if (entity instanceof GolfBallEntity ball && !player.isSpectator()) {
        //                // Ensure player is holding a golf club
        //                if (player.getStackInHand(hand).getItem() instanceof GolfClubItem) {
        //                    if (!world.isClient()) {
        //                        // Calculate and apply impact on ball
        //                        // 根据玩家与球的位置计算
        //                        Vec3d ballPos = entity.getPos();
        //                        Vec3d playerPos = player.getPos();
        //                        Vec3d posDelta3d = ballPos.subtract(playerPos);
        //                        // Vector3f posDelta3f = new Vector3f((float) posDelta3d.x,(float) posDelta3d.y,(float) posDelta3d.z);
        //                        Vec2f posDelta2f = new Vec2f((float) posDelta3d.x, (float) posDelta3d.z); //水平方向向量差
        //                        float hitDistance = posDelta2f.length(); //向量模
        //                        Vec2f hitDirection = posDelta2f.normalize(); //单位向量
        //                        float hitPitch = (30 - hitDistance) / 5;
        //                        Vec3d impulse = new Vec3d(hitDirection.x * 5, hitPitch, hitDirection.y * 5);
        //                        // 给球冲量
        //                        ball.applyImpulse(impulse);
        //                        // Give 1 hit point to ball
        //                        ball.incrementHitCount();
        //                        MyGolfMod.LOGGER.info("击打次数为" + ball.getHitCount());
        //                    }
        //
        //                    // Return SUCCESS to prevent default Minecraft attack damage behavior
        //                    return ActionResult.SUCCESS;
        //                }
        //            }
        //            return ActionResult.PASS; // Let vanilla handle other entity attacks
        //        });

        // 2. RIGHT-CLICK (Pick Up Ball, Entity → Item)
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof GolfBallEntity ballEntity && !player.isSpectator()) {
                if (hand == Hand.MAIN_HAND) {
                    ItemStack mainHandStack = player.getMainHandStack();
                    ItemStack ballStack = createStackFromEntity(ballEntity);

                    // 1. Pickup with Empty Hand
                    if (mainHandStack.isEmpty()) {
                        if (!world.isClient() && player.isInSneakingPose()) {
                            ballEntity.discard();
                            player.setStackInHand(Hand.MAIN_HAND, ballStack);
                        }
                        return ActionResult.SUCCESS;
                    }

                    // 2. Pickup / Stack with Golf Ball
                    if (mainHandStack.isOf(RegisterItems.GOLF_BALL)) {
                        if (!world.isClient() && player.isInSneakingPose()) {
                            ballEntity.discard();
                            if (ItemStack.canCombine(mainHandStack, ballStack) && mainHandStack.getCount() < mainHandStack.getMaxCount()) {
                                if (!player.isCreative()) {
                                    mainHandStack.increment(1);
                                }
                            } else {
                                player.giveItemStack(ballStack);
                            }
                        }
                        return ActionResult.SUCCESS;
                    }
                }
            }
            return ActionResult.PASS;
        });

        // 3. Block Collision Reaction (Sounds based on impact speed)
        ON_COLLISION.register((world, ball, pos, state, normal, speed) -> {
            if (!world.isClient() && speed > 0.1) {
                System.out.println("collision!");   //Debug
                world.playSound(
                        null,
                        pos,
                        state.getSoundGroup().getHitSound(),
                        SoundCategory.BLOCKS,
                        (float) Math.min(speed, 1.0),
                        1.0F
                );
            }

        });

        // 4. Hole Entry Reaction (Sounds, fireworks, ball cleanup)
        ON_HOLE_ENTER.register((world, ball, holePos) -> {
            if (world instanceof ServerWorld serverWorld) {

                if (!ball.isGoaled()) {// Play cup drop sound
                    serverWorld.playSound(
                            null,
                            holePos,
                            SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST,
                            SoundCategory.BLOCKS,
                            1.0F,
                            0.8F + world.random.nextFloat() * 0.4F
                    );
                    // Spawn celebration particles
                    serverWorld.spawnParticles(
                            ParticleTypes.FIREWORK,
                            holePos.getX(), holePos.getY() + 5, holePos.getZ(),
                            15, 0.2, 0.2, 0.2, 0.1
                    );
                }

                ball.setGoaled(true);
            }
        });
    }

    /**
     * Creates an ItemStack from a GolfBallEntity preserving all NBT properties.
     */
    private static ItemStack createStackFromEntity(GolfBallEntity ballEntity) {
        ItemStack ballStack = new ItemStack(RegisterItems.GOLF_BALL);

        // Pass name
        if (ballEntity.hasCustomName()) {
            ballStack.setCustomName(ballEntity.getCustomName());
        }
        // Pass isgoal
        if (ballEntity.isGoaled()) {
            ballStack.getOrCreateNbt().putBoolean("IsGoaled", true);
        }
        // Pass hitcount
        if (ballEntity.getHitCount() > 0) {
            ballStack.getOrCreateNbt().putInt("HitCount", ballEntity.getHitCount());
        }
        // Pass color
        int ballColor = ballEntity.getColor();
        if (ballColor != -1 && ballColor != 0xFFFFFF && ballColor != 0xF9FFFE) {
            RegisterItems.GOLF_BALL.setColor(ballStack, ballColor);
        }
        // Wipe empty NBT compound
        GolfBall.sanitizeNbt(ballStack);
        return ballStack;
    }
}
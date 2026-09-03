package net.fabricmc.mygolf.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.mygolf.MyGolfMod;
import net.fabricmc.mygolf.blockEntity.GolfHoleEntity;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.items.GolfBall;
import net.fabricmc.mygolf.items.GolfClubItem;
import net.fabricmc.mygolf.registry.RegisterItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
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
                    ItemStack ballStack = ballEntity.createStackFromEntity();

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
                world.playSound(
                        null,
                        pos,
                        state.getSoundGroup().getHitSound(),
                        SoundCategory.BLOCKS,
                        (float) Math.min(speed, 1.0),
                        1.0F
                );
            }
            if (!world.isClient()) {
                System.out.printf("collision at Pos [X: %d, Y: %d, Z: %d]", pos.getX(),pos.getY(),pos.getZ());   //Debug
                System.out.println(state);   //Debug
            }

        });

        // 4. Hole Entry Reaction (Sounds, fireworks, ball cleanup)
        ON_HOLE_ENTER.register((world, ball, holePos) -> {
            if (!(world instanceof ServerWorld serverWorld)) return;

            if (!ball.isGoaled()) {
                /// Set ball as goaled
                ball.setGoaled(true);

                /// Play sound
                serverWorld.playSound(
                        null,
                        holePos,
                        SoundEvents.ENTITY_PLAYER_LEVELUP,
                        SoundCategory.BLOCKS,
                        1.0F,
                        1.0F
                );

                /// Shoot fireworks
                ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
                NbtCompound fireworksNbt = fireworkStack.getOrCreateSubNbt("Fireworks");
                fireworksNbt.putByte("Flight", (byte) 1); // Flight duration
                NbtList explosions = new NbtList();
                NbtCompound explosion = new NbtCompound();
                explosion.putByte("Type", (byte) 1); // Large ball
                explosion.putByte("Flicker", (byte) 1);
                explosion.putIntArray("Colors", new int[]{
                        0xE57373, // Soft desaturated red
                        0xEF9A9A, // Light pastel red
                        0xF5F5F5, // Soft off-white
                        0xFFFFFF  // Pure white
                });
                explosions.add(explosion);
                fireworksNbt.put("Explosions", explosions);
                FireworkRocketEntity rocket = new FireworkRocketEntity(
                        serverWorld,
                        holePos.getX() + 0.75,
                        holePos.getY() + 1.0,
                        holePos.getZ() + 0.5,
                        fireworkStack
                );
                rocket.setVelocity(0.0, 0.45, 0.0);
                rocket.velocityModified = true;
                serverWorld.spawnEntity(rocket);

                /// Particles
                serverWorld.spawnParticles(
                        ParticleTypes.FIREWORK,
                        holePos.getX() + 0.5,
                        holePos.getY() + 2.0,
                        holePos.getZ() + 0.5,
                        30,   // Particle count
                        0.2,  // X spread
                        0.2,  // Y spread
                        0.2,  // Z spread
                        0.1   // Speed
                );

                /// Send message
                Text ballName = ball.getName();
                int hits = ball.getHitCount();
                long startTime = ball.getStartTime();
                ball.setStartTime(0L);
                // Timer
                long elapsedTicks = (startTime > 0) ? (serverWorld.getTime() - startTime) : 0;
                double totalSeconds = elapsedTicks / 20.0;
                MutableText timeText;
                if (totalSeconds >= 86400) {
                    timeText = Text.translatable("message.mygolf.time_over_day");
                } else if (totalSeconds >= 3600) {
                    long totalSecsInt = (long) totalSeconds;
                    long hours = totalSecsInt / 3600;
                    long minutes = (totalSecsInt % 3600) / 60;
                    long seconds = totalSecsInt % 60;
                    String formattedTime = String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
                    timeText = Text.translatable("message.mygolf.time", formattedTime);
                } else {
                    int minutes = (int) (totalSeconds / 60);
                    double seconds = totalSeconds % 60;
                    String formattedTime = String.format(java.util.Locale.ROOT, "%02d:%04.1f", minutes, seconds);
                    timeText = Text.translatable("message.mygolf.time", formattedTime);
                }
                // Score celebration message
                MutableText scoreText = switch (hits) {
                    case 1 -> Text.translatable("message.mygolf.hole_in_one");
                    case 2 -> Text.translatable("message.mygolf.eagle");
                    case 3 -> Text.translatable("message.mygolf.birdie");
                    case 4 -> Text.translatable("message.mygolf.par");
                    default -> Text.translatable("message.mygolf.finished_in", hits);
                };
                // Assemble the full message
                MutableText fullMessage = Text.empty()
                        .append(ballName)
                        .append(Text.literal(": "))
                        .append(scoreText)
                        .append(Text.literal(" "))
                        .append(timeText)
                        .formatted(Formatting.GOLD);
                // Broadcasts to everyone's chat window
                serverWorld.getServer().getPlayerManager().broadcast(fullMessage, false);
            }

            BlockEntity blockEntity = serverWorld.getBlockEntity(holePos);
            if (blockEntity instanceof GolfHoleEntity holeEntity) {
                /// Send redstone signal
                holeEntity.setLastHitCount(ball.getHitCount());
                /// Try putting in hole container
                ItemStack ballStack = ball.createStackFromEntity();
                if (!ball.isRemoved() && holeEntity.insertStack(ballStack)) {
                    ball.discard();
                }
            }

        });
    }
}
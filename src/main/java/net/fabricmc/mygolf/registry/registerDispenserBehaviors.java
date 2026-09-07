package net.fabricmc.mygolf.registry;

import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class registerDispenserBehaviors {
    public static void register() {
        DispenserBlock.registerBehavior(RegisterItems.GOLF_BALL, new ItemDispenserBehavior() {
            @Override
            protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
                World world = pointer.getWorld();

                if (!world.isClient()) {
                    // Get dispenser exit position and facing direction
                    Position position = DispenserBlock.getOutputLocation(pointer);
                    Direction direction = pointer.getBlockState().get(DispenserBlock.FACING);

                    // Initialize the entity
                    GolfBallEntity golfBall = new GolfBallEntity(RegisterEntities.GOLF_BALL, world);
                    golfBall.updatePosition(position.getX(), position.getY(), position.getZ());

                    // 1. Copy item dye color and metadata
                    if (RegisterItems.GOLF_BALL.hasColor(stack)) {
                        int itemColor = RegisterItems.GOLF_BALL.getColor(stack);
                        golfBall.setColor(itemColor == 0xFFFFFF ? -1 : itemColor);
                    } else {
                        golfBall.setColor(-1);
                    }

                    // 2. Copy custom name & NBT data
                    if (stack.hasCustomName()) {
                        golfBall.setCustomName(stack.getName());
                    }

                    if (stack.hasNbt() && stack.getNbt() != null) {
                        var nbt = stack.getNbt();
                        if (nbt.contains("StartTime")) golfBall.setStartTime(nbt.getLong("StartTime"));
                        if (nbt.containsUuid("OwnerUUID")) golfBall.setOwnerUuid(nbt.getUuid("OwnerUUID"));
                        if (nbt.contains("IsGoaled")) golfBall.setGoaled(nbt.getBoolean("IsGoaled"));
                        if (nbt.contains("HitCount")) golfBall.setHitCount(nbt.getInt("HitCount"));
                    }

                    // 3. Calculate and set initial launch vector
                    double power = 0.8; // Velocity magnitude multiplier
                    Vec3d launchVelocity = new Vec3d(
                            direction.getOffsetX() * power,
                            direction.getOffsetY() * power + (direction == Direction.UP ? 0.0 : 0.1), // slight vertical elevation
                            direction.getOffsetZ() * power
                    );
                    golfBall.setVelocity(launchVelocity);

                    // 4. Spawn ball in world & decrement stack
                    world.spawnEntity(golfBall);
                    stack.decrement(1);

                }
                return stack;
            }

            @Override
            protected void playSound(BlockPointer pointer) {
                // Play standard dispenser launch sound
                pointer.getWorld().syncWorldEvent(1000, pointer.getPos(), 0);
            }

            @Override
            protected void spawnParticles(BlockPointer pointer, Direction side) {
                // Spawn smoke particles at the output face
                pointer.getWorld().syncWorldEvent(2000, pointer.getPos(), side.getId());
            }
        });
    }
}

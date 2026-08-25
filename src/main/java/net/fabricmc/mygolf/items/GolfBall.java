package net.fabricmc.mygolf.items;

import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.items.base.BaseItem;
import net.fabricmc.mygolf.physics.GolfPhysicsEngine;
import net.fabricmc.mygolf.registry.RegisterEntities;
import net.fabricmc.mygolf.registry.RegisterItems;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

///item:高尔夫球
public class GolfBall extends BaseItem implements DyeableItem {

    static int maxCount = 64;    //最大堆叠数量
    private static final int PURE_WHITE = 0xFFFFFF;
    private static final int VANILLA_WHITE = 0xF9FFFE;   // DyeColor.WHITE
    private final GolfPhysicsEngine.Config physicsConfig = GolfPhysicsEngine.Config.STANDARD_BALL;


    public GolfBall(Settings settings) {
        super(settings);
    }

    //初始化方法
    public static GolfBall defaultInstance() {
        return new GolfBall(defaultSetting());
    }

    //默认设置
    private static Settings defaultSetting() {
        return new Settings().maxCount(maxCount);
    }

    ///添加物品提示
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if(stack.hasNbt() && stack.getNbt().contains("HitCount")) {
            int hits = stack.getNbt().getInt("HitCount");
            if (hits > 0) {
                if (stack.getNbt().contains("IsGoaled") && stack.getNbt().getBoolean("IsGoaled")) {
                    tooltip.add(Text.literal("Hits: " + hits).formatted(Formatting.GOLD));
                }else {
                    tooltip.add(Text.literal("Hits: " + hits).formatted(Formatting.GRAY));
                }
            }
        }

        super.appendTooltip(stack, world, tooltip, context);
    }

    //Right click to spawn golf ball
    @Override
    public TypedActionResult<ItemStack> use(World level, PlayerEntity user, Hand hand) {
        final var itemStack = user.getStackInHand(hand);
        final var hitResult = raycast(level, user, RaycastContext.FluidHandling.NONE);

        if (!level.isClient()) {
            final var golfBallEntity = new GolfBallEntity(RegisterEntities.GOLF_BALL, level);
            final double radius = physicsConfig.radius();

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                if (user.isInSneakingPose()){
                    final Direction side = hitResult.getSide();
                    final Vec3d hitPos = hitResult.getPos();
                    final BlockPos blockPos = hitResult.getBlockPos();

                    double spawnX = hitPos.x + (side.getOffsetX() * (radius + 0.01));
                    double spawnY = hitPos.y;
                    double spawnZ = hitPos.z + (side.getOffsetZ() * (radius + 0.01));

                    if (side == Direction.UP) {
                        spawnY = hitPos.y;
                    } else if (side == Direction.DOWN) {
                        spawnY = hitPos.y - golfBallEntity.getHeight() - 0.01;
                    }

                    // Set initial trial position
                    golfBallEntity.updatePosition(spawnX, spawnY, spawnZ);

                    // Resolve wall clipping against hollow/complex block geometry (Composters, Cauldrons, Hoppers)
                    VoxelShape blockShape = level.getBlockState(blockPos).getCollisionShape(level, blockPos);
                    if (!blockShape.isEmpty()) {
                        Box ballBox = golfBallEntity.getBoundingBox();

                        for (Box wallBox : blockShape.getBoundingBoxes()) {
                            Box worldWallBox = wallBox.offset(blockPos);

                            // Check if ball intersects interior wall box
                            if (ballBox.intersects(worldWallBox)) {
                                // Nudge X/Z outward away from nearest wall edge
                                double minXDist = Math.abs((spawnX - radius) - worldWallBox.maxX);
                                double maxXDist = Math.abs((spawnX + radius) - worldWallBox.minX);
                                double minZDist = Math.abs((spawnZ - radius) - worldWallBox.maxZ);
                                double maxZDist = Math.abs((spawnZ + radius) - worldWallBox.minZ);

                                double minDist = Math.min(Math.min(minXDist, maxXDist), Math.min(minZDist, maxZDist));

                                if (minDist == minXDist) spawnX = worldWallBox.maxX + radius + 0.001;
                                else if (minDist == maxXDist) spawnX = worldWallBox.minX - radius - 0.001;
                                else if (minDist == minZDist) spawnZ = worldWallBox.maxZ + radius + 0.001;
                                else if (minDist == maxZDist) spawnZ = worldWallBox.minZ - radius - 0.001;
                            }
                        }
                    }

                    // Apply clean position and zero out any residual velocity
                    golfBallEntity.updatePosition(spawnX, spawnY, spawnZ);
                    golfBallEntity.setVelocity(Vec3d.ZERO);
                    golfBallEntity.setOnGround(side == Direction.UP);

                }else {
                    final var unit = hitResult.getPos().subtract(user.getPos()).normalize();
                    golfBallEntity.updatePosition(user.getPos().x + unit.x, user.getPos().y + user.getStandingEyeHeight(), user.getPos().z + unit.z);

                }
            }else {
                golfBallEntity.updatePosition(hitResult.getPos().x, hitResult.getPos().y, hitResult.getPos().z);
            }

            // Apply the dyed item color to the spawned entity
            if (RegisterItems.GOLF_BALL.hasColor(itemStack)) {
                int itemColor = RegisterItems.GOLF_BALL.getColor(itemStack);

                // Treat white as undyed (-1)
                if (itemColor == 0xFFFFFF || itemColor == 0xF9FFFE) {
                    golfBallEntity.setColor(-1);
                } else {
                    golfBallEntity.setColor(itemColor);
                }
            } else {
                golfBallEntity.setColor(-1); // Default undyed state
            }

            // Pass Custom Name
            if (itemStack.hasCustomName()) {
                golfBallEntity.setCustomName(itemStack.getName());
            }

            // Pass IsGoaled and HitCount
            if (itemStack.hasNbt()) {
                if (itemStack.getNbt().contains("IsGoaled")) {
                    golfBallEntity.setGoaled(itemStack.getNbt().getBoolean("IsGoaled"));
                }
                if (itemStack.getNbt().contains("HitCount")) {
                    golfBallEntity.setHitCount(itemStack.getNbt().getInt("HitCount"));
                }
            }

            // Spawn ball, Item → Entity
            level.spawnEntity(golfBallEntity);

            if (!user.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
        }
        return TypedActionResult.success(itemStack);
    }

    // Default color if undyed (White)
    @Override
    public int getColor(ItemStack stack) {
        NbtCompound nbt = stack.getSubNbt(DISPLAY_KEY);
        if (nbt != null && nbt.contains(COLOR_KEY, 99)) {
            return nbt.getInt(COLOR_KEY);
        }
        return 0xFFFFFF; // Default White
    }
    public static boolean isGoaled(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasNbt() && stack.getNbt().getBoolean("IsGoaled");
    }

    @Override
    public void setColor(ItemStack stack, int color) {
        if (color == PURE_WHITE || color == VANILLA_WHITE) {
            this.removeColor(stack); // Strips 'display.color' NBT entirely
        } else {
            DyeableItem.super.setColor(stack, color);
        }
    }

    public static void sanitizeNbt(ItemStack stack) {
        if (!stack.hasNbt()) return;

        NbtCompound nbt = stack.getNbt();

        // 1. If no custom name exists, strip Anvil RepairCost and empty display tags
        if (!stack.hasCustomName()) {
            nbt.remove("RepairCost");

            if (nbt.contains("display")) {
                NbtCompound display = nbt.getCompound("display");
                display.remove("Name");
                if (display.isEmpty()) {
                    nbt.remove("display");
                }
            }
        }

        // 2. Strip default/zero values if present
        if (nbt.contains("HitCount") && nbt.getInt("HitCount") <= 0) {
            nbt.remove("HitCount");
        }
        if (nbt.contains("IsGoaled") && !nbt.getBoolean("IsGoaled")) {
            nbt.remove("IsGoaled");
        }

        // 3. Fully nullify NBT if empty so stack.hasNbt() == false
        if (nbt.isEmpty()) {
            stack.setNbt(null);
        }
    }
}

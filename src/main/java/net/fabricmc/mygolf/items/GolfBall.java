package net.fabricmc.mygolf.items;

import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.items.base.BaseItem;
import net.fabricmc.mygolf.registry.RegisterEntities;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

///item:高尔夫球
public class GolfBall extends BaseItem implements DyeableItem {

    static int maxCount = 64;    //最大堆叠数量

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
        super.appendTooltip(stack, world, tooltip, context);
        // 默认为白色文本
        tooltip.add(Text.translatable("空手右键可回收").formatted(Formatting.GRAY));
    }

    //右键生成golf ball测试
    @Override
    public TypedActionResult<ItemStack> use(World level, PlayerEntity user, Hand hand) {
        final var itemStack = user.getStackInHand(hand);
        final var hitResult = raycast(level, user, RaycastContext.FluidHandling.NONE);

        if (!level.isClient()) {
            final var golfBallEntity = new GolfBallEntity(RegisterEntities.GOLF_BALL, level);

            if (user.isInSneakingPose()) {
                final var unit = hitResult.getPos().subtract(user.getPos()).normalize();
                golfBallEntity.updatePosition(user.getPos().x + unit.x, user.getPos().y + user.getStandingEyeHeight(), user.getPos().z + unit.z);
            }else if (hitResult.getType() == HitResult.Type.BLOCK) {
                final Direction side = hitResult.getSide();
                final Vec3d hitPos = hitResult.getPos();

                // Fetch entity bounding box dimensions
                final double width = golfBallEntity.getWidth();
                final double height = golfBallEntity.getHeight();
                final double halfWidth = width / 2.0;

                // Offset horizontally away from side faces
                double spawnX = hitPos.x + (side.getOffsetX() * (halfWidth + 0.01));
                double spawnY = hitPos.y;
                double spawnZ = hitPos.z + (side.getOffsetZ() * (halfWidth + 0.01));

                // Offset vertically for top (UP) and bottom (DOWN) faces
                if (side == Direction.UP) {
                    spawnY = hitPos.y + 0.01; // Rest on top face
                } else if (side == Direction.DOWN) {
                    spawnY = hitPos.y - height - 0.01; // Hang below bottom face
                }

                golfBallEntity.updatePosition(spawnX, spawnY, spawnZ);
            }else {
                golfBallEntity.updatePosition(hitResult.getPos().x, hitResult.getPos().y, hitResult.getPos().z);
            }
            // Apply the dyed item color to the spawned entity
            golfBallEntity.setColor(this.getColor(itemStack));

            // Spawn ball
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
}

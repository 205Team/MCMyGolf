package net.fabricmc.mygolf.items;

import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.items.base.BaseItem;
import net.fabricmc.mygolf.registry.RegisterEntities;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

///item:高尔夫球
public class GolfBall extends BaseItem {

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
            } else {
                golfBallEntity.updatePosition(hitResult.getPos().x, hitResult.getPos().y, hitResult.getPos().z);
            }
            //生成高尔夫球实体
            level.spawnEntity(golfBallEntity);

            itemStack.decrement(1);//数量-1
        }
        return TypedActionResult.success(itemStack);
    }
}

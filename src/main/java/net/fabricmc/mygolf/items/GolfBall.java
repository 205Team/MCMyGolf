package net.fabricmc.mygolf.items;

import com.jme3.math.Vector3f;
import dev.lazurite.rayon.impl.bullet.math.Convert;
import net.fabricmc.mygolf.RegisterEntities;
import net.fabricmc.mygolf.RegisterItems;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.items.base.BaseItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
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
        return new Settings().maxCount(maxCount).group(ItemGroup.MISC);
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
            final var rigidBody = golfBallEntity.getRigidBody();

            if (user.isInSneakingPose()) {
                final var unit = hitResult.getPos().subtract(user.getPos()).normalize();
                golfBallEntity.updatePosition(user.getPos().x + unit.x, user.getPos().y + user.getStandingEyeHeight(), user.getPos().z + unit.z);
                rigidBody.setLinearVelocity(Convert.toBullet(unit).multLocal(20));
                rigidBody.setAngularVelocity(new Vector3f(0, 5, 0));
            } else {
                golfBallEntity.updatePosition(hitResult.getPos().x, hitResult.getPos().y, hitResult.getPos().z);
            }
            //生成高尔夫球实体
            level.spawnEntity(golfBallEntity);
            user.giveItemStack(new ItemStack(RegisterItems.GOLF_BALL));
            return TypedActionResult.success(itemStack);
        }

        return TypedActionResult.pass(itemStack);
    }
}

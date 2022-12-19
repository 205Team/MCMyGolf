package net.fabricmc.mygolf.items;

import com.jme3.math.Vector3f;
import dev.lazurite.rayon.impl.bullet.math.Convert;
import dev.lazurite.rayon.impl.dev.RayonDev;
import dev.lazurite.rayon.impl.dev.entity.StoneBlockEntity;
import net.fabricmc.mygolf.RegisterEntities;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.items.base.BaseItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.Random;

//item：铁棍
    public class IronStick extends BaseItem {

    static int maxCount = 64;    //最大堆叠数量

    public IronStick(Settings settings) {
        super(settings);
    }

    //初始化方法
    public static IronStick defaultInstance() {
        return new IronStick(defaultSetting());
    }

    //默认设置
    private static Settings defaultSetting() {
        return new Settings().maxCount(maxCount).group(ItemGroup.MISC);
    }

    //右键生成golf ball测试
    @Override
    public TypedActionResult<ItemStack> use(World level, PlayerEntity user, Hand hand) {
        final var itemStack = user.getStackInHand(hand);
        final var hitResult = raycast(level, user, RaycastContext.FluidHandling.NONE);

        if (!level.isClient()) {
            final var entity = new GolfBallEntity(RegisterEntities.GOLF_BALL, level);
            final var rigidBody = entity.getRigidBody();

            if (user.isInSneakingPose()) {
                final var random = new Random();
                final var unit = hitResult.getPos().subtract(user.getPos()).normalize();
                entity.updatePosition(user.getPos().x + unit.x, user.getPos().y + user.getStandingEyeHeight(), user.getPos().z + unit.z);
                rigidBody.setLinearVelocity(Convert.toBullet(unit).multLocal(20));
                rigidBody.setAngularVelocity(new Vector3f(0, 5, 0));
//                rigidBody.setAngularVelocity(new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat()));
            } else {
                entity.updatePosition(hitResult.getPos().x, hitResult.getPos().y, hitResult.getPos().z);
            }

            level.spawnEntity(entity);
            return TypedActionResult.success(itemStack);
        }

        return TypedActionResult.pass(itemStack);
    }
}


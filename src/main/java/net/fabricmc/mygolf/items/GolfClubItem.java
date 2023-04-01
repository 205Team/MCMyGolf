package net.fabricmc.mygolf.items;

import com.jme3.math.Vector3f;
import dev.lazurite.rayon.impl.bullet.thread.PhysicsThread;
import net.fabricmc.mygolf.RegisterSounds;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.items.base.ItemAbstract;
import net.fabricmc.mygolf.tools.StringTool;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;
//item：高尔夫球杆

public class GolfClubItem extends Item implements ItemAbstract {

    // 定义蓄力的最长时间，单位为 tick
    private static final int MAX_USE_TIME = 40;
    // 定义最大初速度
    private static final float MAX_BALL_SPEED = 40;

    public GolfClubItem(Item.Settings settings) {
        super(settings);
    }

    //    //初始化方法
    public static GolfClubItem defaultInstance() {
        return new GolfClubItem(new Settings().maxCount(1));
    }

    // 重写 onStoppedUsing 方法，在松开右键时触发生成高尔夫球实体
    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (user instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) user;
            Vec3d lookVec = player.getRotationVector();
            Vec3d posVec = player.getPos().add(lookVec.multiply(1.0));
            // 获取所有附近的高尔夫球实体
            List<GolfBallEntity> balls = world.getEntitiesByClass(GolfBallEntity.class, new Box(posVec.add(-5.0, -5.0, -5.0), posVec.add(5.0, 5.0, 5.0)), entity -> entity.distanceTo(player) < 2.0);
            if (!balls.isEmpty()) {
                // 选择距离玩家最近的高尔夫球实体
                GolfBallEntity ball = balls.stream()
                        .min(Comparator.comparingDouble(entity -> entity.distanceTo(player)))
                        .orElse(null);
                if (ball != null) {
                    //计算力量
                    float power = getPower(remainingUseTicks); // 根据使用时间计算蓄力比例
                    float hitPower = power * MAX_BALL_SPEED;

                    //击球
                    Vec3d ballPos = ball.getPos();
                    Vec3d playerPos = player.getPos();
                    Vec3d posDelta3d = ballPos.subtract(playerPos);
                    Vec2f posDelta2f = new Vec2f((float) posDelta3d.x, (float) posDelta3d.z); //水平方向向量差
                    float hitDistance = posDelta2f.length(); //向量模
                    Vec2f hitDirection = posDelta2f.normalize(); //单位向量
                    float hitPitch = (30 - hitDistance) / 5;
                    Vector3f impulse = new Vector3f(hitDirection.x * hitPower, (float) (hitPitch * (0.5 + power)), hitDirection.y * hitPower);
                    //rayon:给球一个冲量
                    PhysicsThread.get(world.getServer()).execute(() -> {
                        ball.getRigidBody().applyCentralImpulse(impulse);
                    });
                    ball.hitByPlayer(player);

                    playHitSound(world, player, (float) (0.5 + power * 0.5));
                    player.sendMessage(Text.of(String.format("%.1f%%", power * 100)));
                }
            } else {
                playSwingSound(world, player);
            }
        }
    }


    // 重写 getMaxUseTime 方法，返回蓄力最长时间
    @Override
    public int getMaxUseTime(ItemStack stack) {
        return MAX_USE_TIME;
    }

    // 重写 getUseAction 方法，返回 RIGHT_CLICK 类型，表示右键使用
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    //    //默认设置
    private static Settings defaultSetting() {
        return new Settings();
    }

    // 重写 getUseTime 方法，返回 MAX_USE_TIME，表示使用时间等同于蓄力最长时间
    public static int getMaxUseTime() {
        return MAX_USE_TIME;
    }

    // 重写 use 方法，在蓄力时触发动画播放和数据变化
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.isSneaking()) { // 如果玩家正在 Sneak，不进行蓄力
            return TypedActionResult.pass(stack);
        } else {
            user.setCurrentHand(hand);
            return TypedActionResult.consume(stack);
        }
    }

    // 计算蓄力比例，根据使用时间计算
    private float getPower(int remainingUseTicks) {
        float power = 1.0f - (MAX_USE_TIME - remainingUseTicks) / (float) MAX_USE_TIME;
        power = MathHelper.clamp(power, 0.0f, 1.0f);
        return power;
    }

    // 播放击球声音
    private void playHitSound(World world, PlayerEntity player, float volume) {
        if (!world.isClient) {
            world.playSound(null, player.getBlockPos(), RegisterSounds.GOLF_BALL_HIT_SOUND_EVENT, SoundCategory.BLOCKS, 1f, 1f);
        }
    }

    // 播放挥空声音
    private void playSwingSound(World world, PlayerEntity player) {
        if (!world.isClient) {
            world.playSound(null, player.getBlockPos(), RegisterSounds.GOLF_CLUB_SWING_SOUND_EVENT, SoundCategory.BLOCKS, 1f, 1f);
        }
    }

    @Override
    public String codeName() {
        return StringTool.getIdFrom(getClass().getSimpleName());
    }
}



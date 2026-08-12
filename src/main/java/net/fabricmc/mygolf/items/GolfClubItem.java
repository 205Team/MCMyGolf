package net.fabricmc.mygolf.items;

import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.items.base.ItemAbstract;
import net.fabricmc.mygolf.registry.RegisterSounds;
import net.fabricmc.mygolf.tools.DebugUtil;
import net.fabricmc.mygolf.tools.StringTool;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;
//item：高尔夫球杆

public class GolfClubItem extends Item implements ItemAbstract {

    public static final float[] LOFT_PRESETS = { -5.0f, -15.0f, -30.0f, -45.0f, -60.0f, -75.0f };   // Preset loft angles (Negative pitch = UP in Minecraft)
    private static final int MAX_USE_TIME = 72000; // 定义蓄力的最长时间，单位为 tick
    public static final int MAX_CHARGE_TICKS = 30; // 1.5 seconds to reach 100% power
    public static final double MAX_SHOT_POWER = 0.6; // Max speed multiplier

    public GolfClubItem(Item.Settings settings) {
        super(settings);
    }

    //初始化方法
    public static GolfClubItem defaultInstance() {
        return new GolfClubItem(new Settings().maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    // 重写 onStoppedUsing 方法，在蓄力时触发动画播放和数据变化，在松开右键时触发击球
    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player)) return;

        // [----------]
        int heldTicks = this.getMaxUseTime(stack) - remainingUseTicks;

        // Mouse tap
        // [>---------]
        if (heldTicks < 3) {
            cycleLoftAngle(stack, player, world);
            return;
        }

        if (!world.isClient()) {
            // Mouse Hold
            float powerRatio = Math.min(1.0f, (float) heldTicks / MAX_CHARGE_TICKS);

            // Minimum shot threshold (prevents accidental misfires)
            // [->--------]
            if (powerRatio < 0.1f) return;

            // Find closest GolfBallEntity within 4 blocks of the player
            GolfBallEntity targetBall = GolfBallEntity.getClosestBall(world, player, GolfBallEntity.MIN_RADIUS);

            if (targetBall != null) {
                // Read current active loft angle from item NBT
                float currentLoft = getSelectedLoft(stack);

                // Calculate direction vector using player's horizontal yaw and club loft
                Vec3d launchDir = Vec3d.fromPolar(currentLoft, player.getYaw());
                // [----->----]
                double finalSpeed = powerRatio * MAX_SHOT_POWER;

                // Apply velocity to the ball
                targetBall.applyImpulse(launchDir.multiply(finalSpeed));
                targetBall.incrementHitCount();

                // Apply Magnus effect
                if (GolfBallEntity.ENABLE_MAGNUS_EFFECT) {
                    float yawRad = (float) Math.toRadians(player.getYaw());

                    // 1. Backspin scales with shot speed and loft angle steepness
                    double loftMagnitude = Math.abs(currentLoft); // e.g. 5.0 to 75.0
                    double backspinIntensity = finalSpeed * Math.sin(Math.toRadians(loftMagnitude)) * 0.15;

                    // 2. Optional Sidespin (positive = slice/right, negative = hook/left)
                    // Set to 0.0f or read from player stance / custom curve NBT
                    double sidespinIntensity = 0.0;

                    // 3. Transform local shot spin to world-space spin vector
                    // Perpendicular axis relative to player facing direction:
                    double backspinX = -Math.cos(yawRad) * backspinIntensity;
                    double backspinZ = -Math.sin(yawRad) * backspinIntensity;
                    double sidespinY = sidespinIntensity;

                    targetBall.spinVector = new Vec3d(backspinX, sidespinY, backspinZ);
                } else {
                    targetBall.spinVector = Vec3d.ZERO;
                }

                // Play swing audio
                playHitSound(world, player);

            }else {
                playSwingSound(world, player);

            }
        }
    }

    public static float getSelectedLoft(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        int index = (nbt != null) ? nbt.getInt("LoftIndex") : 0;
        return LOFT_PRESETS[Math.abs(index) % LOFT_PRESETS.length];
    }

    private void cycleLoftAngle(ItemStack stack, PlayerEntity player, World world) {
        if (!world.isClient()) {
            NbtCompound nbt = stack.getOrCreateNbt();
            int currentIndex = nbt.getInt("LoftIndex");

            // Cycle to next preset index in rotation
            int nextIndex = (currentIndex + 1) % LOFT_PRESETS.length;
            nbt.putInt("LoftIndex", nextIndex);

            // Play gear click sound and send action bar message
            world.playSound(null, player.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundCategory.PLAYERS, 0.4f, 1.2f);

            player.sendMessage(Text.literal("Loft set to: " + LOFT_PRESETS[nextIndex])
                    .formatted(Formatting.GREEN), true); // true = displays above action bar
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
        return UseAction.BOW;
    }

    //默认设置
    private static Settings defaultSetting() {
        return new Settings();
    }

    // 播放击球声音
    private void playHitSound(World world, PlayerEntity player) {
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



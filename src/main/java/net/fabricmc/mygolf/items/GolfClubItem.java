package net.fabricmc.mygolf.items;

import net.fabricmc.mygolf.MyGolfModClient;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.items.base.ItemAbstract;
import net.fabricmc.mygolf.registry.RegisterSounds;
import net.fabricmc.mygolf.tools.DebugUtil;
import net.fabricmc.mygolf.tools.StringTool;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

//item：高尔夫球杆

public class GolfClubItem extends Item implements ItemAbstract {

    public static final float[] LOFT_PRESETS = { 0.0f, -15.0f, -40.0f, -60.0f, -75.0f };   // Preset loft angles (Negative pitch = UP in Minecraft)
    private static final int MAX_USE_TIME = 72000; // 定义蓄力的最长时间，单位为 tick
    public static final int MIN_CHARGE_TICKS = 5;  // * 0.05 seconds before a shot fires
    public static final int MAX_CHARGE_TICKS = 20; // * 0.05 seconds to reach 100% power
    public static final int MAX_LOOPS = 3; // Maximum allowed loops
    public static final double MAX_SHOT_POWER = 3.0; // Max speed multiplier
    public static final int COOLDOWN_TICKS = 15; // * 0.05 second cooldown

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

        // Sneak + Right Click -> Toggle Loft immediately (No charging)
        if (user.isSneaking()) {
            cycleLoftAngle(stack, user, world);
            return TypedActionResult.success(stack, world.isClient());
        }

        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        // Normal Right Click -> Start charging shot
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    // Charge and swing
    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player)) return;

        // Minimum shot threshold (prevents accidental misfires)
        int heldTicks = this.getMaxUseTime(stack) - remainingUseTicks;
        if (heldTicks < MIN_CHARGE_TICKS) return;
        float currentLoft = getSelectedLoft(stack);

        if (!world.isClient()) {
            int chargeTicks = heldTicks - MIN_CHARGE_TICKS;
            int maxAllowedTicks = MAX_CHARGE_TICKS * MAX_LOOPS;

            float powerRatio;
            if (chargeTicks >= maxAllowedTicks) {
                // Capped: Exceeded 3 full loops -> force random shot power
                powerRatio = 0.01f + world.getRandom().nextFloat() * 0.99f;
            } else {
                powerRatio = calculatePowerRatio(chargeTicks, currentLoft);
            }

            // Find closest GolfBallEntity within 4 blocks of the player
            GolfBallEntity targetBall = GolfBallEntity.getClosestBall(world, player, GolfBallEntity.MIN_RADIUS);

            if (targetBall != null) {

                // Calculate direction vector using player's horizontal yaw and club loft
                Vec3d launchDir = Vec3d.fromPolar(currentLoft, player.getYaw());
                double finalSpeed = powerRatio * MAX_SHOT_POWER;

                // Apply velocity to the ball
                targetBall.applyImpulse(launchDir.multiply(finalSpeed));
                targetBall.incrementHitCount();

                // Apply Magnus effect
                if (GolfBallEntity.ENABLE_MAGNUS_EFFECT) {
                    float yawRad = (float) Math.toRadians(player.getYaw());

                    // Backspin scales with shot speed and loft angle steepness
                    double loftMagnitude = Math.abs(currentLoft); // e.g. 5.0 to 75.0
                    double backspinIntensity = finalSpeed * Math.sin(Math.toRadians(loftMagnitude)) * 1.5;

                    // Transform local shot spin to world-space spin vector
                    // Perpendicular axis relative to player facing direction:
                    double backspinX = -Math.cos(yawRad) * backspinIntensity;
                    double backspinZ = -Math.sin(yawRad) * backspinIntensity;

                    targetBall.setSpin(new Vec3d(backspinX, 0.0, backspinZ));
                } else {
                    targetBall.setSpin(Vec3d.ZERO);
                }
                playHitSound(world, player);

            }else {
                playSwingSound(world, player);

            }

            player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        }
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int count) {
        int heldTicks = this.getMaxUseTime(stack) - count;
        int maxAllowedTicks = MIN_CHARGE_TICKS + (MAX_CHARGE_TICKS * MAX_LOOPS);

        // Automatically trigger shot when 3 full loops complete
        if (heldTicks >= maxAllowedTicks) {
            user.stopUsingItem();
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

    public static float calculatePowerRatio(float chargeTicks, float loftDegrees) {
        float linearProgress = (chargeTicks % MAX_CHARGE_TICKS) / (float) MAX_CHARGE_TICKS;

        // Normalize loft (0° to 90°) to a 0.0f -> 1.0f range
        float normalizedLoft = MathHelper.clamp(-loftDegrees / 90.0f, 0.0f, 1.0f);

        // Dynamic Exponent Interpolation:
        // Low Loft  (0°)  -> Exponent 2.2f (Starts slow, skyrockets near max power)
        // High Loft (90°) -> Exponent 0.45f (Skyrockets fast early, flattens/slows near max power)
        float lowLoftExponent = 2.2f;
        float highLoftExponent = 0.45f;
        float exponent = lowLoftExponent + (highLoftExponent - lowLoftExponent) * normalizedLoft;

        return (float) Math.pow(linearProgress, exponent);
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
    @Override
    public String codeName() {
        return StringTool.getIdFrom(getClass().getSimpleName());
    }
}



package net.fabricmc.mygolf.items;

import net.fabricmc.mygolf.items.base.BaseItem;
import net.fabricmc.mygolf.registry.RegisterBlocks;
import net.fabricmc.mygolf.tools.StringTool;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.BlockSoundGroup; // Correct import
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class FlagstickItem extends BaseItem {

    static int maxCount = 64;    //最大堆叠数量private static final String DIG_KEY = "DigProgress";
    private static final int MAX_DIG_TICKS = 15;
    private static final String DIG_KEY = "DigProgress";
    private static final String LAST_TICK_KEY = "LastDigTick";
    private static final String TARGET_POS_KEY = "TargetBlockPos"; // Track targeted block

    public FlagstickItem(Settings settings) {
        super(settings);
    }

    //初始化方法
    public static FlagstickItem defaultInstance() {
        return new FlagstickItem(defaultSetting());
    }

    //默认设置
    private static Settings defaultSetting() {
        return new Settings().maxCount(maxCount);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos targetPos = context.getBlockPos();
        BlockState state = world.getBlockState(targetPos);

        if (!state.isIn(BlockTags.DIRT)) return ActionResult.PASS;

        PlayerEntity player = context.getPlayer();
        if (player == null) return ActionResult.PASS;

        // 1. Trigger hand swing animation (renders cleanly because isUsingItem is false)
        player.swingHand(context.getHand());

        if (world.isClient) {
            Vec3d hitPos = context.getHitPos();
            for (int i = 0; i < 3; i++) {
                world.addParticle(
                        new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                        hitPos.x, hitPos.y, hitPos.z,
                        (world.random.nextDouble() - 0.5) * 0.1,
                        world.random.nextDouble() * 0.1,
                        (world.random.nextDouble() - 0.5) * 0.1
                );
            }
            return ActionResult.SUCCESS;
        }

        world.playSound(
                null, targetPos,
                SoundEvents.ITEM_SHOVEL_FLATTEN,
                SoundCategory.BLOCKS,
                0.6F,
                1.0F
        );

        // 3. Reset progress if last right-click was more than 6 ticks ago (released right click)
        ItemStack stack = context.getStack();
        long currentTime = world.getTime();
        long currentPosLong = targetPos.asLong();

        NbtCompound nbt = stack.getOrCreateNbt();
        long lastTick = nbt.getLong(LAST_TICK_KEY);
        long lastPosLong = nbt.getLong(TARGET_POS_KEY);
        int progress = nbt.getInt(DIG_KEY);

        if (currentPosLong != lastPosLong ||currentTime - lastTick > 6) {
            progress = 0;
        }

        // Minecraft auto-repeats right-clicks every 4 ticks while holding
        progress += 4;
        nbt.putLong(LAST_TICK_KEY, currentTime);
        nbt.putLong(TARGET_POS_KEY, currentPosLong);

        if (progress >= MAX_DIG_TICKS) {
            // Placement logic
            BlockPos placePos = targetPos.up();
            ItemPlacementContext placementContext = new ItemPlacementContext(context);
            BlockState flagState = RegisterBlocks.FLAGSTICK_BLOCK.getPlacementState(placementContext);

            if (flagState != null && world.getBlockState(placePos).canReplace(placementContext) && flagState.canPlaceAt(world, placePos)) {
                world.setBlockState(placePos, flagState);
                world.setBlockState(targetPos, RegisterBlocks.GOLF_HOLE.getDefaultState());

                BlockSoundGroup sound = flagState.getSoundGroup();
                world.playSound(null,
                        placePos,
                        sound.getPlaceSound(),
                        SoundCategory.BLOCKS,
                        (sound.getVolume() + 1.0F) / 2.0F,
                        sound.getPitch() * 0.8F
                );

                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
            }
            clearDigNbt(stack);
        } else {
            nbt.putInt(DIG_KEY, progress);
        }

        return ActionResult.CONSUME;
    }

    // Reset charge progress if player stops right-clicking / switches items
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && stack.hasNbt() && stack.getNbt().contains(LAST_TICK_KEY)) {
            // Immediately wipe NBT if item is unselected OR right-click was released
            if (!selected || (world.getTime() - stack.getNbt().getLong(LAST_TICK_KEY) > 6)) {
                clearDigNbt(stack);
            }
        }
    }

    private static void clearDigNbt(ItemStack stack) {
        if (stack.hasNbt()) {
            NbtCompound nbt = stack.getNbt();
            nbt.remove(DIG_KEY);
            nbt.remove(LAST_TICK_KEY);
            nbt.remove(TARGET_POS_KEY);
            if (nbt.isEmpty()) {
                stack.setNbt(null); // Fully removes NBT compound to preserve stackability
            }
        }
    }

    @Override
    public String codeName() {
        return StringTool.getIdFrom(getClass().getSimpleName());
    }
}
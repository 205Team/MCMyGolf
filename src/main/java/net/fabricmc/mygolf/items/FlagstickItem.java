package net.fabricmc.mygolf.items;

import net.fabricmc.mygolf.items.base.BaseItem;
import net.fabricmc.mygolf.registry.RegisterBlocks;
import net.fabricmc.mygolf.tools.StringTool;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
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

    static int maxCount = 64;    //最大堆叠数量
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

    // 1. Trigger charging when right-clicking on a Dirt-tagged block
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);

        // Checks for dirt, grass blocks, coarse dirt, podzol, etc.
        if (state.isIn(BlockTags.DIRT)) {
            PlayerEntity player = context.getPlayer();
            if (player != null) {
                player.setCurrentHand(context.getHand());
                return ActionResult.CONSUME;
            }
        }
        return ActionResult.PASS;
    }

    // 2. Animation during long-press
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPEAR; // Gives a downwards thrusting/digging stance
    }

    // 3. Duration of the long press (30 ticks = 1.5 seconds)
    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 15;
    }

    // 4. Effects played every tick while holding down use
    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (user instanceof PlayerEntity player) {
            HitResult hit = player.raycast(5.0D, 0.0F, false);
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos pos = blockHit.getBlockPos();
                BlockState state = world.getBlockState(pos);

                if (state.isIn(BlockTags.DIRT)) {
                    // Play digging sound every 5 ticks
                    if (remainingUseTicks % 5 == 0) {
                        world.playSound(
                                null,
                                pos,
                                SoundEvents.ITEM_SHOVEL_FLATTEN,
                                SoundCategory.BLOCKS,
                                0.6F,
                                1.0F
                        );
                    }

                    // Spawn digging dust particles on client
                    if (world.isClient) {
                        Vec3d hitPos = blockHit.getPos();
                        for (int i = 0; i < 2; i++) {
                            world.addParticle(
                                    new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                                    hitPos.x, hitPos.y, hitPos.z,
                                    (world.random.nextDouble() - 0.5) * 0.1,
                                    world.random.nextDouble() * 0.1,
                                    (world.random.nextDouble() - 0.5) * 0.1
                            );
                        }
                    }
                }
            }
        }
    }

    // 5. Called automatically when the player finishes holding for getMaxUseTime duration
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient && user instanceof PlayerEntity player) {
            HitResult hit = player.raycast(5.0D, 0.0F, false);
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos targetPos = blockHit.getBlockPos();
                BlockState targetState = world.getBlockState(targetPos);

                if (targetState.isIn(BlockTags.DIRT)) {
                    BlockPos placePos = targetPos.up();
                    BlockHitResult topHit = new BlockHitResult(
                            Vec3d.ofCenter(targetPos).add(0, 0.5, 0),
                            net.minecraft.util.math.Direction.UP,
                            targetPos,
                            false
                    );
                    ItemPlacementContext placementContext = new ItemPlacementContext(
                            world, player, user.getActiveHand(), stack, topHit
                    );

                    // Replace with your actual registered Flagstick Block reference
                    BlockState flagstickState = RegisterBlocks.FLAGSTICK_BLOCK.getPlacementState(placementContext);

                    if (flagstickState != null && world.getBlockState(placePos).canReplace(placementContext) && flagstickState.canPlaceAt(world, placePos)) {
                        // Place block in world
                        world.setBlockState(placePos, flagstickState);
                        world.setBlockState(placePos.down(), RegisterBlocks.GOLF_HOLE.getDefaultState());

                        // Play placement sound
                        BlockSoundGroup soundGroup = flagstickState.getSoundGroup();
                        world.playSound(
                                null,
                                placePos,
                                soundGroup.getPlaceSound(),
                                SoundCategory.BLOCKS,
                                (soundGroup.getVolume() + 1.0F) / 2.0F,
                                soundGroup.getPitch() * 0.8F
                        );

                        // Consume 1 item if not in creative
                        if (!player.getAbilities().creativeMode) {
                            stack.decrement(1);
                        }
                    }
                }
            }
        }
        return stack;
    }

    @Override
    public String codeName() {
        return StringTool.getIdFrom(getClass().getSimpleName());
    }
}
package net.fabricmc.mygolf.blockEntity;

import net.fabricmc.mygolf.registry.RegisterBlockEntities;
import net.fabricmc.mygolf.blockEntity.base.BaseBlockEntity;
import net.fabricmc.mygolf.blockEntity.base.ImplementedInventory;
import net.fabricmc.mygolf.blockEntity.render.GolfHoleScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

public class GolfHoleEntity extends BaseBlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(3, ItemStack.EMPTY);
    private int lastHitCount = 0;

    public GolfHoleEntity(BlockPos pos, BlockState state) {
        super(RegisterBlockEntities.GOLF_HOLE_ENTITY, pos, state);
    }

    public int getLastHitCount() {
        return this.lastHitCount;
    }

    public void setLastHitCount(int hits) {
        // Clamp between 1 and 15 for valid Redstone signal range
        this.lastHitCount = MathHelper.clamp(hits, 1, 15);
        this.markDirty();

        if (this.world != null) {
            this.world.updateComparators(this.pos, this.getCachedState().getBlock());
        }
    }

    public boolean insertStack(ItemStack stackToInsert) {
        for (int i = 0; i < this.size(); i++) {
            ItemStack slotStack = this.getStack(i);

            // Can we place the stack into this slot?
            if (this.isValid(i, stackToInsert)) {
                if (slotStack.isEmpty()) {
                    this.setStack(i, stackToInsert.copy());
                    stackToInsert.setCount(0);
                    this.markDirty();
                    return true;
                } else if (ItemStack.canCombine(slotStack, stackToInsert)) {
                    int maxInsert = Math.min(this.getMaxCountPerStack(), slotStack.getMaxCount()) - slotStack.getCount();
                    if (maxInsert >= stackToInsert.getCount()) {
                        slotStack.increment(stackToInsert.getCount());
                        stackToInsert.setCount(0);
                        this.markDirty();
                        return true;
                    }
                }
            }
        }
        return false; // Storage full or incompatible
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return items;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new GolfHoleScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        Inventories.writeNbt(nbt, items);
        super.writeNbt(nbt);
    }
}

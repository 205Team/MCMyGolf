package net.fabricmc.mygolf.blockEntity.render;

import net.fabricmc.mygolf.MyGolfModClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;


public class GolfHoleScreenHandler extends ScreenHandler {

    private final Inventory inventory;

    public GolfHoleScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(3));
    }

    public GolfHoleScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(MyGolfModClient.GOLF_HOLE_SCREEN_HANDLER, syncId);
        checkSize(inventory, 3);
        this.inventory = inventory;
        inventory.onOpen(playerInventory.player);

        // Container Inventory (1 row of 3 centered slots)
        for (int l = 0; l < 3; ++l) {
            this.addSlot(new Slot(inventory, l, 62 + l * 18, 20));
        }

        // Player Inventory
        for (int m = 0; m < 3; ++m) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + m * 9 + 9, 8 + l * 18, 51 + m * 18));
            }
        }

        // Player Hotbar
        for (int m = 0; m < 9; ++m) {
            this.addSlot(new Slot(playerInventory, m, 8 + m * 18, 109));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            // Transferring FROM container TO player inventory
            if (invSlot < this.inventory.size()) {
                if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // Transferring FROM player inventory TO container
            else if (!this.insertItem(originalStack, 0, this.inventory.size(), false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }

            if (originalStack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, originalStack);
        }

        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
}

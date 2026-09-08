package net.fabricmc.mygolf.mixin;

import net.fabricmc.mygolf.items.GolfBall;
import net.fabricmc.mygolf.registry.RegisterItems;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public class AnvilScreenHandlerMixin {

    /// Sanitize to stackable ball on anvil
    @Inject(method = "updateResult", at = @At("TAIL"))
    private void sanitizeGolfBallAnvilOutput(CallbackInfo ci) {
        AnvilScreenHandler handler = (AnvilScreenHandler) (Object) this;

        ItemStack outputStack = handler.getSlot(2).getStack();

        if (outputStack.getItem() instanceof GolfBall) {
            GolfBall.sanitizeNbt(outputStack);
        }
    }

    /// Give player UUID to ball on renaming
    @Inject(method = "onTakeOutput", at = @At("HEAD"))
    private void stampGolfBallOwnerOnTake(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        // Check if the output item is a Golf Ball and has been given a custom name
        if (stack.isOf(RegisterItems.GOLF_BALL) && stack.hasCustomName()) {
            stack.getOrCreateNbt().putUuid("OwnerUUID", player.getUuid());
        }
    }
}
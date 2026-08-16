package net.fabricmc.mygolf.mixin;

import net.fabricmc.mygolf.items.GolfBall;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public class AnvilScreenHandlerMixin {

    @Inject(method = "updateResult", at = @At("TAIL"))
    private void sanitizeGolfBallAnvilOutput(CallbackInfo ci) {
        AnvilScreenHandler handler = (AnvilScreenHandler) (Object) this;

        // Slot 2 is the Anvil output slot
        ItemStack outputStack = handler.getSlot(2).getStack();

        if (outputStack.getItem() instanceof GolfBall) {
            GolfBall.sanitizeNbt(outputStack);
        }
    }
}
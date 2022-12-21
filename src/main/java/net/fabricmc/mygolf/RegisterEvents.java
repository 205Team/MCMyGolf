package net.fabricmc.mygolf;

import com.jme3.math.Vector3f;
import dev.lazurite.rayon.impl.bullet.thread.PhysicsThread;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;

public class RegisterEvents {
    public static void registerEvents() {

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
        {
            /* 手动的旁观者检查是必要的，因为 AttackBlockCallbacks 会在旁观者检查之前应用 */
            //左击高尔夫球
            if(entity instanceof GolfBallEntity && !player.isSpectator()) {
                PhysicsThread
                        .get(world.getServer())
                        .execute(()->{((GolfBallEntity) entity).getRigidBody().applyCentralImpulse(new Vector3f(0.0f,3.0f,0.0f));});
            }
            return ActionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
        {
            //右击高尔夫球
            if(entity instanceof GolfBallEntity && !player.isSpectator() && player.getMainHandStack().isEmpty()) {
                player.giveItemStack(new ItemStack(RegisterItems.IRON_STICK_ITEM));
                entity.kill();
            }
            return ActionResult.PASS;
        });
    }
}

package net.fabricmc.mygolf;

import com.jme3.math.Vector3f;
import dev.lazurite.rayon.impl.bullet.thread.PhysicsThread;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class RegisterEvents {
    public static void registerEvents() {

        //事件：左击实体
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
        {
            /* 手动的旁观者检查是必要的，因为 AttackBlockCallbacks 会在旁观者检查之前应用 */
            //敲打高尔夫球 远低近高
            if(entity instanceof GolfBallEntity && !player.isSpectator()) {
                //根据玩家与球的位置计算
                Vec3d ballPos = entity.getPos();
                Vec3d playerPos = player.getPos();
                Vec3d posDelta3d = ballPos.subtract(playerPos);
                //Vector3f posDelta3f = new Vector3f((float) posDelta3d.x,(float) posDelta3d.y,(float) posDelta3d.z);
                Vec2f posDelta2f = new Vec2f((float) posDelta3d.x,(float) posDelta3d.z); //水平方向向量差
                float hitDistance = posDelta2f.length(); //向量模
                Vec2f hitDirection = posDelta2f.normalize(); //单位向量
                float hitPitch = (30-hitDistance)/5;
                Vector3f impulse = new Vector3f(hitDirection.x * 5, hitPitch, hitDirection.y * 5);
                //rayon:给球一个冲量
                PhysicsThread
                        .get(world.getServer())
                        .execute(()->{((GolfBallEntity) entity).getRigidBody().applyCentralImpulse(impulse);});
                if(world.isClient)
                    System.out.println("imp " + impulse);
            }
            return ActionResult.PASS;
        });

        //事件：右击实体
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
        {
            //捡起高尔夫球
            if(entity instanceof GolfBallEntity && !player.isSpectator() && player.getMainHandStack().isEmpty()) {
                player.giveItemStack(new ItemStack(RegisterItems.IRON_STICK_ITEM));
                entity.kill();
            }
            return ActionResult.PASS;
        });
    }
}

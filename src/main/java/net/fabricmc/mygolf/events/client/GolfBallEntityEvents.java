package net.fabricmc.mygolf.events.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * 高尔夫球实体相关特殊事件
 */
public class GolfBallEntityEvents {
    public static boolean isPrevLookBall = false;

    /**
     * 高尔夫球箭头渲染
     */
    public static ClientTickEvents.EndTick golfBallArrowRenderEvent() {
        return client ->
        {
            //视线投射击中对象
            HitResult hit = client.crosshairTarget;
            //玩家是否看向实体
            if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) hit;
                Entity entity = entityHit.getEntity();
                //玩家是否看向高尔夫球
                if (entity instanceof GolfBallEntity) {
                    //渲染箭头
                    if (!isPrevLookBall) {
                        client.player.sendMessage(Text.of("look at ball"), true);
                    }
                    isPrevLookBall = true;
                    return;
                }
            }
            if (isPrevLookBall) {
                client.player.sendMessage(Text.of("look away ball"), true);
            }
            isPrevLookBall = false;
        };
    }
}

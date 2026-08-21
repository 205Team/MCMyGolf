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
public class GolfBallUIEvents {
    public static boolean isPrevLookBall = false;

    /**
     * 高尔夫球箭头渲染
     */
    public static ClientTickEvents.EndTick golfBallArrowRenderEvent() {
        return client ->
        {
            //To do
        };
    }
}

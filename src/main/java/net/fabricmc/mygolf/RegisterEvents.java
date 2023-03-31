package net.fabricmc.mygolf;

import dev.lazurite.rayon.api.event.collision.ElementCollisionEvents;
import net.fabricmc.mygolf.blocks.GolfHole;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.events.client.ItemGroupClassifyingEvents;
import net.fabricmc.mygolf.events.client.MyClientTickEvents;

public class RegisterEvents {
    /**
     * 注册所有事件
     */
    public static void registerEvents() {

        //高尔夫球实体事件
        GolfBallEntity.registerEvents();

        //注册tick事件
        registerTickEvent();

        //注册物品类事件
        ItemGroupClassifyingEvents.registerEvents();

        ElementCollisionEvents.BLOCK_COLLISION.register((element, terrain, impulse) -> {
            if (element instanceof GolfBallEntity) {
                if (terrain.getBlockState().getBlock().equals(RegisterBlocks.GOLF_HOLE)) {
                    System.out.println("Collided with Hole!!");
                }
            }
        });

    }

    /**
     * 注册tick相关事件
     */
    public static void registerTickEvent() {
        MyClientTickEvents.registerEvents();
    }
}

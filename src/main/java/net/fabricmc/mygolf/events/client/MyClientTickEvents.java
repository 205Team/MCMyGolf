package net.fabricmc.mygolf.events.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/// 客户端tick事件
public class MyClientTickEvents {

    /** 注册所有事件*/
    public static void registerEvents() {
        registerEvent(GolfBallEntityEvents.golfBallArrowRenderEvent());
    }

    /** 注册单个事件*/
    private static void registerEvent(ClientTickEvents.EndTick event) {
        ClientTickEvents.END_CLIENT_TICK.register(event);
    }


}

package net.fabricmc.mygolf;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.mygolf.events.GolfBallEntityEvents;
import net.fabricmc.mygolf.events.client.ItemGroupClassifyingEvents;
import net.fabricmc.mygolf.events.client.MyClientTickEvents;
import net.fabricmc.mygolf.global.CommonStr;
import net.fabricmc.mygolf.global.ModConfig;
import net.fabricmc.mygolf.registry.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyGolfMod implements ModInitializer {
        // This logger is used to write text to the console and the log file.
        // It is considered best practice to use your mod id as the logger's name.
        // That way, it's clear which mod wrote info, warnings, and errors.
        public static final Logger LOGGER = LoggerFactory.getLogger(CommonStr.modId);

        @Override
        public void onInitialize() {
                //Read configs
                ModConfig.load();

                //注册方块
                RegisterBlocks.registryBlocks();
                //注册方块实体
                RegisterBlockEntities.registerBlockEntities();
                //注册物品
                RegisterItems.registryItems();
                //注册实体
                RegisterEntities.registryEntities();
                //注册声音
                RegisterSounds.registrySounds();

                //注册事件
                //注册高尔夫球实体事件
                GolfBallEntityEvents.registerEvents();
                //注册tick事件
                MyClientTickEvents.registerEvents();
                //注册物品类事件
                ItemGroupClassifyingEvents.registerEvents();

                LOGGER.info("Hello Fabric world!");
        }
}

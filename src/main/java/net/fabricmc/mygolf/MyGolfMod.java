package net.fabricmc.mygolf;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.mygolf.global.CommonStr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MyGolfMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(CommonStr.modId);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		//注册物品
		RegisterItems.registryItems();
		//注册方块
		RegisterBlocks.registryBlocks();
		//注册实体
		RegisterEntities.registryEntities();
		LOGGER.info("Hello Fabric world!");
	}

}


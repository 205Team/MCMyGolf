package net.fabricmc.mygolf;

import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.minecraft.entity.EntityType;

public class RegisterEntities {
    public static EntityType<GolfBallEntity> GOLF_BALL;
    public static void registryEntities() {
        GOLF_BALL = GolfBallEntity.register();
    }

}

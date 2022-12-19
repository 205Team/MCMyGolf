package net.fabricmc.mygolf;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.global.CommonStr;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class RegisterEntities {
    public static EntityType<GolfBallEntity> GOLF_BALL;
    public static void registryEntities() {
        GOLF_BALL = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier(CommonStr.modId, "golf_ball_entity"),
                FabricEntityTypeBuilder.createLiving()
                        .entityFactory(GolfBallEntity::new)
                        .spawnGroup(SpawnGroup.MISC)
                        .defaultAttributes(LivingEntity::createLivingAttributes)
                        .dimensions(EntityDimensions.fixed(0.75f, 0.75f))
                        .trackRangeBlocks(80)
                        .build());
    }

    private static void registryEntity() {

    }
}

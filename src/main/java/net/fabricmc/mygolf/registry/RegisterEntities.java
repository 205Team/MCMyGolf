package net.fabricmc.mygolf.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.global.CommonStr;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class RegisterEntities {
    public static final EntityType<GolfBallEntity> GOLF_BALL = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(CommonStr.modId, "golf_ball_entity"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, GolfBallEntity::new)
                    .dimensions(EntityDimensions.fixed(GolfBallEntity.BALL_DIMENSIONS.width, GolfBallEntity.BALL_DIMENSIONS.height)) // Small collision box for golf ball
                    .trackRangeBlocks(256)                             // Network sync range
                    .trackedUpdateRate(1)                              // Update every tick for smooth physics
                    .build()
    );

    public static void registryEntities() {

    }

}

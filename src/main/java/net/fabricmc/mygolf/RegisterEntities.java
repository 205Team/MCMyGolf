package net.fabricmc.mygolf;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.global.CommonStr;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class RegisterEntities {
    private static Object CubeEntity;
    public static final EntityType<GolfBallEntity> GOLF_BALL = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier(CommonStr.modId, "golf_ball_entity"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, GolfBallEntity::new).dimensions(EntityDimensions.fixed(0.75f, 0.75f)).build());
    public static void registryEntities() {
        FabricDefaultAttributeRegistry.register(GOLF_BALL, GolfBallEntity.createMobAttributes());
    }

    private static void registryEntity() {

    }
}

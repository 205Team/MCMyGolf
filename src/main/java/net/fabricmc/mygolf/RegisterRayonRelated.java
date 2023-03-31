package net.fabricmc.mygolf;

import dev.lazurite.rayon.impl.bullet.collision.space.block.BlockProperty;

public class RegisterRayonRelated {
    public static void registryRayonRelated() {
        BlockProperty.addBlockProperty(RegisterBlocks.GOLF_HOLE, 0.75f, 0.25f, true, false);
    }
}

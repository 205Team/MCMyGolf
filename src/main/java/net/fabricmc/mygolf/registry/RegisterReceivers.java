package net.fabricmc.mygolf.registry;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.items.GolfClubItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

import static net.fabricmc.mygolf.events.client.GolfClubEvents.UNDO_PACKET_ID;

public class RegisterReceivers {
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(UNDO_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                ItemStack heldClub = player.getMainHandStack();
                if (!(heldClub.getItem() instanceof GolfClubItem) || !heldClub.hasNbt()) return;

                NbtCompound nbt = heldClub.getNbt();
                if (nbt == null || !nbt.getBoolean("HasUndo")) {
                    player.sendMessage(Text.literal("No shot available to undo!").formatted(Formatting.RED), true);
                    return;
                }

                ServerWorld world = (ServerWorld) player.getWorld();
                Vec3d targetPos = new Vec3d(nbt.getDouble("UndoX"), nbt.getDouble("UndoY"), nbt.getDouble("UndoZ"));

                BlockPos targetBlockPos = BlockPos.ofFloored(targetPos);
                if (!world.isChunkLoaded(targetBlockPos.getX() >> 4, targetBlockPos.getZ() >> 4)) {
                    player.sendMessage(Text.literal("Cannot undo to an unloaded chunk!").formatted(Formatting.RED), true);
                    return;
                }

                UUID ballUuid = nbt.getUuid("UndoBallUuid");
                GolfBallEntity ball = (GolfBallEntity) world.getEntity(ballUuid);
                if (ball != null && ball.isAlive()) {
                    world.playSound(
                            null,
                            targetPos.x, targetPos.y, targetPos.z,
                            SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                            SoundCategory.PLAYERS,
                            1.0F,
                            2.0F
                    );
                    world.spawnParticles(
                            ParticleTypes.INSTANT_EFFECT,
                            targetPos.x, targetPos.y + 0.1, targetPos.z,
                            30,
                            0.25, 0.25, 0.25,
                            0.05
                    );

                    // Revert state & position
                    ball.setVelocity(Vec3d.ZERO);
                    ball.setSpin(Vec3d.ZERO);
                    ball.requestTeleport(targetPos.x, targetPos.y, targetPos.z);
                    ball.velocityDirty = true;

                    // Clear snapshot & reset prompt tick
                    nbt.putBoolean("HasUndo", false);
                    nbt.putLong("UndoPromptTick", 0);

                    player.sendMessage(Text.literal("✓ Last shot undone!").formatted(Formatting.GREEN, Formatting.BOLD), true);
                } else {
                    player.sendMessage(Text.literal("Golf ball could not be found!").formatted(Formatting.RED), true);
                }

            });
        });

    }
}

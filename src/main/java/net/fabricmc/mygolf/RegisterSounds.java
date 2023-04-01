package net.fabricmc.mygolf;

import net.fabricmc.mygolf.global.CommonStr;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class RegisterSounds {
    public static final Identifier GOLF_BALL_HIT_SOUND_ID = new Identifier(CommonStr.modId, "golf_ball_hit");
    public static SoundEvent GOLF_BALL_HIT_SOUND_EVENT = SoundEvent.of(GOLF_BALL_HIT_SOUND_ID);
    public static final Identifier GOLF_CLUB_SWING_SOUND_ID = new Identifier(CommonStr.modId, "golf_club_swing");
    public static SoundEvent GOLF_CLUB_SWING_SOUND_EVENT = SoundEvent.of(GOLF_CLUB_SWING_SOUND_ID);

    public static void registrySounds() {
        Registry.register(Registries.SOUND_EVENT, GOLF_BALL_HIT_SOUND_ID, GOLF_BALL_HIT_SOUND_EVENT);
        Registry.register(Registries.SOUND_EVENT, GOLF_CLUB_SWING_SOUND_ID, GOLF_CLUB_SWING_SOUND_EVENT);
    }
}

package com.tacz.guns.config.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public class SoundConfig {
    public static ModConfigSpec.IntValue HIT_SOUND_CONCURRENCY_LIMIT;
    public static ModConfigSpec.IntValue DEFAULT_SOUND_CONCURRENCY_LIMIT;
    public static ModConfigSpec.IntValue HIGH_FREQUENCY_SOUND_CONCURRENCY_LIMIT;
    public static ModConfigSpec.BooleanValue FIRST_PERSON_ANIMATION_SOUND_TRACKING;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("sound");

        builder.comment("Max active hit marker sounds for the same entity and sound id. 0 disables this limit.");
        HIT_SOUND_CONCURRENCY_LIMIT = builder.defineInRange("HitSoundConcurrencyLimit", 1, 0, 128);

        builder.comment("Max active normal gun sounds for the same entity and sound id. 0 disables this limit.");
        DEFAULT_SOUND_CONCURRENCY_LIMIT = builder.defineInRange("DefaultSoundConcurrencyLimit", 2, 0, 128);

        builder.comment("Max active high-frequency gun sounds, such as shooting and animation keyframe sounds, for the same entity and sound id. 0 disables this limit.");
        HIGH_FREQUENCY_SOUND_CONCURRENCY_LIMIT = builder.defineInRange("HighFrequencySoundConcurrencyLimit", 4, 0, 128);

        builder.comment("Use a non-relative entity-tracking world sound source for first-person animation keyframe sounds. This can improve compatibility with physical sound mods, but may introduce slight stereo drift while moving.");
        FIRST_PERSON_ANIMATION_SOUND_TRACKING = builder.define("FirstPersonAnimationSoundTracking", false);

        builder.pop();
    }
}

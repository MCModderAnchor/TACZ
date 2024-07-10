package com.tacz.guns.api.client.animation;

public interface AnimationListener {
    /**
     * @param values When ChannelType is TRANSLATION, the length of values will be 3 and will store xyz offsets.
     *               When ChannelType is ROTATION, the length of values will be 4 OR 3. When it is 4, it means it is a quaternion.
     *               When ChannelType is SCALE, the length of values will be 3 and will store xyz scale.
     * @param blend  When blending, animation value should be accumulated, instead of being covered.
     */
    void update(float[] values, boolean blend);

    float[] initialValue();

    ObjectAnimationChannel.ChannelType getType();
}

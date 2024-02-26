package com.tac.guns.client.animation;


import javax.annotation.Nullable;

public interface AnimationListenerSupplier {
    @Nullable
    AnimationListener supplyListeners(String nodeName, ObjectAnimationChannel.ChannelType type);
}

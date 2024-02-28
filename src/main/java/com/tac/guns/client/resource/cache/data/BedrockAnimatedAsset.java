package com.tac.guns.client.resource.cache.data;

import com.tac.guns.client.animation.AnimationController;
import com.tac.guns.client.model.BedrockAnimatedModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record BedrockAnimatedAsset(@Nonnull BedrockAnimatedModel model,
                                   @Nullable AnimationController defaultController) {
}

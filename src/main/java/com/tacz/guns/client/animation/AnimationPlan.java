package com.tacz.guns.client.animation;

public class AnimationPlan {
    public String animationName;
    public ObjectAnimation.PlayType playType;
    public float transitionTimeS;

    public AnimationPlan(String animationName, ObjectAnimation.PlayType playType, float transitionTimeS) {
        this.animationName = animationName;
        this.playType = playType;
        this.transitionTimeS = transitionTimeS;
    }
}
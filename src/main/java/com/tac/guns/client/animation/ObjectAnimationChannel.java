package com.tac.guns.client.animation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ObjectAnimationChannel {
    public final ChannelType type;
    private final List<AnimationListener> listeners = new ArrayList<>();
    /**
     * name of node
     */
    public String node;
    /**
     * The key frame times, in seconds
     */
    public AnimationChannelContent content;
    /**
     * This variable is used for animation transitions.
     * Please don't change it if you don't understand what you are doing.
     */
    boolean transitioning = false;

    public ObjectAnimationChannel(ChannelType type) {
        this.type = type;
        this.content = new AnimationChannelContent();
    }

    public ObjectAnimationChannel(ChannelType type, AnimationChannelContent content) {
        this.type = type;
        this.content = content;
    }

    public void addListener(AnimationListener listener) {
        if (listener.getType().equals(type))
            listeners.add(listener);
        else
            throw new RuntimeException("trying to add wrong type of listener to channel.");
    }

    public void removeListener(AnimationListener listener) {
        listeners.remove(listener);
    }

    public void clearListeners() {
        listeners.clear();
    }

    public List<AnimationListener> getListeners() {
        return listeners;
    }

    public float getEndTimeS() {
        return content.keyframeTimeS[content.keyframeTimeS.length - 1];
    }

    /**
     * Perform a calculation based on the input time and notify all AnimationListener of the result
     *
     * @param timeS absolute time in seconds
     */
    public void update(float timeS) {
        if (!transitioning) {
            float[] result = getResult(timeS);
            for (AnimationListener listener : listeners) {
                listener.update(result);
            }
        }
    }

    public float[] getResult(float timeS) {
        int indexFrom = computeIndex(timeS);
        int indexTo = Math.min(content.keyframeTimeS.length - 1, indexFrom + 1);
        float alpha = computeAlpha(timeS, indexFrom);

        float[] result = new float[content.values[indexFrom].length];
        content.interpolator.interpolate(indexFrom, indexTo, alpha, result);

        return result;
    }

    private int computeIndex(float timeS) {
        int index = Arrays.binarySearch(content.keyframeTimeS, timeS);
        if (index >= 0) {
            return index;
        }
        return Math.max(0, -index - 2);
    }

    private float computeAlpha(float timeS, int indexFrom) {
        if (timeS <= content.keyframeTimeS[0]) {
            return 0.0f;
        }
        if (timeS >= content.keyframeTimeS[content.keyframeTimeS.length - 1]) {
            return 1.0f;
        }
        float local = timeS - content.keyframeTimeS[indexFrom];
        float delta = content.keyframeTimeS[indexFrom + 1] - content.keyframeTimeS[indexFrom];
        return local / delta;
    }

    public enum ChannelType {
        TRANSLATION,
        ROTATION,
        SCALE
    }
}

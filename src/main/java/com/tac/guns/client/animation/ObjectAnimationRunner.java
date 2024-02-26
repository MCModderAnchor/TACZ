package com.tac.guns.client.animation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class ObjectAnimationRunner {
    @Nonnull
    private final ObjectAnimation animation;
    protected long transitionTimeNs;
    /**
     * 用于动画过渡，储存的是过渡起点动画的值，与下方transitionFromChannels一一对应
     */
    protected ArrayList<float[]> valueFrom;
    /**
     * 用于动画过渡，储存的是过渡起点需要恢复原位的channel的值，与下方recoverChannels一一对应
     */
    protected ArrayList<float[]> valueRecover;
    /**
     * 用于动画过渡，储存的是过渡起点动画的channel
     */
    protected ArrayList<ObjectAnimationChannel> transitionFromChannels;
    /**
     * 用于动画过渡，储存的是过渡终点动画的channel，顺序与上面对应
     */
    protected ArrayList<ObjectAnimationChannel> transitionToChannels;
    /**
     * 用于动画过渡，储存的是过渡起点动画需要恢复到原位的channel
     */
    protected ArrayList<ObjectAnimationChannel> recoverChannels;
    private boolean running = false;
    private long lastUpdateNs;
    /**
     * current animation playback progress
     */
    private long progressNs;
    private boolean isTransitioning = false;
    @Nullable
    private ObjectAnimationRunner transitionTo;
    private long transitionProgressNs;

    public ObjectAnimationRunner(@Nonnull ObjectAnimation animation) {
        this.animation = Objects.requireNonNull(animation);
    }

    public @Nonnull ObjectAnimation getAnimation() {
        return animation;
    }

    public @Nullable ObjectAnimationRunner getTransitionTo() {
        return transitionTo;
    }

    public boolean isTransitioning() {
        return isTransitioning;
    }

    public void run() {
        if (!running) {
            running = true;
            lastUpdateNs = System.nanoTime();
        }
    }

    public void pause() {
        running = false;
    }

    public void hold() {
        progressNs = (long) (animation.getMaxEndTimeS() * 1e9) + 1;
        pause();
    }

    public void reset() {
        progressNs = 0;
    }

    public long getProgressNs() {
        return progressNs;
    }

    public void setProgressNs(long progressNs) {
        this.progressNs = progressNs;
    }

    public void transition(ObjectAnimationRunner transitionTo, long transitionTimeNS) {
        if (this.transitionTo == null) {
            this.valueFrom = new ArrayList<>();
            this.valueRecover = new ArrayList<>();
            this.transitionFromChannels = new ArrayList<>();
            this.transitionToChannels = new ArrayList<>();
            this.recoverChannels = new ArrayList<>();
            this.transitionTo = transitionTo;
            this.pause();
            for (Map.Entry<String, List<ObjectAnimationChannel>> entry : animation.getChannels().entrySet()) {
                List<ObjectAnimationChannel> toChannels = transitionTo.animation.getChannels().get(entry.getKey());
                if (toChannels != null) {
                    //如果过渡终点的动画中 同一个node 包含相同类型的动画数据(位移、旋转、缩放)，那么加入到list中用于更新。
                    for (ObjectAnimationChannel channel : entry.getValue()) {
                        Optional<ObjectAnimationChannel> toChannel =
                                toChannels.stream().filter(c -> c.type.equals(channel.type)).findAny();
                        if (toChannel.isPresent()) {
                            valueFrom.add(channel.getResult(progressNs / 1e9f));
                            transitionFromChannels.add(channel);
                            transitionToChannels.add(toChannel.get());
                            //取消过渡目标的channel对模型的更新，统一在起点channel进行更新。
                            toChannel.get().transitioning = true;
                        } else {
                            valueRecover.add(channel.getResult(progressNs / 1e9f));
                            recoverChannels.add(channel);
                        }
                    }
                } else {
                    //如果过渡终点的动画中 同一个node 不包含动画数据，那么将过渡到原位。
                    for (ObjectAnimationChannel channel : entry.getValue()) {
                        valueRecover.add(channel.getResult(progressNs / 1e9f));
                        recoverChannels.add(channel);
                    }
                }
            }
        } else if (isTransitioning) {
            ArrayList<float[]> newValueFrom = new ArrayList<>();
            ArrayList<float[]> newValueRecover = new ArrayList<>();
            ArrayList<ObjectAnimationChannel> newTransitionFromChannels = new ArrayList<>();
            ArrayList<ObjectAnimationChannel> newTransitionToChannels = new ArrayList<>();
            ArrayList<ObjectAnimationChannel> newRecoverChannels = new ArrayList<>();
            //如果正在过渡，则需要把当前过渡计算出的插值保存，作为下次过渡的起点
            for (int i = 0; i < transitionFromChannels.size(); i++) {
                assert this.transitionTo != null;
                ObjectAnimationChannel fromChannel = transitionFromChannels.get(i);
                ObjectAnimationChannel toChannel = transitionToChannels.get(i);
                float[] from = valueFrom.get(i);
                float[] to = toChannel.getResult(this.transitionTo.progressNs / 1e9f);
                float[] result = new float[from.length];
                float progress = easeOutCubic((float) transitionProgressNs / transitionTimeNs);
                if (fromChannel.type.equals(ObjectAnimationChannel.ChannelType.TRANSLATION)) {
                    lerp(from, to, progress, result);
                } else if (fromChannel.type.equals(ObjectAnimationChannel.ChannelType.ROTATION)) {
                    slerp(from, to, progress, result);
                } else if (fromChannel.type.equals(ObjectAnimationChannel.ChannelType.SCALE)) {
                    lerp(from, to, progress, result);
                }

                List<ObjectAnimationChannel> newToChannels = transitionTo.animation.getChannels().get(fromChannel.node);
                if (newToChannels != null) {
                    Optional<ObjectAnimationChannel> newToChannel =
                            newToChannels.stream().filter(c -> c.type.equals(fromChannel.type)).findAny();
                    if (newToChannel.isPresent()) {
                        newValueFrom.add(result);
                        newTransitionFromChannels.add(fromChannel);
                        newTransitionToChannels.add(newToChannel.get());
                        //取消过渡目标的channel对模型的更新，统一在起点channel进行更新。
                        newToChannel.get().transitioning = true;
                    } else {
                        newValueRecover.add(result);
                        newRecoverChannels.add(fromChannel);
                    }
                } else {
                    newValueRecover.add(result);
                    newRecoverChannels.add(fromChannel);
                }
                toChannel.transitioning = false;
            }
            this.valueFrom = newValueFrom;
            this.valueRecover = newValueRecover;
            this.transitionToChannels = newTransitionToChannels;
            this.transitionFromChannels = newTransitionFromChannels;
            this.recoverChannels = newRecoverChannels;
            this.transitionTo = transitionTo;
        }
        this.transitionTimeNs = transitionTimeNS;
        this.transitionProgressNs = 0;
        this.isTransitioning = true;
    }

    public long getTransitionTimeNs() {
        return transitionTimeNs;
    }

    public long getTransitionProgressNs() {
        return transitionProgressNs;
    }

    public void setTransitionProgressNs(long progressNs) {
        this.transitionProgressNs = progressNs;
    }

    public void stopTransition() {
        this.isTransitioning = false;
        for (ObjectAnimationChannel channel : transitionToChannels) {
            channel.transitioning = false;
        }
        this.transitionTimeNs = 0;
        this.transitionProgressNs = 0;
        this.transitionFromChannels = null;
        this.transitionToChannels = null;
        this.recoverChannels = null;
        this.valueFrom = null;
        this.valueRecover = null;
    }

    public void update() {
        long currentNs = System.nanoTime();

        if (running) {
            progressNs += currentNs - lastUpdateNs;
        }
        switch (animation.playType) {
            case PLAY_ONCE_HOLD -> {
                if (progressNs / 1e9 > animation.getMaxEndTimeS()) {
                    hold();
                }
            }
            case LOOP -> {
                if (progressNs / 1e9 > animation.getMaxEndTimeS()) {
                    progressNs = progressNs % (long) (animation.getMaxEndTimeS() * 1e9);
                }
            }
        }
        animation.timeNs = progressNs;

        if (isTransitioning) {
            transitionProgressNs += currentNs - lastUpdateNs;
            if (transitionProgressNs >= transitionTimeNs) {
                stopTransition();
            } else {
                float transitionProgress = (float) transitionProgressNs / transitionTimeNs;
                updateTransition(easeOutCubic(transitionProgress));
            }
        } else {
            animation.update();
        }

        lastUpdateNs = currentNs;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isHolding() {
        return progressNs == (long) (getAnimation().getMaxEndTimeS() * 1e9) + 1;
    }

    /**
     * 动画过渡的时候，计算出的插值将通过当前Runner中包含的ObjectAnimation中的channel对模型进行update
     * 这意味着需要暂时取消transitionTo中对应channel的update功能(将变量available设置为false)
     */
    private void updateTransition(float progress) {
        assert transitionTo != null;
        for (int i = 0; i < transitionToChannels.size(); i++) {
            ObjectAnimationChannel fromChannel = transitionFromChannels.get(i);
            ObjectAnimationChannel toChannel = transitionToChannels.get(i);

            float[] from = valueFrom.get(i);
            float[] to = toChannel.getResult(transitionTo.progressNs / 1e9f);
            float[] result = new float[from.length];

            if (fromChannel.type.equals(ObjectAnimationChannel.ChannelType.TRANSLATION)) {
                lerp(from, to, progress, result);
            } else if (fromChannel.type.equals(ObjectAnimationChannel.ChannelType.ROTATION)) {
                slerp(from, to, progress, result);
            } else if (fromChannel.type.equals(ObjectAnimationChannel.ChannelType.SCALE)) {
                lerp(from, to, progress, result);
            }
            for (AnimationListener listener : fromChannel.getListeners()) {
                listener.update(result);
            }

        }
        for (int i = 0; i < recoverChannels.size(); i++) {
            ObjectAnimationChannel channel = recoverChannels.get(i);
            float[] from = valueRecover.get(i);
            float[] result = new float[from.length];
            if (channel.type.equals(ObjectAnimationChannel.ChannelType.TRANSLATION)) {
                for (AnimationListener listener : channel.getListeners()) {
                    float[] to = listener.recover();
                    lerp(from, to, progress, result);
                    listener.update(result);
                }
            } else if (channel.type.equals(ObjectAnimationChannel.ChannelType.ROTATION)) {
                for (AnimationListener listener : channel.getListeners()) {
                    float[] to = listener.recover();
                    slerp(from, to, progress, result);
                    listener.update(result);
                }
            } else if (channel.type.equals(ObjectAnimationChannel.ChannelType.SCALE)) {
                for (AnimationListener listener : channel.getListeners()) {
                    float[] to = listener.recover();
                    lerp(from, to, progress, result);
                    listener.update(result);
                }
            }
        }
    }

    private float easeOutCubic(double x) {
        return (float) (1 - Math.pow(1 - x, 4));
    }

    private void lerp(float[] from, float[] to, float alpha, float[] result) {
        for (int i = 0; i < result.length; i++) {
            result[i] = from[i] * (1 - alpha) + to[i] * alpha;
        }
    }

    private void slerp(float[] from, float[] to, float alpha, float[] result) {
        float ax = from[0];
        float ay = from[1];
        float az = from[2];
        float aw = from[3];
        float bx = to[0];
        float by = to[1];
        float bz = to[2];
        float bw = to[3];

        float dot = ax * bx + ay * by + az * bz + aw * bw;
        if (dot < 0) {
            bx = -bx;
            by = -by;
            bz = -bz;
            bw = -bw;
            dot = -dot;
        }
        float epsilon = 1e-6f;
        float s0, s1;
        if ((1.0 - dot) > epsilon) {
            float omega = (float) Math.acos(dot);
            float invSinOmega = 1.0f / (float) Math.sin(omega);
            s0 = (float) Math.sin((1.0 - alpha) * omega) * invSinOmega;
            s1 = (float) Math.sin(alpha * omega) * invSinOmega;
        } else {
            s0 = 1.0f - alpha;
            s1 = alpha;
        }
        float rx = s0 * ax + s1 * bx;
        float ry = s0 * ay + s1 * by;
        float rz = s0 * az + s1 * bz;
        float rw = s0 * aw + s1 * bw;
        result[0] = rx;
        result[1] = ry;
        result[2] = rz;
        result[3] = rw;
    }
}

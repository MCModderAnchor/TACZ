package com.tacz.guns.api.client.animation.statemachine;

import com.tacz.guns.api.client.animation.AnimationController;
import com.tacz.guns.api.client.animation.DiscreteTrackArray;
import com.tacz.guns.api.client.animation.ObjectAnimationRunner;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AnimationStateContext {
    private @Nullable AnimationStateMachine<?> stateMachine;

    public @Nullable AnimationStateMachine<?> getStateMachine() {
        return stateMachine;
    }

    public DiscreteTrackArray getTrackArray() {
        if (stateMachine == null) {
            throw new IllegalStateException("This context has not been bound to a statemachine yet.");
        }
        return (DiscreteTrackArray) stateMachine.getAnimationController().getUpdatingTrackArray();
    }

    public int addTrackLine() {
        return getTrackArray().addTrackLine();
    }

    public int assignNewTrack(int index) {
        return getTrackArray().assignNewTrack(index);
    }

    /**
     * 优先返回轨道行中的空闲轨道，如果没有空闲轨道则会开辟一个新的轨道
     * @param index 轨道行的下标
     * @param interruptHolding 是否将处于 holding 状态的轨道视为空闲轨道
     * @see AnimationStateContext#assignNewTrack(int)
     */
    public int findIdleTrack(int index, boolean interruptHolding) {
        List<Integer> trackList = getTrackArray().getByIndex(index);
        if (stateMachine == null) {
            return -1;
        }
        AnimationController controller = stateMachine.getAnimationController();
        for (int track : trackList) {
            ObjectAnimationRunner animation = controller.getAnimation(track);
            if (animation == null || animation.isStopped() || (interruptHolding && animation.isHolding())) {
                return track;
            }
        }
        return assignNewTrack(index);
    }

    void setStateMachine(@Nullable AnimationStateMachine<?> stateMachine) {
        if (this.stateMachine != null) {
            this.stateMachine.getAnimationController().setUpdatingTrackArray(null);
        }
        this.stateMachine = stateMachine;
        if (stateMachine != null) {
            stateMachine.getAnimationController().setUpdatingTrackArray(new DiscreteTrackArray());
        }
    }
}

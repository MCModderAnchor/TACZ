package com.tacz.guns.api.client.animation.statemachine;

import com.tacz.guns.api.client.animation.AnimationController;
import com.tacz.guns.api.client.animation.DiscreteTrackArray;
import com.tacz.guns.api.client.animation.ObjectAnimation;
import com.tacz.guns.api.client.animation.ObjectAnimationRunner;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;

public class AnimationStateContext {
    private boolean shouldHideCrossHair = false;
    private @Nullable AnimationStateMachine<?> stateMachine;
    private final DiscreteTrackArray trackArray = new DiscreteTrackArray();

    /**
     * 状态机脚本不要调用此方法。
     * @return 上下文绑定的状态机。
     */
    public @Nullable AnimationStateMachine<?> getStateMachine() {
        return stateMachine;
    }

    /**
     * 状态机脚本不要调用此方法。
     * @return 上下文的离散轨道序列。
     */
    public DiscreteTrackArray getTrackArray() {
        return trackArray;
    }

    /**
     * 分配一个新的轨道行，返回新的轨道行下标。
     * @return 新的轨道行下标
     * @throws TrackArrayMismatchException 当状态机对应的 track array 不是当前 context 指定的实例，抛出此异常。
     */
    public int addTrackLine() {
        checkTrackArray();
        return getTrackArray().addTrackLine();
    }

    /**
     * 确保轨道行的数量
     * @param size 需要确保的轨道行数量。
     * @throws TrackArrayMismatchException 当状态机对应的 track array 不是当前 context 指定的实例，抛出此异常。
     */
    public void ensureTrackLineSize(int size) {
        checkTrackArray();
        getTrackArray().ensureCapacity(size);
    }

    /**
     * 获取轨道行的数量
     * @return 轨道行的数量
     * @throws TrackArrayMismatchException 当状态机对应的 track array 不是当前 context 指定的实例，抛出此异常。
     */
    public int getTrackLineSize() {
        checkTrackArray();
        return getTrackArray().getTrackLineSize();
    }

    /**
     * 为指定轨道行分配一个新的轨道，返回新的轨道的下标
     * @param index 轨道行的下标
     * @return 新的轨道下标
     * @throws TrackArrayMismatchException 当状态机对应的 track array 不是当前 context 指定的实例，抛出此异常。
     */
    public int assignNewTrack(int index) {
        checkTrackArray();
        return getTrackArray().assignNewTrack(index);
    }

    /**
     * 优先返回轨道行中的空闲轨道，如果没有空闲轨道则会开辟一个新的轨道
     * @param index 轨道行的下标
     * @param interruptHolding 是否将处于 holding 状态的轨道视为空闲轨道
     * @return 轨道在控制器中的指针
     * @throws TrackArrayMismatchException 当状态机对应的 track array 不是当前 context 指定的实例，抛出此异常。
     * @see AnimationStateContext#assignNewTrack(int)
     */
    public int findIdleTrack(int index, boolean interruptHolding) {
        var stateMachine = checkStateMachine();
        checkTrackArray();
        DiscreteTrackArray trackArray = getTrackArray();
        List<Integer> trackList = trackArray.getByIndex(index);
        AnimationController controller = stateMachine.getAnimationController();
        for (int track : trackList) {
            ObjectAnimationRunner animation = controller.getAnimation(track);
            if (animation == null || animation.isStopped() || (interruptHolding && animation.isHolding())) {
                return track;
            }
        }
        return trackArray.assignNewTrack(index);
    }

    /**
     * 保证指定的轨道行有足够的轨道数量
     * @param index 轨道行下标
     * @param amount 需要的轨道数量
     */
    public void ensureTracksAmount(int index, int amount) {
        checkTrackArray();
        getTrackArray().ensureTrackAmount(index, amount);
    }

    /**
     * 获取轨道指针
     * @param trackLineIndex 轨道行的下标
     * @param trackIndex 轨道的下标
     * @return 轨道在控制器中的指针，或者 -1 当轨道不存在
     */
    public int getTrack(int trackLineIndex, int trackIndex) {
        checkTrackArray();
        DiscreteTrackArray trackArray = getTrackArray();
        if (trackLineIndex >= trackArray.getTrackLineSize()) {
            return -1;
        }
        List<Integer> tracks = trackArray.getByIndex(trackLineIndex);
        if (trackIndex >= tracks.size()) {
            return -1;
        }
        return tracks.get(trackIndex);
    }

    /**
     * 用于只需要一个轨道的轨道行，如果目标轨道行没有轨道，则会分配一个轨道，
     * 如果已经有多个轨道，多余的轨道不会舍弃，会返回其中的第一个轨道。
     * @param index 轨道行的下标
     * @throws TrackArrayMismatchException 当状态机对应的 track array 不是当前 context 指定的实例，抛出此异常。
     * @return 轨道的下标
     */
    public int getAsSingletonTrack(int index) {
        checkTrackArray();
        DiscreteTrackArray trackArray = getTrackArray();
        List<Integer> trackList = trackArray.getByIndex(index);
        if (trackList.isEmpty()) {
            return trackArray.assignNewTrack(index);
        } else {
            return trackList.get(0);
        }
    }

    /**
     * 在指定轨道上运行动画。如果轨道已经有动画在运行，将会打断，并根据输入的过渡时间开始过渡。
     * 新动画在播放的瞬间就开始运行，并不会因为过渡而停止。旧动画则在播放开始的瞬间停止。
     * @param name 动画的名称
     * @param track 轨道在控制器中的指针
     * @param blending 动画是否向下混合
     * @param playType 动画的播放状态，为枚举的 ordinal 值。
     * @param transitionTime 过渡时长
     * @see AnimationConstant
     */
    public void runAnimation(String name, int track, boolean blending, int playType, float transitionTime){
        var stateMachine = checkStateMachine();
        ObjectAnimation.PlayType pt = ObjectAnimation.PlayType.values()[playType];
        stateMachine.getAnimationController().runAnimation(track, name, pt, transitionTime);
        stateMachine.getAnimationController().setBlending(track, blending);
    }

    /**
     * 将动画停止。停止后的动画关键帧不会再影响模型。
     * @param track 轨道在控制器中的指针
     */
    public void stopAnimation(int track) {
        var stateMachine = checkStateMachine();
        ObjectAnimationRunner runner = stateMachine.getAnimationController().getAnimation(track);
        if (runner != null) {
            runner.stop();
        }
    }

    /**
     * 将动画进度拖至动画末尾并挂起。挂起的动画将定格在动画的最后一帧。
     * @param track 轨道在控制器中的指针
     */
    public void holdAnimation(int track) {
        var stateMachine = checkStateMachine();
        ObjectAnimationRunner runner = stateMachine.getAnimationController().getAnimation(track);
        if (runner != null) {
            runner.hold();
        }
    }

    /**
     * 暂停动画。动画将会定格，关键帧仍然影响模型。
     * @param track 轨道在控制器中的指针
     */
    public void pauseAnimation(int track) {
        var stateMachine = checkStateMachine();
        ObjectAnimationRunner runner = stateMachine.getAnimationController().getAnimation(track);
        if (runner != null) {
            runner.pause();
        }
    }

    /**
     * 恢复动画运行。如果动画已经在运行，则什么都不会发生
     * @param track 轨道在控制器中的指针
     */
    public void resumeAnimation(int track) {
        var stateMachine = checkStateMachine();
        ObjectAnimationRunner runner = stateMachine.getAnimationController().getAnimation(track);
        if (runner != null) {
            runner.run();
        }
    }

    /**
     * 设置动画播放的绝对进度。
     * 如果启用归一化 (normalization 设为 true)，则 progress 可取值 0 ~ 1，0 代表动画开头，1 代表动画结尾。
     * 否则，progress 代表时长，单位：秒
     * @param track 轨道在控制器中的指针
     * @param progress 动画的绝对进度，如果 normalization 为 true，则可取值 0 ~ 1，0 代表动画开头，1 代表动画结尾。否则代表时长，单位为秒
     * @param normalization 是否启用归一化
     */
    public void setAnimationProgress(int track, float progress, boolean normalization) {
        var stateMachine = checkStateMachine();
        ObjectAnimationRunner runner = stateMachine.getAnimationController().getAnimation(track);
        if (runner != null) {
            if (runner.isRunning() || runner.isPausing()) {
                if (normalization) {
                    progress = runner.getAnimation().getMaxEndTimeS() * progress;
                }
                runner.setProgressNs((long) (progress * 1e9));
                return;
            }
            ObjectAnimationRunner runner1 = runner.getTransitionTo();
            if (runner1 != null) {
                if (normalization) {
                    progress = runner1.getAnimation().getMaxEndTimeS() * progress;
                }
                runner1.setProgressNs((long) (progress * 1e9));
            }
        }
    }

    /**
     * 在当前动画进度的基础上移动一段进度，比如前进 10s、后退 10s。
     * 如果启用归一化 (normalization 设为 true)，则 progress 可取值 -1 ~ 1，-1 代表后退动画全长，1 代表前进动画全长。
     * 否则，progress 代表时长，单位：秒
     * @param track 轨道在控制器中的指针
     * @param progress 相对进度，可为负值。如果 normalization 为 true，则可取值 -1 ~ 1，-1 代表后退动画全长，1 代表前进动画全长。
     *                 否则代表时长，单位为秒。
     * @param normalization 是否启用归一化
     */
    public void adjustAnimationProgress(int track, float progress, boolean normalization) {
        var stateMachine = checkStateMachine();
        ObjectAnimationRunner runner = stateMachine.getAnimationController().getAnimation(track);
        if (runner != null) {
            if (runner.isRunning()) {
                if (normalization) {
                    progress = runner.getAnimation().getMaxEndTimeS() * progress;
                }
                runner.setProgressNs(runner.getProgressNs() + (long) (progress * 1e9));
                return;
            }
            ObjectAnimationRunner runner1 = runner.getTransitionTo();
            if (runner1 != null) {
                if (normalization) {
                    progress = runner1.getAnimation().getMaxEndTimeS() * progress;
                }
                runner1.setProgressNs(runner1.getProgressNs() + (long) (progress * 1e9));
            }
        }
    }

    /**
     * 获取指定轨道是否被挂起
     * @return 返回对应轨道的动画是否挂起。轨道为空时此方法返回 false，因为轨道没有动画的时候视为轨道停止，而非挂起。
     */
    public boolean isHolding(int track) {
        var stateMachine = checkStateMachine();
        ObjectAnimationRunner runner = stateMachine.getAnimationController().getAnimation(track);
        if (runner != null) {
            return (runner.getTransitionTo() != null ? runner.getTransitionTo().isHolding() : runner.isHolding());
        } else {
            return false;
        }
    }

    /**
     * 获取指定轨道是否停止
     * @return 返回对应轨道的动画是否停止。轨道为空时此方法返回 true，因为轨道没有动画的时候视为轨道停止。
     */
    public boolean isStopped(int track) {
        var stateMachine = checkStateMachine();
        ObjectAnimationRunner runner = stateMachine.getAnimationController().getAnimation(track);
        if (runner != null) {
            return (runner.getTransitionTo() != null ? runner.getTransitionTo().isStopped() : runner.isStopped());
        } else {
            return true;
        }
    }

    /**
     * 获取指定轨道是否暂停
     * @return 返回对应轨道的动画是否暂停。轨道为空时此方法返回 false，因为轨道没有动画的时候视为轨道停止，而非暂停。
     */
    public boolean isPause(int track) {
        var stateMachine = checkStateMachine();
        ObjectAnimationRunner runner = stateMachine.getAnimationController().getAnimation(track);
        if (runner != null) {
            return (runner.getTransitionTo() != null ? !runner.getTransitionTo().isPausing() : !runner.isPausing());
        } else {
            return false;
        }
    }

    /**
     * 获取动画文件中是否存在某个动画
     * @param name 动画名称
     * @return 动画是否存在
     */
    public boolean hasAnimationPrototype(String name) {
        var stateMachine = checkStateMachine();
        AnimationController animationController = stateMachine.getAnimationController();
        return animationController.containPrototype(name);
    }

    /**
     * 手动触发一次状态转移
     * @param input 状态转移的输入
     */
    public void trigger(String input) {
        var stateMachine = checkStateMachine();
        stateMachine.trigger(input);
    }

    /**
     * 动画有时会有剧烈的视角运动，因此可能需要隐藏准心减少眩晕感。
     * @return 渲染时是否需要隐藏准心
     */
    public boolean shouldHideCrossHair() {
        return shouldHideCrossHair;
    }

    /**
     * 动画有时会有剧烈的视角运动，因此可能需要隐藏准心减少眩晕感。
     * @param shouldHideCrossHair  渲染时是否需要隐藏准心
     */
    public void setShouldHideCrossHair(boolean shouldHideCrossHair) {
        this.shouldHideCrossHair = shouldHideCrossHair;
    }

    void setStateMachine(@Nullable AnimationStateMachine<?> stateMachine) {
        if (this.stateMachine != null) {
            this.stateMachine.getAnimationController().setUpdatingTrackArray(null);
        }
        if (stateMachine != null) {
            stateMachine.getAnimationController().setUpdatingTrackArray(trackArray);
        }
        this.stateMachine = stateMachine;
    }

    private void checkTrackArray() {
        if (stateMachine != null && stateMachine.getAnimationController().getUpdatingTrackArray() != trackArray) {
            throw new TrackArrayMismatchException();
        }
    }

    @Nonnull
    private AnimationStateMachine<?> checkStateMachine() {
        if (this.stateMachine == null) {
            throw new IllegalStateException("This context has not been bound to a state machine.");
        }
        return this.stateMachine;
    }
}

package com.tacz.guns.client.animation;

import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Supplier;

public class AnimationController {
    protected final ArrayList<ObjectAnimationRunner> currentRunners = new ArrayList<>();
    protected final ArrayList<Boolean> blending = new ArrayList<>();
    private final AnimationListenerSupplier listenerSupplier;
    private final ArrayList<Queue<AnimationPlan>> animationQueue = new ArrayList<>();
    protected Map<String, ObjectAnimation> prototypes = Maps.newHashMap();

    public AnimationController(List<ObjectAnimation> animationPrototypes, AnimationListenerSupplier model) {
        for (ObjectAnimation prototype : animationPrototypes) {
            if (prototype == null) {
                continue;
            }
            prototypes.put(prototype.name, prototype);
        }
        this.listenerSupplier = model;
    }

    public void providePrototypeIfAbsent(String name, Supplier<ObjectAnimation> supplier) {
        if (!prototypes.containsKey(name)) {
            prototypes.put(name, supplier.get());
        }
    }

    public boolean containPrototype(String name) {
        return prototypes.containsKey(name);
    }

    @Nullable
    public ObjectAnimationRunner getAnimation(int track) {
        if (track >= currentRunners.size()) {
            return null;
        }
        return currentRunners.get(track);
    }

    public void removeAnimation(int track) {
        if (track < currentRunners.size()) {
            currentRunners.set(track, null);
        }
        if (track < animationQueue.size()) {
            animationQueue.set(track, null);
        }
    }

    public void queueAnimation(int track, Queue<AnimationPlan> queue) {
        //ensure the capability
        for (int i = animationQueue.size(); i <= track; i++) {
            animationQueue.add(null);
        }

        animationQueue.set(track, queue);

        if (queue != null) {
            AnimationPlan plan = null;
            while (plan == null && !queue.isEmpty()) {
                plan = queue.poll();
            }
            if (plan != null) {
                run(track, plan.animationName, plan.playType, plan.transitionTimeS);
            }
        }
    }

    public void runAnimation(int track, String animationName, ObjectAnimation.PlayType playType, float transitionTimeS) {
        //运行单个动画的时候视为执行一个只有一个动画的动画队列，因此需要清理旧的队列。
        if (track < animationQueue.size()) {
            animationQueue.set(track, null);
        }
        run(track, animationName, playType, transitionTimeS);
    }

    synchronized private void run(int track, String animationName, ObjectAnimation.PlayType playType, float transitionTimeS) {
        ObjectAnimation prototype = prototypes.get(animationName);
        if (prototype == null) {
            return;
        }
        //ensure the capability
        for (int i = currentRunners.size(); i <= track; i++) {
            currentRunners.add(null);
        }

        ObjectAnimation animation = new ObjectAnimation(prototype);
        animation.applyAnimationListeners(listenerSupplier);
        animation.playType = playType;
        ObjectAnimationRunner runner = new ObjectAnimationRunner(animation);
        runner.setProgressNs(0);
        runner.run();

        ObjectAnimationRunner oldRunner = currentRunners.get(track);
        if (transitionTimeS > 0) {
            if (oldRunner != null) {
                oldRunner.transition(runner, (long) (transitionTimeS * 1e9));
            } else {
                currentRunners.set(track, runner);
            }
        } else {
            currentRunners.set(track, runner);
        }
    }

    public void setBlending(int track, boolean blend) {
        //ensure the capability
        for (int i = blending.size(); i <= track; i++) {
            blending.add(false);
        }
        blending.set(track, blend);
    }

    synchronized public void update() {
        // 动画混合时，track 级别越低，优先级越低（体现在旋转的叠加先后顺序上）
        for (int i = currentRunners.size() - 1; i >= 0; i--) {
            boolean blend = i < blending.size() ? blending.get(i) : false;
            ObjectAnimationRunner runner = currentRunners.get(i);
            if (runner == null) {
                continue;
            }
            //更新当前动画runner
            if (runner.isRunning() || runner.isHolding() || runner.isTransitioning()) {
                runner.update(blend);
            }
            //更新过渡目标动画runner，并且如果过渡已经完成，将其塞进currentRunners
            if (runner.getTransitionTo() != null) {
                runner.getTransitionTo().update(blend);
                if (!runner.isTransitioning()) {
                    currentRunners.set(i, runner.getTransitionTo());
                    runner = runner.getTransitionTo();
                }
            }
            //如果动画结束，检查队列是否有下一个动画，有则播放
            if ((runner.isHolding() || runner.isStopped()) && !runner.isTransitioning()) {
                if (i < animationQueue.size()) {
                    Queue<AnimationPlan> queue = animationQueue.get(i);
                    if (queue != null) {
                        AnimationPlan plan = null;
                        while (plan == null && !queue.isEmpty()) {
                            plan = queue.poll();
                        }
                        if (plan != null) {
                            run(i, plan.animationName, plan.playType, plan.transitionTimeS);
                        }
                    }
                }
            }
        }
    }
}

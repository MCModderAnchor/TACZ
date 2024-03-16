package com.tac.guns.util.math;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class SecondOrderDynamics {
    public static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(15, Thread::new);

    static {
        for (int i = 0; i < 15; i++) {
            executorService.execute(() -> {
            });
        }
    }

    private final float k1;
    private final float k2;
    private final float k3;

    private float py;
    private float pyd;
    private float px;

    private float target;

    /**
     * @param f  Natural frequency
     * @param z  Damping coefficient
     * @param r  Initial velocity
     * @param x0 Initial position
     */
    public SecondOrderDynamics(float f, float z, float r, float x0) {
        k1 = (float) (z / (Math.PI * f));
        k2 = (float) (1 / ((2 * Math.PI * f) * (2 * Math.PI * f)));
        k3 = (float) (r * z / (2 * Math.PI * f));

        py = px = x0;
        pyd = 0;

        target = x0;

        executorService.execute(this::update);
    }

    /**
     * @param T abandoned.
     * @return processed y value
     */
    public float update(float x) {
        target = x;
        return py + 0.05f * pyd;
    }

    private void update() {
        while (true) {
            float t = 0.05f;
            float xd = (target - px) / t;
            float y = py + t * pyd;

            pyd = pyd + t * (px + k3 * xd - py - k1 * pyd) / k2;
            px = target;
            py = y;

            try {
                Thread.sleep(6);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

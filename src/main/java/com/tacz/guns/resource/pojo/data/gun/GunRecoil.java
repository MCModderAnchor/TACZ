package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import javax.annotation.Nullable;

public class GunRecoil {
    private static final SplineInterpolator INTERPOLATOR = new SplineInterpolator();

    @SerializedName("pitch")
    @Nullable
    private GunRecoilKeyFrame[] pitch;

    @SerializedName("yaw")
    @Nullable
    private GunRecoilKeyFrame[] yaw;

    public GunRecoilKeyFrame[] getPitch() {
        return pitch;
    }

    public void setPitch(@Nullable GunRecoilKeyFrame[] pitch) {
        this.pitch = pitch;
    }

    public GunRecoilKeyFrame[] getYaw() {
        return yaw;
    }

    public void setYaw(@Nullable GunRecoilKeyFrame[] yaw) {
        this.yaw = yaw;
    }

    /**
     * 返回经过随机取值、缩放后摄像机垂直后坐力的样条插值函数。
     *
     * @param modifier 配件对后坐力的修改
     * @return 样条插值函数
     */
    @Nullable
    public PolynomialSplineFunction genPitchSplineFunction(float modifier) {
        return getSplineFunction(pitch, modifier);
    }

    /**
     * 返回经过随机取值、缩放后摄像机水平后坐力的样条插值函数。
     *
     * @param modifier 配件对后坐力的修改
     * @return 样条插值函数
     */
    @Nullable
    public PolynomialSplineFunction genYawSplineFunction(float modifier) {
        return getSplineFunction(yaw, modifier);
    }

    private PolynomialSplineFunction getSplineFunction(GunRecoilKeyFrame[] keyFrames, float modifier) {
        if (keyFrames == null || keyFrames.length == 0) {
            return null;
        }
        double[] values = new double[keyFrames.length + 1];
        double[] times = new double[keyFrames.length + 1];
        times[0] = 0;
        values[0] = 0;
        for (int i = 0; i < keyFrames.length; i++) {
            times[i + 1] = keyFrames[i].getTime() * 1000 + 30;
        }
        for (int i = 0; i < keyFrames.length; i++) {
            float[] value = keyFrames[i].getValue();
            values[i + 1] = (value[0] + Math.random() * (value[1] - value[0])) * modifier;
        }
        return INTERPOLATOR.interpolate(times, values);
    }
}

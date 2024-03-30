package com.tac.guns.util.math;

import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.util.Mth;

public class MathUtil {
    public static double magnificationToFovMultiplier(double magnification, double currentFov) {
        return magnificationToFov(magnification, currentFov) / currentFov;
    }

    public static double magnificationToFov(double magnification, double currentFov) {
        double currentTan = Math.tan(Math.toRadians(currentFov / 2));
        double newTan = currentTan / magnification;
        return Math.toDegrees(Math.atan(newTan)) * 2;
    }

    public static double fovToMagnification(double currentFov, double originFov) {
        return Math.tan(Math.toRadians(originFov / 2)) / Math.tan(Math.toRadians(currentFov / 2));
    }

    public static float[] toQuaternion(float roll, float pitch, float yaw) {
        double cy = Math.cos(yaw * 0.5);
        double sy = Math.sin(yaw * 0.5);
        double cp = Math.cos(pitch * 0.5);
        double sp = Math.sin(pitch * 0.5);
        double cr = Math.cos(roll * 0.5);
        double sr = Math.sin(roll * 0.5);
        return new float[]{
                (float) (cy * cp * sr - sy * sp * cr),
                (float) (sy * cp * sr + cy * sp * cr),
                (float) (sy * cp * cr - cy * sp * sr),
                (float) (cy * cp * cr + sy * sp * sr)
        };
    }

    public static float[] inverseQuaternion(float[] quaternion) {
        float[] result = new float[4];
        // 求共轭
        result[0] = -quaternion[0];
        result[1] = -quaternion[1];
        result[2] = -quaternion[2];
        result[3] = quaternion[3];
        // 求模长平方，进行归一化
        float m2 = quaternion[0] * quaternion[0] + quaternion[1] * quaternion[1] + quaternion[2] * quaternion[2] + quaternion[3] * quaternion[3];
        result[0] = result[0] / m2;
        result[1] = result[1] / m2;
        result[2] = result[2] / m2;
        result[3] = result[3] / m2;
        return result;
    }

    public static void blendQuaternion(Quaternion to, Quaternion from) {
        Quaternion q1 = new Quaternion(to);
        Quaternion q2 = new Quaternion(from);
        normalizeQuaternion(q1);
        normalizeQuaternion(q2);
        logQuaternion(q1);
        logQuaternion(q2);
        q1.set(q1.i() + q2.i(), q1.j() + q2.j(), q1.k() + q2.k(), q1.r() + q2.r());
        expQuaternion(q1);
        normalizeQuaternion(q1);
        to.set(q1.i(), q1.j(), q1.k(), q1.r());
    }

    public static void normalizeQuaternion(Quaternion q) {
        float f = q.i() * q.i() + q.j() * q.j() + q.k() * q.k() + q.r() * q.r();
        if (f > 0) {
            float f1 = Mth.fastInvSqrt(f);
            q.set(f1 * q.i(), f1 * q.j(), f1 * q.k(), f1 * q.r());
        } else {
            q.set(0, 0, 0, 1);
        }
    }

    public static void logQuaternion(Quaternion q) {
        double norm = Math.sqrt(q.i() * q.i() + q.j() * q.j() + q.k() * q.k() + q.r() * q.r());
        double vec = Math.sqrt(q.i() * q.i() + q.j() * q.j() + q.k() * q.k());
        double i = q.r() / norm;
        if (i > 1) {
            i = 1;
        }
        if (i < -1) {
            i = -1;
        }
        double theta = Math.acos(i);
        double factor = vec == 0 ? 0 : theta / vec;
        q.set(
                (float) (q.i() * factor),
                (float) (q.j() * factor),
                (float) (q.k() * factor),
                (float) Math.log(norm)
        );
    }

    public static void expQuaternion(Quaternion q) {
        double magnitude = Math.sqrt(q.i() * q.i() + q.j() * q.j() + q.k() * q.k());
        double expW = Math.exp(q.r());
        double sinMagnitude = Math.sin(magnitude);
        double coef = magnitude == 0 ? 0 : expW * sinMagnitude / magnitude;
        q.set(
                (float) (coef * q.i()),
                (float) (coef * q.j()),
                (float) (coef * q.k()),
                (float) (expW * Math.cos(magnitude))
        );
    }

    public static float[] slerp(float[] from, float[] to, float alpha) {
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
        float[] result = new float[4];
        result[0] = s0 * ax + s1 * bx;
        result[1] = s0 * ay + s1 * by;
        result[2] = s0 * az + s1 * bz;
        result[3] = s0 * aw + s1 * bw;
        return result;
    }

    public static Quaternion toQuaternion(float[] q) {
        return new Quaternion(q[0], q[1], q[2], q[3]);
    }

    public static Quaternion slerp(Quaternion from, Quaternion to, float alpha) {
        float ax = from.i();
        float ay = from.j();
        float az = from.k();
        float aw = from.r();
        float bx = to.i();
        float by = to.j();
        float bz = to.k();
        float bw = to.r();

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
        return new Quaternion(rx, ry, rz, rw);
    }

    public static Vector3f getEulerAngles(Matrix4f matrix) {
        double sy = Math.sqrt(matrix.m00 * matrix.m00 + matrix.m10 * matrix.m10);
        boolean singular = sy < 1e-6;
        double x, y, z;
        if (!singular) {
            x = Math.atan2(matrix.m21, matrix.m22);
            y = Math.atan2(-matrix.m20, sy);
            z = Math.atan2(matrix.m10, matrix.m00);
        } else {
            x = Math.atan2(-matrix.m12, matrix.m11);
            y = Math.atan2(-matrix.m20, sy);
            z = 0;
        }
        return new Vector3f((float) x, (float) y, (float) z);
    }

    /**
     * 在两个变换矩阵之间旋转、位移的插值。
     *
     * @param resultMatrix 输出结果将乘进此矩阵
     */
    public static void applyMatrixLerp(Matrix4f fromMatrix, Matrix4f toMatrix, Matrix4f resultMatrix, float alpha) {
        // 计算位移的插值
        Vector3f translation = new Vector3f(toMatrix.m03 - fromMatrix.m03, toMatrix.m13 - fromMatrix.m13, toMatrix.m23 - fromMatrix.m23);
        translation.mul(alpha);
        // 计算旋转的插值
        Vector3f fromRotation = MathUtil.getEulerAngles(fromMatrix);
        float[] qFrom = MathUtil.toQuaternion(fromRotation.x(), fromRotation.y(), fromRotation.z());
        Vector3f toRotation = MathUtil.getEulerAngles(toMatrix);
        float[] qTo = MathUtil.toQuaternion(toRotation.x(), toRotation.y(), toRotation.z());
        Quaternion qLerped = MathUtil.toQuaternion(MathUtil.slerp(qFrom, qTo, alpha));
        Quaternion qInverse = MathUtil.toQuaternion(inverseQuaternion(qFrom));
        qLerped.mul(qInverse);
        // 应用位移和旋转
        resultMatrix.translate(new Vector3f(translation.x(), translation.y(), translation.z()));
        resultMatrix.multiply(qLerped);
    }
}

package com.tacz.guns.util.math;

import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;

public class MathUtil {
    public static final float[] QUATERNION_ONE = {0, 0, 0, 1};

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

    public static double zoomSensitivityRatio(double currentFov, double originFov, double coefficient) {
        return Math.atan(Math.tan(Math.toRadians(currentFov / 2)) * coefficient) /
                Math.atan(Math.tan(Math.toRadians(originFov / 2)) * coefficient);
    }

    public static double copySign(double magnitude, double sign) {
        return Math.abs(magnitude) * (sign < 0 ? -1 : 1);
    }

    /**
     * 按照 z(roll) -> y(yaw) -> x(pitch) 的旋转顺序，求四元数。
     * @param pitch 绕 x 轴旋转的弧度
     * @param yaw 绕 y 轴旋转的弧度
     * @param roll 绕 z 轴旋转的弧度
     * @return 四元数，前三个数是虚部，最后一个数是实部。
     */
    public static float[] toQuaternion(float pitch, float yaw, float roll) {
        double cy = Math.cos(roll * 0.5);
        double sy = Math.sin(roll * 0.5);
        double cp = Math.cos(yaw * 0.5);
        double sp = Math.sin(yaw * 0.5);
        double cr = Math.cos(pitch * 0.5);
        double sr = Math.sin(pitch * 0.5);
        return new float[]{
                (float) (cy * cp * sr - sy * sp * cr),
                (float) (sy * cp * sr + cy * sp * cr),
                (float) (sy * cp * cr - cy * sp * sr),
                (float) (cy * cp * cr + sy * sp * sr)
        };
    }

    /**
     * 按照 z(roll) -> y(yaw) -> x(pitch) 的旋转顺序，求四元数。
     * @param pitch 绕 x 轴旋转的弧度
     * @param yaw 绕 y 轴旋转的弧度
     * @param roll 绕 z 轴旋转的弧度
     * @param quaternion 求解的结果将写入这个四元数中。
     */
    public static void toQuaternion(float pitch, float yaw, float roll, @Nonnull Quaternion quaternion) {
        double cy = Math.cos(roll * 0.5);
        double sy = Math.sin(roll * 0.5);
        double cp = Math.cos(yaw * 0.5);
        double sp = Math.sin(yaw * 0.5);
        double cr = Math.cos(pitch * 0.5);
        double sr = Math.sin(pitch * 0.5);

        quaternion.set(
                (float) (cy * cp * sr - sy * sp * cr),
                (float) (sy * cp * sr + cy * sp * cr),
                (float) (sy * cp * cr - cy * sp * sr),
                (float) (cy * cp * cr + sy * sp * sr)
        );
    }

    /**
     * 将四元数转换为欧拉角，
     * @param q 四元数
     * @return 按照 x(pitch) -> y(yaw) -> z(roll) 的顺序的三轴角数组。
     */
    public static float[] toEulerAngles(Quaternion q) {
        float[] angles = new float[3];
        // pitch (x-axis rotation)
        double sinrCosp = 2 * (q.r() * q.i() + q.j() * q.k());
        double cosrCosp = 1 - 2 * (q.i() * q.i() + q.j() * q.j());
        angles[0] = (float) Math.atan2(sinrCosp, cosrCosp);
        // yaw (y-axis rotation)
        double sinp = 2 * (q.r() * q.j() - q.i() * q.k());
        if (Math.abs(sinp) >= 1) {
            // use 90 degrees if out of range
            angles[1] = (float) copySign(Math.PI / 2, sinp);
        } else {
            angles[1] = (float) Math.asin(sinp);
        }
        // roll (z-axis rotation)
        double sinyCosp = 2 * (q.r() * q.k() + q.j() * q.i());
        double cosyCosp = 1 - 2 * (q.j() * q.j() + q.k() * q.k());
        angles[2] = (float) Math.atan2(sinyCosp, cosyCosp);
        return angles;
    }

    /**
     * 将四元数转换为欧拉角，
     * @param q 四元数，前三个数是虚部，最后一个数是实部。
     * @return 按照 x(pitch) -> y(yaw) -> z(roll) 的顺序的三轴角数组。
     */
    public static float[] toEulerAngles(float[] q) {
        float[] angles = new float[3];
        // pitch (x-axis rotation)
        double sinrCosp = 2 * (q[3] * q[0] + q[1] * q[2]);
        double cosrCosp = 1 - 2 * (q[0] * q[0] + q[1] * q[1]);
        angles[0] = (float) Math.atan2(sinrCosp, cosrCosp);
        // yaw (y-axis rotation)
        double sinp = 2 * (q[3] * q[1] - q[2] * q[0]);
        if (Math.abs(sinp) >= 1) {
            angles[1] = (float) copySign(Math.PI / 2, sinp); // use 90 degrees if out of range
        } else {
            angles[1] = (float) Math.asin(sinp);
        }
        // roll (z-axis rotation)
        double sinyCosp = 2 * (q[3] * q[2] + q[1] * q[0]);
        double cosyCosp = 1 - 2 * (q[1] * q[1] + q[2] * q[2]);
        angles[2] = (float) Math.atan2(sinyCosp, cosyCosp);
        return angles;
    }

    /**
     * 将负旋转角(弧度)转换为等效的正角(角度)
     * @param angle 弧度
     * @return 等效正角(角度)
     */
    public static double toDegreePositive(double angle) {
        while (angle < 0) {
            angle += Math.PI * 2;
        }
        return Math.toDegrees(angle);
    }

    /**
     * 求四元数的逆
     * @param quaternion 四元数，前三个数是虚部，最后一个数是实部。
     * @return 四元数的逆
     */
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

    public static float[] solveEquations(float[][] coefficients, float[] constants) {
        int n = constants.length;
        // 高斯消元
        for (int pivot = 0; pivot < n - 1; pivot++) {
            for (int row = pivot + 1; row < n; row++) {
                float factor = coefficients[row][pivot] / coefficients[pivot][pivot];
                for (int col = pivot; col < n; col++) {
                    coefficients[row][col] -= coefficients[pivot][col] * factor;
                }
                constants[row] -= constants[pivot] * factor;
            }
        }
        // 回代求解
        float[] solution = new float[n];
        for (int i = n - 1; i >= 0; i--) {
            float sum = 0.0f;
            for (int j = i + 1; j < n; j++) {
                sum += coefficients[i][j] * solution[j];
            }
            solution[i] = (constants[i] - sum) / coefficients[i][i];
        }
        return solution;
    }

    public static float[] getRelativeQuaternion(float[] qa, float[] qb) {
        /*
        Given two quaternions A and B, find the quaternion C such that the result of A multiplied by C is equal to B.
        Solve the following equations:
             aw*ci -ak*cj +aj*ck +ai*cw = bi
             ak*ci +aw*cj -ai*ck +aj*cw = bj
            -aj*ci +ai*cj +aw*ck +ak*cw = bk
            -ai*ci -aj*cj -ak*ck +aw*cw = bw
        */
        float[][] coefficients = {
                {qa[3], -qa[2], qa[1], qa[0]},
                {qa[2], qa[3], -qa[0], qa[1]},
                {-qa[1], qa[0], qa[3], qa[2]},
                {-qa[0], -qa[1], -qa[2], qa[3]},
        };
        float[] constants = {qb[0], qb[1], qb[2], qb[3]};
        return solveEquations(coefficients, constants);
    }

    public static Quaternion getRelativeQuaternion(Quaternion qa, Quaternion qb) {
        /*
        Given two quaternions A and B, find the quaternion C such that the result of A multiplied by C is equal to B.
        Solve the following equations:
             aw*ci -ak*cj +aj*ck +ai*cw = bi
             ak*ci +aw*cj -ai*ck +aj*cw = bj
            -aj*ci +ai*cj +aw*ck +ak*cw = bk
            -ai*ci -aj*cj -ak*ck +aw*cw = bw
        */
        float[][] coefficients = {
                {qa.r(), -qa.k(), qa.j(), qa.i()},
                {qa.k(), qa.r(), -qa.i(), qa.j()},
                {-qa.j(), qa.i(), qa.r(), qa.k()},
                {-qa.i(), -qa.j(), -qa.k(), qa.r()},
        };
        float[] constants = {qb.i(), qb.j(), qb.k(), qb.r()};
        float[] result = solveEquations(coefficients, constants);
        return new Quaternion(result[0], result[1], result[2], result[3]);
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
        float[] qRelative = getRelativeQuaternion(qFrom, qTo);
        Quaternion qLerped = MathUtil.toQuaternion(MathUtil.slerp(QUATERNION_ONE, qRelative, alpha));
        // 应用位移和旋转
        resultMatrix.translate(new Vector3f(translation.x(), translation.y(), translation.z()));
        resultMatrix.multiply(qLerped);
    }

    public static Pair<Float, Vector3f> getAngleAndAxis(Quaternion quaternion) {
        double angle = 2 * Math.acos(quaternion.r());
        double sin = Math.sin(angle / 2);
        // 旋转角为 0 或者 2*PI，旋转结果与旋转轴无关
        if (sin == 0) {
            return Pair.of(0f, new Vector3f(0, 0, 0));
        }
        Vector3f axis = new Vector3f(quaternion.i(), quaternion.j(), quaternion.k());
        axis.mul((float) (1 / sin));
        return Pair.of((float) angle, axis);
    }

    public static Pair<Float, Vector3f> getAngleAndAxis(float[] quaternion) {
        double angle = 2 * Math.acos(quaternion[3]);
        double sin = Math.sin(angle / 2);
        // 旋转角为 0 或者 2*PI，旋转结果与旋转轴无关
        if (sin == 0) {
            return Pair.of(0f, new Vector3f(0, 0, 0));
        }
        Vector3f axis = new Vector3f(quaternion[0], quaternion[1], quaternion[2]);
        axis.mul((float) (1 / sin));
        return Pair.of((float) angle, axis);
    }


    public static Quaternion multiplyQuaternion(Quaternion quaternion, float multiplier) {
        Pair<Float, Vector3f> angleAndAxis = getAngleAndAxis(quaternion);
        float newAngle = angleAndAxis.getLeft() * multiplier;
        Vector3f axis = angleAndAxis.getRight();
        double sin = Math.sin(newAngle / 2);
        double cos = Math.cos(newAngle / 2);
        axis.mul((float) sin);
        return new Quaternion(axis.x(), axis.y(), axis.z(), (float) cos);
    }

    public static float[] multiplyQuaternion(float[] quaternion, float multiplier) {
        Pair<Float, Vector3f> angleAndAxis = getAngleAndAxis(quaternion);
        float newAngle = angleAndAxis.getLeft() * multiplier;
        Vector3f axis = angleAndAxis.getRight();
        double sin = Math.sin(newAngle / 2);
        double cos = Math.cos(newAngle / 2);
        axis.mul((float) sin);
        return new float[]{axis.x(), axis.y(), axis.z(), (float) cos};
    }

    public static double getTwoVecAngle(Vec3 v1, Vec3 v2) {
        double dotProduct = v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
        double magnitude1 = Math.sqrt(v1.x * v1.x + v1.y * v1.y + v1.z * v1.z);
        double magnitude2 = Math.sqrt(v2.x * v2.x + v2.y * v2.y + v2.z * v2.z);
        if (magnitude1 * magnitude2 == 0) {
            return -1;
        }
        double cos = dotProduct / (magnitude1 * magnitude2);
        return Math.acos(cos);
    }

    public static float splineCurve(float[] y, float tension, float alpha) {
        if (y.length != 4) {
            throw new IllegalArgumentException("y value length must be 4 when doing catmull-rom spline");
        }
        if (tension < 0 || tension > 1) {
            throw new IllegalArgumentException("tension must be 0~1 when doing catmull-rom spline");
        }
        float v0 = (y[2] - y[0]) * 0.5f;
        float v1 = (y[3] - y[1]) * 0.5f;
        float t2 = alpha * alpha;
        float t3 = alpha * t2;
        float h1 = 2f * t3 - 3f * t2 + 1f;
        float h2 = -2f * t3 + 3f * t2;
        float h3 = t3 - 2f * t2 + alpha;
        float h4 = t3 - t2;
        return h1 * y[1] + h2 * y[2] + h3 * v0 + h4 * v1;
    }

    public static float[] quaternionSplineCurve(float[][] quaternions, float tension, float alpha) {
        if (quaternions.length != 4) {
            throw new IllegalArgumentException("y value length must be 4 when doing catmull-rom spline");
        }
        if (tension < 0 || tension > 1) {
            throw new IllegalArgumentException("tension must be 0~1 when doing catmull-rom spline");
        }
        float[] angles0 = toEulerAngles(quaternions[0]);
        float[] angles1 = toEulerAngles(quaternions[1]);
        float[] angles2 = toEulerAngles(quaternions[2]);
        float[] angles3 = toEulerAngles(quaternions[3]);
        float[] result = new float[3];
        for (int i = 0; i < 3; i++) {
            float[] input = new float[]{angles0[i], angles1[i], angles2[i], angles3[i]};
            result[i] = splineCurve(input, tension, alpha);
        }
        return toQuaternion(result[0], result[1], result[2]);
    }
}

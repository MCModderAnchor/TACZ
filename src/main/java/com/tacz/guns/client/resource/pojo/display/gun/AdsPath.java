package com.tacz.guns.client.resource.pojo.display.gun;

/**
 * 第一人称开镜定位的分轴曲线，起终点仍由模型的观察节点决定。
 */
public class AdsPath {
    private AdsCurve x = new AdsCurve();
    private AdsCurve y = new AdsCurve();
    private AdsCurve z = new AdsCurve();
    private AdsCurve rotation = new AdsCurve();

    public float x(float progress) {
        return x.evaluate(progress);
    }

    public float y(float progress) {
        return y.evaluate(progress);
    }

    public float z(float progress) {
        return z.evaluate(progress);
    }

    public float rotation(float progress) {
        return rotation.evaluate(progress);
    }

    public boolean isValid() {
        return x != null && x.isValid() && y != null && y.isValid()
                && z != null && z.isValid() && rotation != null && rotation.isValid();
    }
}

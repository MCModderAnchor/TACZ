package com.tacz.guns.resource.pojo.data.attachment;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

public class Modifier {
    @SerializedName("addend")
    private double addend = 0;

    @SerializedName("percent")
    private double percent = 0;

    @SerializedName("multiplier")
    private double multiplier = 1;

    @Nullable
    @SerializedName("function")
    private String function = null;

    public double getAddend() {
        return addend;
    }

    public double getPercent() {
        return percent;
    }

    public double getMultiplier() {
        return multiplier;
    }

    @Nullable
    public String getFunction() {
        return function;
    }

    @Deprecated
    public void setAddend(double addend) {
        this.addend = addend;
    }

    @Deprecated
    public void setPercent(double percent) {
        this.percent = percent;
    }
}

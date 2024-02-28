package com.tac.guns.client.resource.cache.data;

import com.tac.guns.client.resource.pojo.data.GunData;
import com.tac.guns.client.resource.pojo.display.GunDisplay;

public class ClientGunIndex {
    private String name;
    private String tooltip;
    private GunDisplay display;
    private GunData data;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    public GunDisplay getDisplay() {
        return display;
    }

    public void setDisplay(GunDisplay display) {
        this.display = display;
    }

    public GunData getData() {
        return data;
    }

    public void setData(GunData data) {
        this.data = data;
    }
}

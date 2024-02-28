package com.tac.guns.client.resource.cache.data;

public class ClientGunInfo {
    private String name;
    private String tooltip;

    // todo 把枪械data也包含进来

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
}

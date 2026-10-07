package com.tacz.guns.client.renderer.item;

/** Static gun handoff; selectedLod is meaningful only after a mesh has been selected. */
public record StaticGunRenderResult(Status status, boolean selectedLod) {
    public enum Status {
        SUBMITTED, PENDING, UNSUPPORTED
    }

    public static final StaticGunRenderResult PENDING = new StaticGunRenderResult(Status.PENDING, false);
    public static final StaticGunRenderResult UNSUPPORTED = new StaticGunRenderResult(Status.UNSUPPORTED, false);

    public boolean submitted() { return status == Status.SUBMITTED; }
}

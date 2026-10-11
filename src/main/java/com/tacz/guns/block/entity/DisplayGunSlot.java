package com.tacz.guns.block.entity;

/** 由方块实体持有的稳定槽位身份；不保存渲染资源或物品副本。 */
public final class DisplayGunSlot {
    private final GunDisplayBlockEntity owner;
    private final int index;
    private long revision;
    private long appearanceRevision;
    private long notifiedRevision = Long.MIN_VALUE;

    public DisplayGunSlot(GunDisplayBlockEntity owner, int index) {
        this.owner = owner;
        this.index = index;
    }

    public GunDisplayBlockEntity owner() {
        return owner;
    }

    public int index() {
        return index;
    }

    public long appearanceRevision() {
        return appearanceRevision;
    }

    public void changed() {
        revision++;
        appearanceRevision++;
    }

    /** 槽位旋转只影响实例矩阵，不改变共享几何。 */
    public void transformed() { revision++; }

    /** BER 在登记后调用一次，把 BE 已知的变化交给渲染组。 */
    public boolean consumeChange() {
        if (notifiedRevision == revision) return false;
        notifiedRevision = revision;
        return true;
    }
}

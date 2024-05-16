package com.tacz.guns.network;

import java.util.function.IntSupplier;

/**
 * <p>Author: MrCrayfish</p>
 * <p>Open source at <a href="https://github.com/MrCrayfish/Framework">Github</a> under LGPL License.</p>
 */
public abstract class LoginIndexHolder implements IntSupplier {
    private int loginIndex;

    public int getLoginIndex() {
        return this.loginIndex;
    }

    public void setLoginIndex(final int loginIndex) {
        this.loginIndex = loginIndex;
    }

    @Override
    public int getAsInt() {
        return this.getLoginIndex();
    }
}

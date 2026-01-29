package com.tacz.guns.compat.shouldersurfing;

import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingPlugin;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingRegistrar;
import com.tacz.guns.api.item.IGun;

public class ShoulderSurfingPlugin implements IShoulderSurfingPlugin {
	@Override
	public void register(IShoulderSurfingRegistrar registrar) {
		registrar.registerAdaptiveItemCallback(
				itemStack -> {
					if (!(itemStack.getItem() instanceof IGun)) {
						return false;
					}
					return ShoulderSurfingCompatInner.isAiming();
				}
		);
	}
}

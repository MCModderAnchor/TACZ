package com.tacz.guns.api.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@SuppressWarnings("unused")
public record LuaEntityAccessor(LivingEntity entity) {
    public void sendSystemMessage(Component message) {
        entity.sendSystemMessage(message);
    }

    public void sendActionBar(Component message) {
        if (entity instanceof Player player) {
            player.displayClientMessage(message, true);
        }
    }

    public float getHealth() {
        return entity.getHealth();
    }

    public boolean hurt(float amount) {
        return entity.hurt(entity.level().damageSources().generic(), amount);
    }

    public Component literal(String text) {
        return Component.literal(text);
    }

    public Component translatable(String key) {
        return Component.translatable(key);
    }

    public Component translatable(String key, Component... components) {
        return Component.translatable(key, (Object[]) components);
    }
}

package com.tacz.guns.api.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@SuppressWarnings("unused")
public record LuaEntityAccessor(LivingEntity entity) {
    public void sendSystemMessage(Component message) {
        entity.sendMessage(message, entity.getUUID());
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
        return entity.hurt(DamageSource.GENERIC, amount);
    }

    public Component literal(String text) {
        return new TextComponent(text);
    }

    public Component translatable(String key) {
        return new TranslatableComponent(key);
    }

    public Component translatable(String key, Component... components) {
        return new TranslatableComponent(key, (Object[]) components);
    }
}

package com.tacz.guns.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class GunRefitTrigger extends SimpleCriterionTrigger<GunRefitTrigger.TriggerInstance> {
    public static final GunRefitTrigger INSTANCE = new GunRefitTrigger();
    private static final ResourceLocation ID = new ResourceLocation("tacz", "gun_refit");

    public static void register() {
        CriteriaTriggers.register(INSTANCE);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public void trigger(ServerPlayer player, ItemStack before, ItemStack after) {
        if (!ItemStack.matches(before, after)) {
            this.trigger(player, instance -> true);
        }
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        return new TriggerInstance(player);
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        public TriggerInstance(ContextAwarePredicate player) {
            super(ID, player);
        }
    }
}

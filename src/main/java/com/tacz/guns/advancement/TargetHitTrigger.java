package com.tacz.guns.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

public class TargetHitTrigger extends SimpleCriterionTrigger<TargetHitTrigger.TriggerInstance> {
    public static final TargetHitTrigger INSTANCE = new TargetHitTrigger();
    static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("tacz", "target_hit");

    public static void register() {
        CriteriaTriggers.register(INSTANCE);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public void trigger(ServerPlayer player, double distance) {
        this.trigger(player, instance -> instance.matches(distance));
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        MinMaxBounds.Doubles distance = MinMaxBounds.Doubles.fromJson(json.get("distance"));
        return new TriggerInstance(player, distance);
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final MinMaxBounds.Doubles distance;

        public TriggerInstance(ContextAwarePredicate player, MinMaxBounds.Doubles distance) {
            super(ID, player);
            this.distance = distance;
        }

        public boolean matches(double distance) {
            return this.distance.matches(distance);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            if (!this.distance.isAny()) {
                json.add("distance", this.distance.serializeToJson());
            }
            return json;
        }
    }
}

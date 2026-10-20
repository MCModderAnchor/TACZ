package com.tacz.guns.advancement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;

public class GunRefitTrigger extends SimpleCriterionTrigger<GunRefitTrigger.TriggerInstance> {
    public static final GunRefitTrigger INSTANCE = new GunRefitTrigger();
    @Override
    public Codec<TriggerInstance> codec() { return TriggerInstance.CODEC; }
    public void trigger(ServerPlayer player, ItemStack before, ItemStack after) {
        if (!ItemStack.matches(before, after)) this.trigger(player, instance -> true);
    }
    public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player)).apply(instance, TriggerInstance::new));
    }
}

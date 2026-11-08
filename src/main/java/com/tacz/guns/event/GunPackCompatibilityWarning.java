package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.resource.GunPackLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = GunMod.MOD_ID)
public final class GunPackCompatibilityWarning {
    public static final String GUIDE_COMMAND = "tacz_conversion_guide";

    private GunPackCompatibilityWarning() { }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        warn(event.getEntity());
    }

    public static void warn(Player player) {
        var packs = GunPackLoader.INSTANCE.getPacksNeedingConversion();
        if (packs.isEmpty()) return;

        Component guide = Component.translatable("message.tacz.pack_compatibility.guide")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + GUIDE_COMMAND))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("message.tacz.pack_compatibility.guide.hover"))));
        String packList = "  - " + String.join("\n  - ", packs);
        player.sendSystemMessage(Component.translatable("message.tacz.pre")
                .append(Component.translatable("message.tacz.pack_compatibility.warning",
                        SharedConstants.getCurrentVersion().getName(), guide, packList).withStyle(ChatFormatting.YELLOW)));
    }
}

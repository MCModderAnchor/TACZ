package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.command.RootCommand;
import com.tacz.guns.config.common.OtherConfig;
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
    public static final String WARNING_KEY = "message.tacz.pack_compatibility.warning";
    public static final String COMMAND_GROUP = "pack_compatibility";
    public static final String GUIDE_COMMAND = "guide";
    public static final String DISMISS_COMMAND = "dismiss";
    private static final String COMMAND_PREFIX = "/" + RootCommand.ROOT_NAME + " " + COMMAND_GROUP + " ";

    private GunPackCompatibilityWarning() { }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        warn(event.getEntity());
    }

    public static void warn(Player player) {
        if (OtherConfig.HIDE_GUN_PACK_COMPATIBILITY_WARNING.get()) return;

        var packs = GunPackLoader.INSTANCE.getPacksNeedingConversion();
        if (packs.isEmpty()) return;

        Component guide = Component.translatable("message.tacz.pack_compatibility.guide")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, COMMAND_PREFIX + GUIDE_COMMAND))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("message.tacz.pack_compatibility.guide.hover"))));
        Component dismiss = Component.translatable("message.tacz.pack_compatibility.dismiss")
                .withStyle(style -> style.withColor(ChatFormatting.GRAY).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, COMMAND_PREFIX + DISMISS_COMMAND)));
        Component actions = Component.empty().append(guide).append(" ").append(dismiss);
        String packList = "  - " + String.join("\n  - ", packs);
        player.sendSystemMessage(Component.translatable("message.tacz.pre")
                .append(Component.translatable(WARNING_KEY,
                        SharedConstants.getCurrentVersion().getName(), actions, packList).withStyle(ChatFormatting.YELLOW)));
    }
}

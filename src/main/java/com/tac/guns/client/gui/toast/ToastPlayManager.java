package com.tac.guns.client.gui.toast;

import com.tac.guns.item.level.GunLevelManager;
import com.tac.guns.network.message.ServerMessageLevelUp;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ToastPlayManager {
    public static void levelUpMessage(ServerMessageLevelUp message) {
        int level = message.getLevel();
        ItemStack gun = message.getGun();
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (GunLevelManager.DAMAGE_UP_LEVELS.contains(level)) {
            Minecraft.getInstance().getToasts().addToast(new GunLevelUpToast(gun,
                    new TranslatableComponent("toast.tac.level_up"),
                    new TranslatableComponent("toast.tac.sub.damage_up")));
        } else if (level >= GunLevelManager.MAX_LEVEL) {
            Minecraft.getInstance().getToasts().addToast(new GunLevelUpToast(gun,
                    new TranslatableComponent("toast.tac.level_up"),
                    new TranslatableComponent("toast.tac.sub.final_level")));
        } else {
            Minecraft.getInstance().getToasts().addToast(new GunLevelUpToast(gun,
                    new TranslatableComponent("toast.tac.level_up"),
                    new TranslatableComponent("toast.tac.sub.level_up")));
        }
    }
}

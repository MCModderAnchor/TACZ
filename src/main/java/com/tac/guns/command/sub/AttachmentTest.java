package com.tac.guns.command.sub;

import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.api.item.IGun;
import com.tac.guns.item.builder.AttachmentItemBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class AttachmentTest {
    public static void testAttachment(ServerPlayer player){
        ItemStack itemStack = player.getMainHandItem();
        if(itemStack.getItem() instanceof IGun iGun){
            iGun.setAttachment(itemStack, AttachmentType.SCOPE, AttachmentItemBuilder.create().build());
        }
    }
}

package com.tacz.guns.client.resource_legacy.loader.index;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.resource.pojo.AttachmentIndexPOJO;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import static com.tacz.guns.client.resource_legacy.ClientGunPackLoader.ATTACHMENT_INDEX;

public final class ClientAttachmentIndexLoader {
    private static final Marker MARKER = MarkerManager.getMarker("ClientAttachmentIndexLoader");

    public static void loadAttachmentIndex() {
        TimelessAPI.getAllCommonAttachmentIndex().forEach(index -> {
            ResourceLocation id = index.getKey();
            AttachmentIndexPOJO pojo = index.getValue().getPojo();
            try {
                ATTACHMENT_INDEX.put(id, ClientAttachmentIndex.getInstance(id, pojo));
            } catch (IllegalArgumentException exception) {
                GunMod.LOGGER.warn(MARKER, "{} index file read fail!", id);
                exception.printStackTrace();
            }
        });
    }
}

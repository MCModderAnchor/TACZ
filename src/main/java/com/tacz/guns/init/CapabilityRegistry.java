package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.entity.sync.core.DataHolder;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class CapabilityRegistry {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, GunMod.MOD_ID);

    public static final Supplier<AttachmentType<DataHolder>> DATA_HOLDER = ATTACHMENT_TYPES.register(
        "synced_entity_data",
        () -> AttachmentType.builder(DataHolder::new).build()
    );
}

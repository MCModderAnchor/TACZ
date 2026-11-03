package com.tacz.guns.compat.jei.entry;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/** A single registration's shared stacks and compatibility results for both query directions. */
public record AttachmentQueryData(List<AttachmentQueryEntry> attachmentQueries,
                                  List<GunAttachmentQueryEntry> gunAttachmentQueries,
                                  int gunCount, int attachmentCount, long compatibilityChecks) {
    private static final Map<String, GunTabType> GUN_TABS = Arrays.stream(GunTabType.values())
            .collect(Collectors.toUnmodifiableMap(type -> type.name().toLowerCase(Locale.US), type -> type));
    private static final Comparator<Gun> GUN_ORDER = Comparator.comparingInt(Gun::sort)
            .thenComparing(gun -> gun.id().toString());
    private static final Comparator<Attachment> ATTACHMENT_ORDER = Comparator.comparingInt(Attachment::sort)
            .thenComparing(attachment -> attachment.id().toString());

    public AttachmentQueryData {
        attachmentQueries = List.copyOf(attachmentQueries);
        gunAttachmentQueries = List.copyOf(gunAttachmentQueries);
    }

    public static AttachmentQueryData load() {
        List<Gun> guns = new ArrayList<>();
        TimelessAPI.getAllCommonGunIndex().forEach(entry -> {
            ItemStack stack = GunItemBuilder.create().setId(entry.getKey()).build();
            if (stack.getItem() instanceof IGun) {
                guns.add(new Gun(entry.getKey(), entry.getValue().getType(), entry.getValue().getSort(), stack));
            }
        });
        List<Attachment> attachments = new ArrayList<>();
        TimelessAPI.getAllCommonAttachmentIndex().forEach(entry -> {
            if (!entry.getValue().getPojo().isHidden()) {
                attachments.add(new Attachment(entry.getKey(), entry.getValue().getType(), entry.getValue().getSort(),
                        AttachmentItemBuilder.create().setId(entry.getKey()).build()));
            }
        });
        return create(guns, attachments,
                (gun, attachment) -> ((IGun) gun.stack().getItem()).allowAttachment(gun.stack(), attachment.stack()),
                (gun, type) -> ((IGun) gun.stack().getItem()).allowAttachmentType(gun.stack(), type));
    }

    static AttachmentQueryData create(List<Gun> guns, List<Attachment> attachments,
                                       BiPredicate<Gun, Attachment> compatible,
                                       BiPredicate<Gun, AttachmentType> allowsType) {
        List<Attachment> sortedAttachments = attachments.stream().sorted(ATTACHMENT_ORDER).toList();
        Set<AttachmentType> presentTypes = EnumSet.noneOf(AttachmentType.class);
        for (Attachment attachment : attachments) {
            if (attachment.type() != AttachmentType.NONE) {
                presentTypes.add(attachment.type());
            }
        }
        Map<Attachment, Map<GunTabType, List<ItemStack>>> gunsByAttachment = new IdentityHashMap<>();
        Map<Gun, Map<AttachmentType, List<ItemStack>>> attachmentsByGun = new IdentityHashMap<>();
        long checks = 0;
        for (Gun gun : guns) {
            GunTabType gunTab = GUN_TABS.get(gun.type());
            Set<AttachmentType> allowedTypes = EnumSet.noneOf(AttachmentType.class);
            for (AttachmentType type : presentTypes) {
                if (allowsType.test(gun, type)) {
                    allowedTypes.add(type);
                }
            }
            for (Attachment attachment : sortedAttachments) {
                boolean reverseEligible = allowedTypes.contains(attachment.type());
                if (gunTab == null && !reverseEligible) {
                    continue;
                }
                checks++;
                if (!compatible.test(gun, attachment)) {
                    continue;
                }
                // The attachment view historically does not require allowAttachmentType.
                if (gunTab != null) {
                    gunsByAttachment.computeIfAbsent(attachment, key -> new EnumMap<>(GunTabType.class))
                            .computeIfAbsent(gunTab, key -> new ArrayList<>()).add(gun.stack());
                }
                if (reverseEligible) {
                    attachmentsByGun.computeIfAbsent(gun, key -> new EnumMap<>(AttachmentType.class))
                            .computeIfAbsent(attachment.type(), key -> new ArrayList<>()).add(attachment.stack());
                }
            }
        }

        List<AttachmentQueryEntry> attachmentQueries = new ArrayList<>();
        // Preserve attachment iteration order and the original gun order within each tab.
        for (Attachment attachment : attachments) {
            Map<GunTabType, List<ItemStack>> groups = gunsByAttachment.get(attachment);
            if (groups != null) {
                for (GunTabType type : GunTabType.values()) {
                    List<ItemStack> matches = groups.get(type);
                    if (matches != null) {
                        attachmentQueries.add(new AttachmentQueryEntry(attachment.stack(), matches));
                    }
                }
            }
        }
        List<GunAttachmentQueryEntry> gunQueries = new ArrayList<>();
        for (Gun gun : guns.stream().sorted(GUN_ORDER).toList()) {
            Map<AttachmentType, List<ItemStack>> groups = attachmentsByGun.get(gun);
            if (groups != null) {
                for (AttachmentType type : AttachmentType.values()) {
                    List<ItemStack> matches = groups.get(type);
                    if (matches == null) {
                        continue;
                    }
                    int total = matches.size();
                    int pageSize = GunAttachmentQueryEntry.ATTACHMENTS_PER_PAGE;
                    int pages = (total + pageSize - 1) / pageSize;
                    for (int page = 0; page < pages; page++) {
                        gunQueries.add(new GunAttachmentQueryEntry(gun.stack(), type,
                                matches.subList(page * pageSize, Math.min((page + 1) * pageSize, total)),
                                total, page + 1, pages));
                    }
                }
            }
        }
        return new AttachmentQueryData(attachmentQueries, gunQueries, guns.size(), attachments.size(), checks);
    }

    record Gun(ResourceLocation id, String type, int sort, ItemStack stack) { }

    record Attachment(ResourceLocation id, AttachmentType type, int sort, ItemStack stack) { }
}

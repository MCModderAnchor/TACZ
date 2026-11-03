package com.tacz.guns.util;

import com.tacz.guns.resource.CommonAssetsManager;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class AllowAttachmentTagMatcher {
    private static final String TAG_PREFIX = "#";
    private static volatile Cache cache = new Cache();

    public record Cache(
            Map<ResourceLocation, Set<ResourceLocation>> allowAttachmentCache,
            Map<ResourceLocation, Set<ResourceLocation>> tagMatchCache
    ) {
        public Cache() {
            this(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        }
    }

    public static boolean match(ResourceLocation gunId, ResourceLocation attachmentId) {
        return cache.allowAttachmentCache().computeIfAbsent(gunId, AllowAttachmentTagMatcher::resolveAllowed).contains(attachmentId);
    }

    public static boolean match0(Pair<ResourceLocation, ResourceLocation> record) {
        return resolveAllowed(record.getLeft()).contains(record.getRight());
    }

    private static Set<ResourceLocation> resolveAllowed(ResourceLocation gunId) {
        var provider = CommonAssetsManager.get();
        return expand(provider.getAllowAttachmentTags(gunId), null, provider::getAttachmentTags);
    }

    /**
     * 匹配配件是否有指定的标签。
     * 目前内部用于独头弹特殊标签的判断，
     * 也能方便到外部（附属，整合包等）制作它们的特殊标签。
     *
     * @param tag tacz 配件标签
     * @param attachmentId 配件 id
     * @return 配件 id 是否有这个配件标签
     * @since 1.1.7
     */
    public static boolean matchTag(ResourceLocation tag, ResourceLocation attachmentId) {
        return cache.tagMatchCache().computeIfAbsent(tag, AllowAttachmentTagMatcher::resolveTag).contains(attachmentId);
    }

    public static boolean matchTag0(Pair<ResourceLocation, ResourceLocation> record) {
        return resolveTag(record.getLeft()).contains(record.getRight());
    }

    private static Set<ResourceLocation> resolveTag(ResourceLocation tag) {
        var provider = CommonAssetsManager.get();
        return expand(provider.getAttachmentTags(tag), tag, provider::getAttachmentTags);
    }

    /** Expand a complete root independently; caching partial child results is incorrect for cycles. */
    static Set<ResourceLocation> expand(Set<String> content, ResourceLocation rootTag,
                                        Function<ResourceLocation, Set<String>> getTag) {
        if (content == null || content.isEmpty()) {
            return Set.of();
        }
        Set<ResourceLocation> attachments = new HashSet<>();
        Set<ResourceLocation> visited = new HashSet<>();
        if (rootTag != null) {
            visited.add(rootTag);
        }
        var pending = new ArrayDeque<Iterator<String>>();
        pending.push(content.iterator());
        while (!pending.isEmpty()) {
            Iterator<String> entries = pending.peek();
            if (!entries.hasNext()) {
                pending.pop();
                continue;
            }
            String entry = entries.next();
            if (entry.startsWith(TAG_PREFIX)) {
                ResourceLocation tagId = ResourceLocation.tryParse(entry.substring(TAG_PREFIX.length()));
                if (tagId == null || !visited.add(tagId)) {
                    continue;
                }
                Set<String> child = getTag.apply(tagId);
                if (child != null && !child.isEmpty()) {
                    pending.push(child.iterator());
                }
            } else {
                ResourceLocation id = ResourceLocation.tryParse(entry);
                if (id != null) {
                    attachments.add(id);
                }
            }
        }
        return Set.copyOf(attachments);
    }

    public static void resetCache() {
        // In-flight computations may finish in the old cache, but cannot repopulate the new one.
        cache = new Cache();
    }
}

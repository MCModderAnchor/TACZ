package com.tacz.guns.util;

import com.tacz.guns.resource.CommonAssetsManager;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AllowAttachmentTagMatcher {
    private static final String TAG_PREFIX = "#";
    private static final Cache CACHE = new Cache();

    public record Cache(
            Map<Pair<ResourceLocation, ResourceLocation>, Boolean> allowAttachmentCache,
            Map<Pair<ResourceLocation, ResourceLocation>, Boolean> tagMatchCache
    ) {
        public Cache() {
            this(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        }
    }

    public static boolean match(ResourceLocation gunId, ResourceLocation attachmentId) {
        var key = Pair.of(gunId, attachmentId);
        return CACHE.allowAttachmentCache().computeIfAbsent(key, AllowAttachmentTagMatcher::match0);
    }

    public static boolean match0(Pair<ResourceLocation, ResourceLocation> record) {
        ResourceLocation gunId = record.getLeft();
        ResourceLocation attachmentId = record.getRight();
        Set<String> allowAttachmentTags = CommonAssetsManager.get().getAllowAttachmentTags(gunId);
        // 如果枪械对应的 allowAttachmentTags 为空，说明目前没有任何可以装的配件
        if (allowAttachmentTags == null || allowAttachmentTags.isEmpty()) {
            return false;
        }
        // 按 allow_attachments 声明顺序遍历，原有布尔匹配语义不变
        return treeSearch(allowAttachmentTags, attachmentId, new HashSet<>());
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
        var key = Pair.of(tag, attachmentId);
        return CACHE.tagMatchCache().computeIfAbsent(key, AllowAttachmentTagMatcher::matchTag0);
    }

    public static boolean matchTag0(Pair<ResourceLocation, ResourceLocation> record) {
        ResourceLocation tag = record.getLeft();
        ResourceLocation attachmentId = record.getRight();
        Set<String> tagContent = CommonAssetsManager.get().getAttachmentTags(tag);
        // 如果 tag 对应的内容集为空，说明目前没有任何内容
        if (tagContent == null || tagContent.isEmpty()) {
            return false;
        }
        Set<ResourceLocation> visited = new HashSet<>();
        visited.add(tag);
        return treeSearch(tagContent, attachmentId, visited);
    }

    private static boolean treeSearch(Set<String> tags, ResourceLocation attachmentId, Set<ResourceLocation> visited) {
        for (String tag : tags) {
            if (tag.startsWith(TAG_PREFIX)) {
                ResourceLocation tagId = ResourceLocation.tryParse(tag.substring(TAG_PREFIX.length()));
                if (tagId == null || !visited.add(tagId)) {
                    continue;
                }
                Set<String> attachmentTags = CommonAssetsManager.get().getAttachmentTags(tagId);
                if (attachmentTags != null && !attachmentTags.isEmpty() && treeSearch(attachmentTags, attachmentId, visited)) {
                    return true;
                }
            } else {
                ResourceLocation matchAttachmentId = ResourceLocation.tryParse(tag);
                if (attachmentId.equals(matchAttachmentId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void resetCache() {
        CACHE.allowAttachmentCache().clear();
        CACHE.tagMatchCache().clear();
    }
}

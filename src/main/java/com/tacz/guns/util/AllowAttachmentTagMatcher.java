package com.tacz.guns.util;

import com.tacz.guns.resource.CommonAssetsManager;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AllowAttachmentTagMatcher {
    private static final String TAG_PREFIX = "#";

    public static boolean match(ResourceLocation gunId, ResourceLocation attachmentId) {
        Set<String> allowAttachmentTags = CommonAssetsManager.get().getAllowAttachmentTags(gunId);
        // 如果枪械对应的 allowAttachmentTags 为空，说明目前没有任何可以装的配件
        if (allowAttachmentTags == null || allowAttachmentTags.isEmpty()) {
            return false;
        }
        // 开始遍历 allowAttachmentTags，寻找配件 id
        AtomicBoolean searchSignal = new AtomicBoolean(false);
        treeSearch(allowAttachmentTags, attachmentId, searchSignal);
        return searchSignal.get();
    }

    private static void treeSearch(Set<String> tags, ResourceLocation attachmentId, AtomicBoolean searchSignal) {
        // 开始遍历 tags，寻找配件 id
        for (String tag : tags) {
            // 如果是 tag，则去 attachment tag 寻找我们的东西
            if (tag.startsWith(TAG_PREFIX)) {
                ResourceLocation tagId = new ResourceLocation(tag.substring(TAG_PREFIX.length()));
                Set<String> attachmentTags = CommonAssetsManager.get().getAttachmentTags(tagId);
                // 如果检索的这个配件 tag 不为空，开始递归查找
                if (attachmentTags != null && !attachmentTags.isEmpty()) {
                    treeSearch(attachmentTags, attachmentId, searchSignal);
                }
            }
            // 如果是配件 id，直接对比
            else {
                ResourceLocation matchAttachmentId = new ResourceLocation(tag);
                if (attachmentId.equals(matchAttachmentId)) {
                    searchSignal.set(true);
                    return;
                }
            }
        }
    }
}

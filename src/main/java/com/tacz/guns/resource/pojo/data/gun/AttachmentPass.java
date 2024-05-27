package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.compress.utils.Sets;

import java.util.Collections;
import java.util.Set;

public class AttachmentPass {
    @SerializedName("tags")
    private Set<String> tags = Sets.newHashSet();

    public boolean isAllow(Set<String> attachmentTags) {
        return !Collections.disjoint(attachmentTags, this.tags);
    }
}

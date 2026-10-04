package com.tacz.guns.resource.convert.folder;

import com.tacz.guns.resource.convert.UnsafeResourceLocation;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.IOException;

public interface FileOp {
    boolean run(File baseDir, File file, UnsafeResourceLocation fileResourceLocation) throws IOException;

    default FileOp andThen(FileOp op) {
        return new FileOp() {
            @Override
            public boolean run(File baseDir, File file, UnsafeResourceLocation fileResourceLocation) throws IOException {
                boolean self = FileOp.this.run(baseDir, file, fileResourceLocation);
                boolean after = op.run(baseDir, file, fileResourceLocation);
                return self && after;
            }

            @Override
            public String toString() {
                return FileOp.this + ", and then " + op.toString();
            }
        };
    }
}

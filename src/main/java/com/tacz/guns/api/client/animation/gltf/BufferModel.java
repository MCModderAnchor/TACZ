package com.tacz.guns.api.client.animation.gltf;

import java.nio.ByteBuffer;

public class BufferModel {
    /**
     * The URI of the buffer data
     */
    private String uri;

    /**
     * The actual data of the buffer
     */
    private ByteBuffer bufferData;

    public String getUri() {
        return uri;
    }

    /**
     * Set the URI for the buffer data
     *
     * @param uri The URI of the buffer data
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    public int getByteLength() {
        return bufferData.capacity();
    }

    public ByteBuffer getBufferData() {
        return Buffers.createSlice(bufferData);
    }

    /**
     * Set the data of this buffer
     *
     * @param bufferData The buffer data
     */
    public void setBufferData(ByteBuffer bufferData) {
        this.bufferData = bufferData;
    }
}

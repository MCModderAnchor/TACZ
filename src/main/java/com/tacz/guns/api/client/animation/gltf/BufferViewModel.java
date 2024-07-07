package com.tacz.guns.api.client.animation.gltf;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class BufferViewModel {
    /**
     * The optional target
     */
    private final Integer target;
    /**
     * The {@link BufferModel} for this model
     */
    private BufferModel bufferModel;
    /**
     * The byte offset
     */
    private int byteOffset;
    /**
     * The byte length
     */
    private int byteLength;
    /**
     * The byte stride
     */
    private Integer byteStride;
    /**
     * An optional callback that will be used to perform the
     * substitution of sparse accessor data in the
     * {@link #getBufferViewData() buffer view data}
     * when it is obtained for the first time.
     */
    private Consumer<? super ByteBuffer> sparseSubstitutionCallback;

    /**
     * Whether the sparse substitution was already applied
     */
    private boolean sparseSubstitutionApplied;

    /**
     * Creates a new instance
     *
     * @param target The optional target
     */
    public BufferViewModel(Integer target) {
        this.byteOffset = 0;
        this.byteLength = 0;
        this.target = target;
    }

    /**
     * Set the callback that will perform the substitution of sparse accessor
     * data in the {@link #getBufferViewData() buffer view data} when it is
     * obtained for the first time.
     *
     * @param sparseSubstitutionCallback The callback
     */
    public void setSparseSubstitutionCallback(
            Consumer<? super ByteBuffer> sparseSubstitutionCallback) {
        this.sparseSubstitutionCallback = sparseSubstitutionCallback;
    }

    public ByteBuffer getBufferViewData() {
        ByteBuffer bufferData = bufferModel.getBufferData();
        ByteBuffer bufferViewData =
                Buffers.createSlice(bufferData, getByteOffset(), getByteLength());
        if (sparseSubstitutionCallback != null && !sparseSubstitutionApplied) {
            sparseSubstitutionCallback.accept(bufferViewData);
            sparseSubstitutionApplied = true;
        }
        return bufferViewData;
    }

    public BufferModel getBufferModel() {
        return bufferModel;
    }

    /**
     * Set the {@link BufferModel} for this model
     *
     * @param bufferModel The {@link BufferModel}
     */
    public void setBufferModel(BufferModel bufferModel) {
        this.bufferModel = bufferModel;
    }

    public int getByteOffset() {
        return byteOffset;
    }

    /**
     * Set the byte offset of this view referring to its {@link BufferModel}
     *
     * @param byteOffset The byte offset
     */
    public void setByteOffset(int byteOffset) {
        this.byteOffset = byteOffset;
    }

    public int getByteLength() {
        return byteLength;
    }

    /**
     * Set the byte length of this buffer view
     *
     * @param byteLength The byte length
     */
    public void setByteLength(int byteLength) {
        this.byteLength = byteLength;
    }

    public Integer getByteStride() {
        return byteStride;
    }

    /**
     * Set the optional byte stride. This byte stride must be
     * non-<code>null</code> if more than one accessor refers
     * to this buffer view.
     *
     * @param byteStride The byte stride
     */
    public void setByteStride(Integer byteStride) {
        this.byteStride = byteStride;
    }

    public Integer getTarget() {
        return target;
    }
}

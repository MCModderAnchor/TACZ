package com.tacz.guns.api.client.animation.gltf;

import com.tacz.guns.api.client.animation.gltf.accessor.AccessorData;
import com.tacz.guns.api.client.animation.gltf.accessor.AccessorDatas;
import com.tacz.guns.api.client.animation.gltf.accessor.Accessors;

public class AccessorModel {
    /**
     * The component type, as a GL constant
     */
    private final int componentType;
    /**
     * The {@link ElementType} of this accessor
     */
    private final ElementType elementType;
    /**
     * The number of elements
     */
    private final int count;
    /**
     * The offset in bytes, referring to the buffer view
     */
    private int byteOffset;
    /**
     * The {@link BufferViewModel} for this model
     */
    private BufferViewModel bufferViewModel;
    /**
     * The stride between the start of one element and the next
     */
    private int byteStride;

    /**
     * The {@link AccessorData}
     */
    private AccessorData accessorData;

    /**
     * The minimum components
     */
    private Number[] max;

    /**
     * The maximum components
     */
    private Number[] min;

    /**
     * Creates a new instance
     *
     * @param componentType The component type GL constant
     * @param count         The number of elements
     * @param elementType   The element type
     */
    public AccessorModel(
            int componentType,
            int count,
            ElementType elementType) {
        this.componentType = componentType;
        this.count = count;
        this.elementType = elementType;
    }

    public BufferViewModel getBufferViewModel() {
        return bufferViewModel;
    }

    /**
     * Set the {@link BufferViewModel} for this model
     *
     * @param bufferViewModel The {@link BufferViewModel}
     */
    public void setBufferViewModel(BufferViewModel bufferViewModel) {
        this.bufferViewModel = bufferViewModel;
    }

    public int getComponentType() {
        return componentType;
    }

    public Class<?> getComponentDataType() {
        return Accessors.getDataTypeForAccessorComponentType(
                getComponentType());
    }

    public int getComponentSizeInBytes() {
        return Accessors.getNumBytesForAccessorComponentType(componentType);
    }

    public int getElementSizeInBytes() {
        return elementType.getNumComponents() * getComponentSizeInBytes();
    }

    public int getByteOffset() {
        return byteOffset;
    }

    /**
     * Set the byte offset, referring to the {@link BufferViewModel}
     *
     * @param byteOffset The byte offset
     */
    public void setByteOffset(int byteOffset) {
        this.byteOffset = byteOffset;
    }

    public int getCount() {
        return count;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public int getByteStride() {
        return byteStride;
    }

    /**
     * Set the byte stride, indicating the number of bytes between the start
     * of one element and the start of the next element
     *
     * @param byteStride The byte stride
     */
    public void setByteStride(int byteStride) {
        this.byteStride = byteStride;
    }

    public AccessorData getAccessorData() {
        if (accessorData == null) {
            accessorData = AccessorDatas.create(this);
        }
        return accessorData;
    }

    public Number[] getMin() {
        if (min == null) {
            min = AccessorDatas.computeMin(getAccessorData());
        }
        return min.clone();
    }

    public Number[] getMax() {
        if (max == null) {
            max = AccessorDatas.computeMax(getAccessorData());
        }
        return max.clone();
    }

}

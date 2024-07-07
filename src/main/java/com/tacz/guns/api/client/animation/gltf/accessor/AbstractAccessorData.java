/*
 *
 * Copyright 2015-2016 Marco Hutter - http://www.javagl.de
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tacz.guns.api.client.animation.gltf.accessor;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Package-private abstract base implementation of an {@link AccessorData}
 */
abstract class AbstractAccessorData implements AccessorData {
    /**
     * The component type
     */
    private final Class<?> componentType;

    /**
     * The byte buffer of the buffer view that the accessor
     * refers to
     */
    private final ByteBuffer bufferViewByteBuffer;

    /**
     * The offset for the accessor inside the byte buffer of
     * the buffer view
     */
    private final int byteOffset;

    /**
     * The number of elements
     */
    private final int numElements;

    /**
     * The number of components per element
     */
    private final int numComponentsPerElement;

    /**
     * The number of bytes per component
     */
    private final int numBytesPerComponent;

    /**
     * The stride, in number of bytes, between two consecutive elements
     */
    private final int byteStridePerElement;

    /**
     * Default constructor
     *
     * @param componentType           The component type
     * @param bufferViewByteBuffer    The byte buffer of the buffer view
     * @param byteOffset              The byte offset in the buffer view
     * @param numElements             The number of elements
     * @param numComponentsPerElement The number of components per element
     * @param numBytesPerComponent    The number of bytes per component
     * @param byteStride              The byte stride between two elements. If this
     *                                is <code>null</code> or <code>0</code>, then the stride will
     *                                be the size of one element.
     * @throws NullPointerException If the bufferViewByteBuffer is
     *                              <code>null</code>
     */
    AbstractAccessorData(Class<?> componentType,
                         ByteBuffer bufferViewByteBuffer, int byteOffset,
                         int numElements, int numComponentsPerElement,
                         int numBytesPerComponent, Integer byteStride) {
        Objects.requireNonNull(bufferViewByteBuffer,
                "The bufferViewByteBuffer is null");

        this.componentType = componentType;
        this.bufferViewByteBuffer = bufferViewByteBuffer;
        this.byteOffset = byteOffset;
        this.numElements = numElements;
        this.numComponentsPerElement = numComponentsPerElement;
        this.numBytesPerComponent = numBytesPerComponent;
        if (byteStride == null || byteStride == 0) {
            this.byteStridePerElement =
                    numComponentsPerElement * numBytesPerComponent;
        } else {
            this.byteStridePerElement = byteStride;
        }
    }

    @Override
    public final Class<?> getComponentType() {
        return componentType;
    }

    @Override
    public final int getNumElements() {
        return numElements;
    }

    @Override
    public final int getNumComponentsPerElement() {
        return numComponentsPerElement;
    }

    @Override
    public final int getTotalNumComponents() {
        return numElements * numComponentsPerElement;
    }

    /**
     * Returns the index of the byte in the byte buffer where the specified
     * component starts
     *
     * @param elementIndex   The element index
     * @param componentIndex The component index
     * @return The byte index
     */
    protected final int getByteIndex(int elementIndex, int componentIndex) {
        return byteOffset + elementIndex * byteStridePerElement + componentIndex * numBytesPerComponent;
    }


    /**
     * Returns the underlying byte buffer
     *
     * @return The byte buffer
     */
    protected final ByteBuffer getBufferViewByteBuffer() {
        return bufferViewByteBuffer;
    }

    /**
     * Returns the byte stride per element
     *
     * @return The byte stride
     */
    protected final int getByteStridePerElement() {
        return byteStridePerElement;
    }

    /**
     * Returns the number of bytes per component
     *
     * @return The number of bytes per component
     */
    protected final int getNumBytesPerComponent() {
        return numBytesPerComponent;
    }

}
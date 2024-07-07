/*
 * www.javagl.de - JglTF
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
package com.tacz.guns.api.client.animation.gltf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utility methods related to buffers
 */
public class Buffers {
    /**
     * Private constructor to prevent instantiation
     */
    private Buffers() {
        // Private constructor to prevent instantiation
    }

    /**
     * Create a slice of the given byte buffer, using its current position
     * and limit. The returned slice will have the same byte order as the
     * given buffer. If the given buffer is <code>null</code>, then
     * <code>null</code> will be returned.
     *
     * @param byteBuffer The byte buffer
     * @return The slice
     */
    public static ByteBuffer createSlice(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }
        return byteBuffer.slice().order(byteBuffer.order());
    }

    /**
     * Create a slice of the given byte buffer, in the specified range.
     * The returned buffer will have the same byte order as the given
     * buffer. If the given buffer is <code>null</code>, then
     * <code>null</code> will be returned.
     *
     * @param byteBuffer The byte buffer
     * @param position   The position where the slice should start
     * @param length     The length of the slice
     * @return The slice
     * @throws IllegalArgumentException If the range that is specified
     *                                  by the position and length are not valid for the given buffer
     */
    public static ByteBuffer createSlice(
            ByteBuffer byteBuffer, int position, int length) {
        if (byteBuffer == null) {
            return null;
        }
        int oldPosition = byteBuffer.position();
        int oldLimit = byteBuffer.limit();
        try {
            int newLimit = position + length;
            if (newLimit > byteBuffer.capacity()) {
                throw new IllegalArgumentException(
                        "The new limit is " + newLimit + ", but the capacity is "
                                + byteBuffer.capacity());
            }
            byteBuffer.limit(newLimit);
            byteBuffer.position(position);
            ByteBuffer slice = byteBuffer.slice();
            slice.order(byteBuffer.order());
            return slice;
        } finally {
            byteBuffer.limit(oldLimit);
            byteBuffer.position(oldPosition);
        }
    }

    /**
     * Creates a new, direct byte buffer that contains the given data,
     * with little-endian byte order
     *
     * @param data The data
     * @return The byte buffer
     */
    public static ByteBuffer create(byte data[]) {
        return create(data, 0, data.length);
    }

    /**
     * Creates a new, direct byte buffer that contains the specified range
     * of the given data, with little-endian byte order
     *
     * @param data   The data
     * @param offset The offset in the data array
     * @param length The length of the range
     * @return The byte buffer
     */
    public static ByteBuffer create(byte data[], int offset, int length) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.put(data, offset, length);
        byteBuffer.position(0);
        return byteBuffer;
    }

    /**
     * Create a new direct byte buffer with the given size, and little-endian
     * byte order.
     *
     * @param size The size of the buffer
     * @return The byte buffer
     * @throws IllegalArgumentException If the given size is negative
     */
    public static ByteBuffer create(int size) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(size);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        return byteBuffer;
    }
}

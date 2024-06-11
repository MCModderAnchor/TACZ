package com.tacz.guns.client.resource.pojo.animation.gltf;

public class AccessorSparseIndices {
    /**
     * The index of the buffer view with sparse indices. The referenced
     * buffer view **MUST NOT** have its `target` or `byteStride` properties
     * defined. The buffer view and the optional `byteOffset` **MUST** be
     * aligned to the `componentType` byte length. (required)
     */
    private Integer bufferView;
    /**
     * The offset relative to the start of the buffer view in bytes.
     * (optional)<br>
     * Default: 0<br>
     * Minimum: 0 (inclusive)
     */
    private Integer byteOffset;
    /**
     * The indices data type. (required)<br>
     * Valid values: [5121, 5123, 5125]
     */
    private Integer componentType;

    /**
     * The index of the buffer view with sparse indices. The referenced
     * buffer view **MUST NOT** have its `target` or `byteStride` properties
     * defined. The buffer view and the optional `byteOffset` **MUST** be
     * aligned to the `componentType` byte length. (required)
     *
     * @return The bufferView
     */
    public Integer getBufferView() {
        return this.bufferView;
    }

    /**
     * The index of the buffer view with sparse indices. The referenced
     * buffer view **MUST NOT** have its `target` or `byteStride` properties
     * defined. The buffer view and the optional `byteOffset` **MUST** be
     * aligned to the `componentType` byte length. (required)
     *
     * @param bufferView The bufferView to set
     * @throws NullPointerException If the given value is <code>null</code>
     */
    public void setBufferView(Integer bufferView) {
        if (bufferView == null) {
            throw new NullPointerException((("Invalid value for bufferView: " + bufferView) + ", may not be null"));
        }
        this.bufferView = bufferView;
    }

    /**
     * The offset relative to the start of the buffer view in bytes.
     * (optional)<br>
     * Default: 0<br>
     * Minimum: 0 (inclusive)
     *
     * @return The byteOffset
     */
    public Integer getByteOffset() {
        return this.byteOffset;
    }

    /**
     * The offset relative to the start of the buffer view in bytes.
     * (optional)<br>
     * Default: 0<br>
     * Minimum: 0 (inclusive)
     *
     * @param byteOffset The byteOffset to set
     * @throws IllegalArgumentException If the given value does not meet
     *                                  the given constraints
     */
    public void setByteOffset(Integer byteOffset) {
        if (byteOffset == null) {
            this.byteOffset = byteOffset;
            return;
        }
        if (byteOffset < 0) {
            throw new IllegalArgumentException("byteOffset < 0");
        }
        this.byteOffset = byteOffset;
    }

    /**
     * Returns the default value of the byteOffset<br>
     *
     * @return The default byteOffset
     * @see #getByteOffset
     */
    public Integer defaultByteOffset() {
        return 0;
    }

    /**
     * The indices data type. (required)<br>
     * Valid values: [5121, 5123, 5125]
     *
     * @return The componentType
     */
    public Integer getComponentType() {
        return this.componentType;
    }

    /**
     * The indices data type. (required)<br>
     * Valid values: [5121, 5123, 5125]
     *
     * @param componentType The componentType to set
     * @throws NullPointerException     If the given value is <code>null</code>
     * @throws IllegalArgumentException If the given value does not meet
     *                                  the given constraints
     */
    public void setComponentType(Integer componentType) {
        if (componentType == null) {
            throw new NullPointerException((("Invalid value for componentType: " + componentType) + ", may not be null"));
        }
        if (((componentType != 5121) && (componentType != 5123)) && (componentType != 5125)) {
            throw new IllegalArgumentException((("Invalid value for componentType: " + componentType) + ", valid: [5121, 5123, 5125]"));
        }
        this.componentType = componentType;
    }
}

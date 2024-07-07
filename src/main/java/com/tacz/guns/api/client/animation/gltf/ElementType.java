package com.tacz.guns.api.client.animation.gltf;

public enum ElementType {
    /**
     * The scalar type
     */
    SCALAR(1),

    /**
     * The 2D vector type
     */
    VEC2(2),

    /**
     * The 3D vector type
     */
    VEC3(3),

    /**
     * The 4D vector type
     */
    VEC4(4),

    /**
     * The 2x2 matrix type
     */
    MAT2(4),

    /**
     * The 3x3 matrix type
     */
    MAT3(9),

    /**
     * The 4x4 matrix type
     */
    MAT4(16);

    /**
     * The number of components that one element consists of
     */
    private final int numComponents;

    /**
     * Creates a new instance with the given number of components
     *
     * @param numComponents The number of components
     */
    ElementType(int numComponents) {
        this.numComponents = numComponents;
    }

    /**
     * Returns whether the given string is a valid element type name, and may be
     * passed to <code>ElementType.valueOf</code> without causing an exception.
     *
     * @param s The string
     * @return Whether the given string is a valid element type
     */
    public static boolean contains(String s) {
        for (ElementType elementType : values()) {
            if (elementType.name().equals(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the element type for the given string. If the string is
     * <code>null</code> or does not describe a valid element type,
     * then <code>null</code> is returned
     *
     * @param string The string
     * @return The element type
     */
    public static ElementType forString(String string) {
        if (string == null) {
            return null;
        }
        if (!contains(string)) {
            return null;
        }
        return ElementType.valueOf(string);
    }

    /**
     * Returns the number of components that one element consists of
     *
     * @return The number of components
     */
    public int getNumComponents() {
        return numComponents;
    }
}

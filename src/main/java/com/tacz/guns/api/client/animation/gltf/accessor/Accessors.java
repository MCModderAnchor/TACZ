package com.tacz.guns.api.client.animation.gltf.accessor;

import com.tacz.guns.api.client.animation.gltf.GltfConstants;

public class Accessors {
    /**
     * Private constructor to prevent instantiation
     */
    private Accessors() {
        // Private constructor to prevent instantiation
    }

    /**
     * Returns the number of components that one element has for the given
     * accessor type. Valid parameters are
     * <pre><code>
     * "SCALAR" :  1
     * "VEC2"   :  2
     * "VEC3"   :  3
     * "VEC4"   :  4
     * "MAT2"   :  4
     * "MAT3"   :  9
     * "MAT4"   : 16
     * </code></pre>
     *
     * @param accessorType The accessor type.
     * @return The number of components
     * @throws IllegalArgumentException If the given type is none of the
     *                                  valid parameters
     */
    public static int getNumComponentsForAccessorType(String accessorType) {
        switch (accessorType) {
            case "SCALAR":
                return 1;
            case "VEC2":
                return 2;
            case "VEC3":
                return 3;
            case "VEC4":
                return 4;
            case "MAT2":
                return 4;
            case "MAT3":
                return 9;
            case "MAT4":
                return 16;
            default:
                break;
        }
        throw new IllegalArgumentException(
                "Invalid accessor type: " + accessorType);
    }

    /**
     * Returns the number of bytes that one component with the given
     * accessor component type consists of.
     * Valid parameters are
     * <pre><code>
     * GL_BYTE           : 1
     * GL_UNSIGNED_BYTE  : 1
     * GL_SHORT          : 2
     * GL_UNSIGNED_SHORT : 2
     * GL_INT            : 4
     * GL_UNSIGNED_INT   : 4
     * GL_FLOAT          : 4
     * </code></pre>
     *
     * @param componentType The component type
     * @return The number of bytes
     * @throws IllegalArgumentException If the given type is none of the
     *                                  valid parameters
     */
    public static int getNumBytesForAccessorComponentType(int componentType) {
        switch (componentType) {
            case GltfConstants.GL_BYTE:
                return 1;
            case GltfConstants.GL_UNSIGNED_BYTE:
                return 1;
            case GltfConstants.GL_SHORT:
                return 2;
            case GltfConstants.GL_UNSIGNED_SHORT:
                return 2;
            case GltfConstants.GL_INT:
                return 4;
            case GltfConstants.GL_UNSIGNED_INT:
                return 4;
            case GltfConstants.GL_FLOAT:
                return 4;
            default:
                break;
        }
        throw new IllegalArgumentException(
                "Invalid accessor component type: " + componentType);
    }

    /**
     * Returns the data type for the given accessor component type.
     * Valid parameters and their return values are
     * <pre><code>
     * GL_BYTE           : byte.class
     * GL_UNSIGNED_BYTE  : byte.class
     * GL_SHORT          : short.class
     * GL_UNSIGNED_SHORT : short.class
     * GL_INT            : int.class
     * GL_UNSIGNED_INT   : int.class
     * GL_FLOAT          : float.class
     * </code></pre>
     *
     * @param componentType The component type
     * @return The data type
     * @throws IllegalArgumentException If the given type is none of the
     *                                  valid parameters
     */
    public static Class<?> getDataTypeForAccessorComponentType(
            int componentType) {
        switch (componentType) {
            case GltfConstants.GL_BYTE:
                return byte.class;
            case GltfConstants.GL_UNSIGNED_BYTE:
                return byte.class;
            case GltfConstants.GL_SHORT:
                return short.class;
            case GltfConstants.GL_UNSIGNED_SHORT:
                return short.class;
            case GltfConstants.GL_INT:
                return int.class;
            case GltfConstants.GL_UNSIGNED_INT:
                return int.class;
            case GltfConstants.GL_FLOAT:
                return float.class;
            default:
                break;
        }
        throw new IllegalArgumentException(
                "Invalid accessor component type: " + componentType);
    }
}

package com.tacz.guns.api.client.animation.gltf.accessor;

import java.util.logging.Logger;

public class AccessorSparseUtils {
    /**
     * The logger used in this class
     */
    private static final Logger logger =
            Logger.getLogger(AccessorSparseUtils.class.getName());

    /**
     * Private constructor to prevent instantiation
     */
    private AccessorSparseUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Extract indices from the given {@link AccessorData}. The given
     * {@link AccessorData} must contain an integral type. That is,
     * its {@link AccessorData#getComponentType() component type} must
     * be <code>byte.class</code>, <code>short.class</code> or
     * <code>int.class</code>.
     *
     * @param accessorData The {@link AccessorData}
     * @return The indices
     * @throws IllegalArgumentException If the given data does not contain
     *                                  an integral type
     */
    private static int[] extractIndices(AccessorData accessorData) {
        if (accessorData.getComponentType() == byte.class) {
            AccessorByteData accessorByteData =
                    (AccessorByteData) accessorData;
            int numElements = accessorByteData.getNumElements();
            int indices[] = new int[numElements];
            for (int i = 0; i < numElements; i++) {
                indices[i] = accessorByteData.getInt(i, 0);
            }
            return indices;
        }
        if (accessorData.getComponentType() == short.class) {
            AccessorShortData accessorShortData =
                    (AccessorShortData) accessorData;
            int numElements = accessorShortData.getNumElements();
            int indices[] = new int[numElements];
            for (int i = 0; i < numElements; i++) {
                indices[i] = accessorShortData.getInt(i, 0);
            }
            return indices;
        }
        if (accessorData.getComponentType() == int.class) {
            AccessorIntData accessorIntData =
                    (AccessorIntData) accessorData;
            int numElements = accessorIntData.getNumElements();
            int indices[] = new int[numElements];
            for (int i = 0; i < numElements; i++) {
                indices[i] = accessorIntData.get(i, 0);
            }
            return indices;
        }
        throw new IllegalArgumentException(
                "Invalid type for indices: " + accessorData.getComponentType());
    }

    /**
     * Substitute the data in the given dense {@link AccessorData} with the
     * (sparse) data that is provided by the given {@link AccessorData}
     * objects. <br>
     * <br>
     * The <code>baseAccessorData</code> is the data that the dense data
     * will be initialized with, <b>before</b> applying the substitution
     * that is defined by the given sparse indices and values.<br>
     * <br>
     * The <code>sparseIndicesAccessorData</code> is an {@link AccessorData}
     * that was created from the <code>accessor.sparse.indices</code>
     * structure.<br>
     * <br>
     * The <code>sparseValuesAccessorData</code> is an {@link AccessorData}
     * that was created from the <code>accessor.sparse.values</code>
     * structure.<br>
     * <br>
     * This method does very few sanity checks. The caller is responsible
     * for calling it only with arguments that are valid (in terms of
     * indices and data types).
     *
     * @param denseAccessorData         The dense {@link AccessorData} to be filled
     * @param baseAccessorData          The optional "base" {@link AccessorData}
     * @param sparseIndicesAccessorData The sparse indices {@link AccessorData}
     * @param sparseValuesAccessorData  The sparse values {@link AccessorData}
     * @throws IllegalArgumentException If the sparseIndicesAccessorData does
     *                                  not contain data with an integral type (byte, short, int).
     */
    public static void substituteAccessorData(
            AccessorData denseAccessorData,
            AccessorData baseAccessorData,
            AccessorData sparseIndicesAccessorData,
            AccessorData sparseValuesAccessorData) {
        Class<?> componentType = denseAccessorData.getComponentType();
        if (componentType == byte.class) {
            AccessorByteData sparseValuesAccessorByteData =
                    (AccessorByteData) sparseValuesAccessorData;
            AccessorByteData baseAccessorByteData =
                    (AccessorByteData) baseAccessorData;
            AccessorByteData denseAccessorByteData =
                    (AccessorByteData) denseAccessorData;
            substituteByteAccessorData(
                    denseAccessorByteData,
                    baseAccessorByteData,
                    sparseIndicesAccessorData,
                    sparseValuesAccessorByteData);
        } else if (componentType == short.class) {
            AccessorShortData sparseValuesAccessorShortData =
                    (AccessorShortData) sparseValuesAccessorData;
            AccessorShortData baseAccessorShortData =
                    (AccessorShortData) baseAccessorData;
            AccessorShortData denseAccessorShortData =
                    (AccessorShortData) denseAccessorData;
            substituteShortAccessorData(
                    denseAccessorShortData,
                    baseAccessorShortData,
                    sparseIndicesAccessorData,
                    sparseValuesAccessorShortData);
        } else if (componentType == int.class) {
            AccessorIntData sparseValuesAccessorIntData =
                    (AccessorIntData) sparseValuesAccessorData;
            AccessorIntData baseAccessorIntData =
                    (AccessorIntData) baseAccessorData;
            AccessorIntData denseAccessorIntData =
                    (AccessorIntData) denseAccessorData;
            substituteIntAccessorData(
                    denseAccessorIntData,
                    baseAccessorIntData,
                    sparseIndicesAccessorData,
                    sparseValuesAccessorIntData);
        } else if (componentType == float.class) {
            AccessorFloatData sparseValuesAccessorFloatData =
                    (AccessorFloatData) sparseValuesAccessorData;
            AccessorFloatData baseAccessorFloatData =
                    (AccessorFloatData) baseAccessorData;
            AccessorFloatData denseAccessorFloatData =
                    (AccessorFloatData) denseAccessorData;

            substituteFloatAccessorData(
                    denseAccessorFloatData,
                    baseAccessorFloatData,
                    sparseIndicesAccessorData,
                    sparseValuesAccessorFloatData);
        } else {
            logger.warning("Invalid component type for accessor: "
                    + componentType);
        }
    }

    /**
     * See {@link #substituteAccessorData}
     *
     * @param denseAccessorData         The dense {@link AccessorData} to be filled
     * @param baseAccessorData          The optional "base" {@link AccessorData}
     * @param sparseIndicesAccessorData The sparse indices {@link AccessorData}
     * @param sparseValuesAccessorData  The sparse values {@link AccessorData}
     * @throws IllegalArgumentException If the sparseIndicesAccessorData does
     *                                  not contain data with an integral type (byte, short, int).
     */
    private static void substituteByteAccessorData(
            AccessorByteData denseAccessorData,
            AccessorByteData baseAccessorData,
            AccessorData sparseIndicesAccessorData,
            AccessorByteData sparseValuesAccessorData) {
        int numElements = denseAccessorData.getNumElements();
        int numComponentsPerElement =
                denseAccessorData.getNumComponentsPerElement();

        if (baseAccessorData != null) {
            // Fill the dense AccessorData with the base data
            for (int e = 0; e < numElements; e++) {
                for (int c = 0; c < numComponentsPerElement; c++) {
                    byte value = baseAccessorData.get(e, c);
                    denseAccessorData.set(e, c, value);
                }
            }
        }

        // Apply the substitution based on the sparse indices and values
        int indices[] = extractIndices(sparseIndicesAccessorData);
        for (int i = 0; i < indices.length; i++) {
            int targetElementIndex = indices[i];
            for (int c = 0; c < numComponentsPerElement; c++) {
                byte substitution = sparseValuesAccessorData.get(i, c);
                denseAccessorData.set(targetElementIndex, c, substitution);
            }
        }
    }

    /**
     * See {@link #substituteAccessorData}
     *
     * @param denseAccessorData         The dense {@link AccessorData} to be filled
     * @param baseAccessorData          The optional "base" {@link AccessorData}
     * @param sparseIndicesAccessorData The sparse indices {@link AccessorData}
     * @param sparseValuesAccessorData  The sparse values {@link AccessorData}
     * @throws IllegalArgumentException If the sparseIndicesAccessorData does
     *                                  not contain data with an integral type (byte, short, int).
     */
    private static void substituteShortAccessorData(
            AccessorShortData denseAccessorData,
            AccessorShortData baseAccessorData,
            AccessorData sparseIndicesAccessorData,
            AccessorShortData sparseValuesAccessorData) {
        int numElements = denseAccessorData.getNumElements();
        int numComponentsPerElement =
                denseAccessorData.getNumComponentsPerElement();

        if (baseAccessorData != null) {
            // Fill the dense AccessorData with the base data
            for (int e = 0; e < numElements; e++) {
                for (int c = 0; c < numComponentsPerElement; c++) {
                    short value = baseAccessorData.get(e, c);
                    denseAccessorData.set(e, c, value);
                }
            }
        }

        // Apply the substitution based on the sparse indices and values
        int indices[] = extractIndices(sparseIndicesAccessorData);
        for (int i = 0; i < indices.length; i++) {
            int targetElementIndex = indices[i];
            for (int c = 0; c < numComponentsPerElement; c++) {
                short substitution = sparseValuesAccessorData.get(i, c);
                denseAccessorData.set(targetElementIndex, c, substitution);
            }
        }
    }

    /**
     * See {@link #substituteAccessorData}
     *
     * @param denseAccessorData         The dense {@link AccessorData} to be filled
     * @param baseAccessorData          The optional "base" {@link AccessorData}
     * @param sparseIndicesAccessorData The sparse indices {@link AccessorData}
     * @param sparseValuesAccessorData  The sparse values {@link AccessorData}
     * @throws IllegalArgumentException If the sparseIndicesAccessorData does
     *                                  not contain data with an integral type (byte, short, int).
     */
    private static void substituteIntAccessorData(
            AccessorIntData denseAccessorData,
            AccessorIntData baseAccessorData,
            AccessorData sparseIndicesAccessorData,
            AccessorIntData sparseValuesAccessorData) {
        int numElements = denseAccessorData.getNumElements();
        int numComponentsPerElement =
                denseAccessorData.getNumComponentsPerElement();

        if (baseAccessorData != null) {
            // Fill the dense AccessorData with the base data
            for (int e = 0; e < numElements; e++) {
                for (int c = 0; c < numComponentsPerElement; c++) {
                    int value = baseAccessorData.get(e, c);
                    denseAccessorData.set(e, c, value);
                }
            }
        }

        // Apply the substitution based on the sparse indices and values
        int indices[] = extractIndices(sparseIndicesAccessorData);
        for (int i = 0; i < indices.length; i++) {
            int targetElementIndex = indices[i];
            for (int c = 0; c < numComponentsPerElement; c++) {
                int substitution = sparseValuesAccessorData.get(i, c);
                denseAccessorData.set(targetElementIndex, c, substitution);
            }
        }
    }

    /**
     * See {@link #substituteAccessorData}
     *
     * @param denseAccessorData         The dense {@link AccessorData} to be filled
     * @param baseAccessorData          The optional "base" {@link AccessorData}
     * @param sparseIndicesAccessorData The sparse indices {@link AccessorData}
     * @param sparseValuesAccessorData  The sparse values {@link AccessorData}
     * @throws IllegalArgumentException If the sparseIndicesAccessorData does
     *                                  not contain data with an integral type (byte, short, int).
     */
    private static void substituteFloatAccessorData(
            AccessorFloatData denseAccessorData,
            AccessorFloatData baseAccessorData,
            AccessorData sparseIndicesAccessorData,
            AccessorFloatData sparseValuesAccessorData) {
        int numElements = denseAccessorData.getNumElements();
        int numComponentsPerElement =
                denseAccessorData.getNumComponentsPerElement();

        if (baseAccessorData != null) {
            // Fill the dense AccessorData with the base data
            for (int e = 0; e < numElements; e++) {
                for (int c = 0; c < numComponentsPerElement; c++) {
                    float value = baseAccessorData.get(e, c);
                    denseAccessorData.set(e, c, value);
                }
            }
        }

        // Apply the substitution based on the sparse indices and values
        int indices[] = extractIndices(sparseIndicesAccessorData);
        for (int i = 0; i < indices.length; i++) {
            int targetElementIndex = indices[i];
            for (int c = 0; c < numComponentsPerElement; c++) {
                float substitution = sparseValuesAccessorData.get(i, c);
                denseAccessorData.set(targetElementIndex, c, substitution);
            }
        }
    }
}

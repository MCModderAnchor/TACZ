package com.tacz.guns.client.resource.pojo.animation.gltf;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private String name;
    private List<Integer> children;
    /**
     * A floating-point 4x4 transformation matrix stored in column-major
     * order. (optional)<br>
     * Default:
     * [1.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,1.0]<br>
     * Number of items: 16<br>
     * Array elements:<br>
     * &nbsp;&nbsp;The elements of this array (optional)
     */
    private float[] matrix;
    /**
     * The node's unit quaternion rotation in the order (x, y, z, w), where w
     * is the scalar. (optional)<br>
     * Default: [0.0,0.0,0.0,1.0]<br>
     * Number of items: 4<br>
     * Array elements:<br>
     * &nbsp;&nbsp;The elements of this array (optional)<br>
     * &nbsp;&nbsp;Minimum: -1.0 (inclusive)<br>
     * &nbsp;&nbsp;Maximum: 1.0 (inclusive)
     */
    private float[] rotation;
    /**
     * The node's non-uniform scale, given as the scaling factors along the
     * x, y, and z axes. (optional)<br>
     * Default: [1.0,1.0,1.0]<br>
     * Number of items: 3<br>
     * Array elements:<br>
     * &nbsp;&nbsp;The elements of this array (optional)
     */
    private float[] scale;
    /**
     * The node's translation along the x, y, and z axes. (optional)<br>
     * Default: [0.0,0.0,0.0]<br>
     * Number of items: 3<br>
     * Array elements:<br>
     * &nbsp;&nbsp;The elements of this array (optional)
     */
    private float[] translation;

    /**
     * The indices of this node's children. (optional)<br>
     * Minimum number of items: 1<br>
     * Array elements:<br>
     * &nbsp;&nbsp;The elements of this array (optional)<br>
     * &nbsp;&nbsp;Minimum: 0 (inclusive)
     *
     * @return The children
     */
    public List<Integer> getChildren() {
        return this.children;
    }

    /**
     * The indices of this node's children. (optional)<br>
     * Minimum number of items: 1<br>
     * Array elements:<br>
     * &nbsp;&nbsp;The elements of this array (optional)<br>
     * &nbsp;&nbsp;Minimum: 0 (inclusive)
     *
     * @param children The children to set
     * @throws IllegalArgumentException If the given value does not meet
     *                                  the given constraints
     */
    public void setChildren(List<Integer> children) {
        if (children == null) {
            this.children = children;
            return;
        }
        if (children.size() < 1) {
            throw new IllegalArgumentException("Number of children elements is < 1");
        }
        for (Integer childrenElement : children) {
            if (childrenElement < 0) {
                throw new IllegalArgumentException("childrenElement < 0");
            }
        }
        this.children = children;
    }

    /**
     * Add the given children. The children of this instance will be replaced
     * with a list that contains all previous elements, and additionally the
     * new element.
     *
     * @param element The element
     * @throws NullPointerException If the given element is <code>null</code>
     */
    public void addChildren(Integer element) {
        if (element == null) {
            throw new NullPointerException("The element may not be null");
        }
        List<Integer> oldList = this.children;
        List<Integer> newList = new ArrayList<Integer>();
        if (oldList != null) {
            newList.addAll(oldList);
        }
        newList.add(element);
        this.children = newList;
    }

    /**
     * Remove the given children. The children of this instance will be
     * replaced with a list that contains all previous elements, except for
     * the removed one.<br>
     * If this new list would be empty, then it will be set to
     * <code>null</code>.
     *
     * @param element The element
     * @throws NullPointerException If the given element is <code>null</code>
     */
    public void removeChildren(Integer element) {
        if (element == null) {
            throw new NullPointerException("The element may not be null");
        }
        List<Integer> oldList = this.children;
        List<Integer> newList = new ArrayList<Integer>();
        if (oldList != null) {
            newList.addAll(oldList);
        }
        newList.remove(element);
        if (newList.isEmpty()) {
            this.children = null;
        } else {
            this.children = newList;
        }
    }

    /**
     * A floating-point 4x4 transformation matrix stored in column-major
     * order. (optional)<br>
     * Default:
     * [1.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,1.0]<br>
     * Number of items: 16<br>
     * Array elements:<br>
     * &nbsp;&nbsp;The elements of this array (optional)
     *
     * @return The matrix
     */
    public float[] getMatrix() {
        return this.matrix;
    }

    public void setMatrix(float[] matrix) {
        if (matrix == null) {
            this.matrix = matrix;
            return;
        }
        if (matrix.length < 16) {
            throw new IllegalArgumentException("Number of matrix elements is < 16");
        }
        if (matrix.length > 16) {
            throw new IllegalArgumentException("Number of matrix elements is > 16");
        }
        this.matrix = matrix;
    }

    /**
     * The node's unit quaternion rotation in the order (x, y, z, w), where w
     * is the scalar. (optional)<br>
     * Default: [0.0,0.0,0.0,1.0]<br>
     * Number of items: 4<br>
     * Array elements:<br>
     * &nbsp;&nbsp;The elements of this array (optional)<br>
     * &nbsp;&nbsp;Minimum: -1.0 (inclusive)<br>
     * &nbsp;&nbsp;Maximum: 1.0 (inclusive)
     *
     * @return The rotation
     */
    public float[] getRotation() {
        return this.rotation;
    }

    /**
     * The node's unit quaternion rotation in the order (x, y, z, w), where w
     * is the scalar. (optional)<br>
     * Default: [0.0,0.0,0.0,1.0]<br>
     * Number of items: 4<br>
     * Array elements:<br>
     * &nbsp;&nbsp;The elements of this array (optional)<br>
     * &nbsp;&nbsp;Minimum: -1.0 (inclusive)<br>
     * &nbsp;&nbsp;Maximum: 1.0 (inclusive)
     *
     * @param rotation The rotation to set
     * @throws IllegalArgumentException If the given value does not meet
     *                                  the given constraints
     */
    public void setRotation(float[] rotation) {
        if (rotation == null) {
            this.rotation = rotation;
            return;
        }
        if (rotation.length < 4) {
            throw new IllegalArgumentException("Number of rotation elements is < 4");
        }
        if (rotation.length > 4) {
            throw new IllegalArgumentException("Number of rotation elements is > 4");
        }
        for (float rotationElement : rotation) {
            if (rotationElement > 1.0D) {
                throw new IllegalArgumentException("rotationElement > 1.0");
            }
            if (rotationElement < -1.0D) {
                throw new IllegalArgumentException("rotationElement < -1.0");
            }
        }
        this.rotation = rotation;
    }

    /**
     * The node's non-uniform scale, given as the scaling factors along the
     * x, y, and z axes. (optional)<br>
     * Default: [1.0,1.0,1.0]<br>
     * Number of items: 3<br>
     * Array elements:<br>
     * &nbsp;&nbsp;The elements of this array (optional)
     *
     * @return The scale
     */
    public float[] getScale() {
        return this.scale;
    }

    /**
     * The node's non-uniform scale, given as the scaling factors along the
     * x, y, and z axes. (optional)<br>
     * Default: [1.0,1.0,1.0]<br>
     * Number of items: 3<br>
     * Array elements:<br>
     * &nbsp;&nbsp;The elements of this array (optional)
     *
     * @param scale The scale to set
     * @throws IllegalArgumentException If the given value does not meet
     *                                  the given constraints
     */
    public void setScale(float[] scale) {
        if (scale == null) {
            this.scale = scale;
            return;
        }
        if (scale.length < 3) {
            throw new IllegalArgumentException("Number of scale elements is < 3");
        }
        if (scale.length > 3) {
            throw new IllegalArgumentException("Number of scale elements is > 3");
        }
        this.scale = scale;
    }

    /**
     * The node's translation along the x, y, and z axes. (optional)<br>
     * Default: [0.0,0.0,0.0]<br>
     * Number of items: 3<br>
     * Array elements:<br>
     * &nbsp;&nbsp;The elements of this array (optional)
     *
     * @return The translation
     */
    public float[] getTranslation() {
        return this.translation;
    }

    /**
     * The node's translation along the x, y, and z axes. (optional)<br>
     * Default: [0.0,0.0,0.0]<br>
     * Number of items: 3<br>
     * Array elements:<br>
     * &nbsp;&nbsp;The elements of this array (optional)
     *
     * @param translation The translation to set
     * @throws IllegalArgumentException If the given value does not meet
     *                                  the given constraints
     */
    public void setTranslation(float[] translation) {
        if (translation == null) {
            this.translation = translation;
            return;
        }
        if (translation.length < 3) {
            throw new IllegalArgumentException("Number of translation elements is < 3");
        }
        if (translation.length > 3) {
            throw new IllegalArgumentException("Number of translation elements is > 3");
        }
        this.translation = translation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}

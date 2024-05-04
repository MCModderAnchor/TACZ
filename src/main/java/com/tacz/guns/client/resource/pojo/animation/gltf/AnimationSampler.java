package com.tacz.guns.client.resource.pojo.animation.gltf;

public class AnimationSampler {
    /**
     * The index of an accessor containing keyframe timestamps. (required)
     */
    private Integer input;

    /**
     * Interpolation algorithm. (optional)<br>
     * Default: "LINEAR"<br>
     * Valid values: [LINEAR, STEP, CUBICSPLINE]
     */
    private String interpolation;

    /**
     * The index of an accessor, containing keyframe output values.
     * (required)
     */
    private Integer output;

    /**
     * The index of an accessor containing keyframe timestamps. (required)
     *
     * @return The input
     */
    public Integer getInput() {
        return this.input;
    }

    /**
     * The index of an accessor containing keyframe timestamps. (required)
     *
     * @param input The input to set
     * @throws NullPointerException If the given value is <code>null</code>
     */
    public void setInput(Integer input) {
        if (input == null) {
            throw new NullPointerException((("Invalid value for input: " + input) + ", may not be null"));
        }
        this.input = input;
    }

    /**
     * Interpolation algorithm. (optional)<br>
     * Default: "LINEAR"<br>
     * Valid values: [LINEAR, STEP, CUBICSPLINE]
     *
     * @return The interpolation
     */
    public String getInterpolation() {
        return this.interpolation;
    }

    /**
     * Interpolation algorithm. (optional)<br>
     * Default: "LINEAR"<br>
     * Valid values: [LINEAR, STEP, CUBICSPLINE]
     *
     * @param interpolation The interpolation to set
     * @throws IllegalArgumentException If the given value does not meet
     *                                  the given constraints
     */
    public void setInterpolation(String interpolation) {
        if (interpolation == null) {
            this.interpolation = interpolation;
            return;
        }
        if (((!"LINEAR".equals(interpolation)) && (!"STEP".equals(interpolation))) && (!"CUBICSPLINE".equals(interpolation))) {
            throw new IllegalArgumentException((("Invalid value for interpolation: " + interpolation) + ", valid: [LINEAR, STEP, CUBICSPLINE]"));
        }
        this.interpolation = interpolation;
    }

    /**
     * Returns the default value of the interpolation<br>
     *
     * @return The default interpolation
     * @see #getInterpolation
     */
    public String defaultInterpolation() {
        return "LINEAR";
    }

    /**
     * The index of an accessor, containing keyframe output values.
     * (required)
     *
     * @return The output
     */
    public Integer getOutput() {
        return this.output;
    }

    /**
     * The index of an accessor, containing keyframe output values.
     * (required)
     *
     * @param output The output to set
     * @throws NullPointerException If the given value is <code>null</code>
     */
    public void setOutput(Integer output) {
        if (output == null) {
            throw new NullPointerException((("Invalid value for output: " + output) + ", may not be null"));
        }
        this.output = output;
    }
}

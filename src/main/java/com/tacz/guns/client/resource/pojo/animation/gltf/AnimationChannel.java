package com.tacz.guns.client.resource.pojo.animation.gltf;

public class AnimationChannel {
    /**
     * The index of a sampler in this animation used to compute the value for
     * the target. (required)
     */
    private Integer sampler;
    /**
     * The descriptor of the animated property. (required)
     */
    private AnimationChannelTarget target;

    /**
     * The index of a sampler in this animation used to compute the value for
     * the target. (required)
     *
     * @return The sampler
     */
    public Integer getSampler() {
        return this.sampler;
    }

    /**
     * The index of a sampler in this animation used to compute the value for
     * the target. (required)
     *
     * @param sampler The sampler to set
     * @throws NullPointerException If the given value is <code>null</code>
     */
    public void setSampler(Integer sampler) {
        if (sampler == null) {
            throw new NullPointerException((("Invalid value for sampler: " + sampler) + ", may not be null"));
        }
        this.sampler = sampler;
    }

    /**
     * The descriptor of the animated property. (required)
     *
     * @return The target
     */
    public AnimationChannelTarget getTarget() {
        return this.target;
    }

    /**
     * The descriptor of the animated property. (required)
     *
     * @param target The target to set
     * @throws NullPointerException If the given value is <code>null</code>
     */
    public void setTarget(AnimationChannelTarget target) {
        if (target == null) {
            throw new NullPointerException((("Invalid value for target: " + target) + ", may not be null"));
        }
        this.target = target;
    }
}

package com.tacz.guns.api.client.animation.gltf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AnimationModel {
    /**
     * The {@link Channel} instances
     * of this animation
     */
    private final List<Channel> channels = new ArrayList<>();
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Add the given {@link Channel}
     *
     * @param channel The {@link Channel}
     */
    public void addChannel(Channel channel) {
        Objects.requireNonNull(channel, "The channel may not be null");
        this.channels.add(channel);
    }

    public List<Channel> getChannels() {
        return Collections.unmodifiableList(channels);
    }

    public enum Interpolation {
        /**
         * Stepwise interpolation
         */
        STEP,

        /**
         * Linear interpolation
         */
        LINEAR,

        /**
         * Spline interpolation
         */
        SPLINE
    }


    /**
     * @param input         The input data
     * @param interpolation The interpolation method
     * @param output        The output data
     */
    public record Sampler(AccessorModel input, Interpolation interpolation, AccessorModel output) {
        /**
         * Default constructor
         *
         * @param input         The input
         * @param interpolation The interpolation
         * @param output        The output
         */
        public Sampler(
                AccessorModel input,
                Interpolation interpolation,
                AccessorModel output) {
            this.input = Objects.requireNonNull(
                    input, "The input may not be null");
            this.interpolation = Objects.requireNonNull(
                    interpolation, "The interpolation may not be null");
            this.output = Objects.requireNonNull(
                    output, "The output may not be null");
        }
    }

    /**
     * @param sampler   The sampler
     * @param nodeModel The node model
     * @param path      The path
     */
    public record Channel(Sampler sampler, NodeModel nodeModel, String path) {

        /**
         * Default constructor
         *
         * @param sampler   The sampler
         * @param nodeModel The node model
         * @param path      The path
         */
        public Channel(
                Sampler sampler,
                NodeModel nodeModel,
                String path) {
            this.sampler = Objects.requireNonNull(
                    sampler, "The sampler may not be null");
            this.nodeModel = nodeModel;
            this.path = Objects.requireNonNull(
                    path, "The path may not be null");

        }
    }
}

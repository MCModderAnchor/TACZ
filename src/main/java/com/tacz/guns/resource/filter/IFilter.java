package com.tacz.guns.resource.filter;

import java.util.List;

public interface IFilter<T> {
    boolean test(T input);

    default List<T> filter(List<T> input, boolean whitelist) {
        input.removeIf(recipeId -> whitelist != test(recipeId));
        return input;
    }
}

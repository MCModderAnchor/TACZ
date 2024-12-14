package com.tacz.guns.resource.filter;

import java.util.HashSet;
import java.util.Set;

public class LiteralFilter<T> implements IFilter<T> {
    private final Set<T> set;

    public LiteralFilter(Set<T> set) {
        this.set = set;
    }

    @Override
    public boolean test(T input) {
        return set.contains(input);
    }

    public Set<T> getSet() {
        return set;
    }

    public static class Builder<T> {
        private final Set<T> entries = new HashSet<>();

        public Builder<T> add(T item) {
            entries.add(item);
            return this;
        }

        public LiteralFilter<T> build() {
            return new LiteralFilter<>(entries);
        }

        public static <T> Builder<T> create() {
            return new Builder<>();
        }
    }
}

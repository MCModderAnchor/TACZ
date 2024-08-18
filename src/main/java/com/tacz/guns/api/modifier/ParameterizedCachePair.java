package com.tacz.guns.api.modifier;


import com.tacz.guns.resource.pojo.data.attachment.Modifier;
import it.unimi.dsi.fastutil.Pair;

import java.util.List;

/**
 * 工具类
 */
public class ParameterizedCachePair<L, R> implements Pair<ParameterizedCache<L>, ParameterizedCache<R>>{
    private final Pair<ParameterizedCache<L>, ParameterizedCache<R>> value;
    private ParameterizedCachePair(Pair<ParameterizedCache<L>, ParameterizedCache<R>> value){
        this.value = value;
    }

    @Override
    public ParameterizedCache<L> left() {
        return value.left();
    }

    @Override
    public ParameterizedCache<R> right() {
        return value.right();
    }

    public static <L, R> ParameterizedCachePair<L, R> of(L defaultLeft , R defaultRight) {
        return new ParameterizedCachePair<>(Pair.of(ParameterizedCache.of(defaultLeft), ParameterizedCache.of(defaultRight)));
    }

    public static <L, R> ParameterizedCachePair<L, R> of(List<Modifier> left, List<Modifier> right, L defaultLeft , R defaultRight) {
        return new ParameterizedCachePair<>(Pair.of(new ParameterizedCache<>(left, defaultLeft),
                new ParameterizedCache<>(right, defaultRight)));
    }
}

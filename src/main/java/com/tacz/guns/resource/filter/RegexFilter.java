package com.tacz.guns.resource.filter;

import java.util.regex.Pattern;

public class RegexFilter<T> implements IFilter<T> {
    private final Pattern pattern;

    public RegexFilter(String regex) {
        this.pattern = Pattern.compile(regex);
    }

    @Override
    public boolean test(T input) {
        return pattern.matcher(input.toString()).matches();
    }

    public Pattern getPattern() {
        return pattern;
    }
}

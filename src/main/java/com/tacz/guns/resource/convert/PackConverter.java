package com.tacz.guns.resource.convert;

import java.io.IOException;

public interface PackConverter<T> {
    boolean convert(T pack) throws IOException;
}

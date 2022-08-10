package org.rickosborne.romance.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.function.Supplier;

@UtilityClass
public class Once {
    public <T> T supply(@NonNull final Supplier<T> supplier) {
        return supplier.get();
    }
}

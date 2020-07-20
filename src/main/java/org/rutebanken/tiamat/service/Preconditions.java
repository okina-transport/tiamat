package org.rutebanken.tiamat.service;

import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Strings.lenientFormat;

/**
 * Classe statique de check de preconditions.
 */
public class Preconditions {

    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    public static void checkArgument(boolean expression, @Nullable Object errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        }
    }

    public static void checkArgument(
            boolean expression,
            @Nullable String errorMessageTemplate,
            @Nullable Object @Nullable ... errorMessageArgs) {
        if (!expression) {
            throw new IllegalArgumentException(lenientFormat(errorMessageTemplate, errorMessageArgs));
        }
    }

    public static <T> T checkNotNull(T reference) {
        return com.google.common.base.Preconditions.checkNotNull(reference);
    }

    public static <T> T checkNotNull(T reference, Object errorMessage) {
        return com.google.common.base.Preconditions.checkNotNull(reference, errorMessage);
    }

    public static <T> T checkNotNull(
            T reference, String errorMessageTemplate, Object... errorMessageArgs) {
        return com.google.common.base.Preconditions.checkNotNull(
                reference, errorMessageTemplate, errorMessageArgs);
    }

    private Preconditions() {}


}

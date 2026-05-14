package io.github.xxx.gb32960.callback.api;

public interface OutputAdapter extends Gb32960Callback {

    default String name() {
        return getClass().getSimpleName();
    }

    default void init() {
    }

    default void close() {
    }
}

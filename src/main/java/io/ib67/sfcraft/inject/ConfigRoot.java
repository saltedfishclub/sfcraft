package io.ib67.sfcraft.inject;

import jakarta.inject.Qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Provides a {@link java.nio.file.Path} to config root (usually "./sfcraft")
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigRoot {
}

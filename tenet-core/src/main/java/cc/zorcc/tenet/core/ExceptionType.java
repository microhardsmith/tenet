package cc.zorcc.tenet.core;

/**
 * The {@code ExceptionType} enum defines a set of constants that represent
 * various sources of exceptions within the framework. These exception types
 * help in categorizing and debugging the issues that arise during the
 * framework's operation.
 */
public enum ExceptionType {

    /**
     * Indicates that the exception was caused by issues related to native memory
     * or native functions. This may include errors from interacting with lower-level
     * system resources or external libraries that are not managed by the JVM.
     */
    NATIVE,

    /**
     * Indicates that the exception was caused by incorrect usage or improper
     * handling of the framework's API. This typically involves violations of
     * expected usage patterns or misuse of provided functions.
     */
    MISUSE,

    /**
     * Indicates that the exception was related to JSON parsing operations.
     * This may involve syntax errors, unexpected data formats, or issues
     * encountered during the deserialization process.
     */
    JSON,

    /**
     * Indicates that the exception was related to TOML parsing operations.
     * Similar to JSON, this may involve syntax errors, unexpected data formats,
     * or issues encountered during the deserialization process.
     */
    TOML,

    /**
     * Indicates that the exception was related to compression and decompression operations.
     * Currently, zstd, brotli, deflate and gzip compression and decompression are supported using {@link Comp}
     */
    COMPRESSION,
}

package cc.zorcc.core;

/**
 *   Framework Exception enum, list all the exception source for debugging
 */
public enum ExceptionType {
    /**
     *   Exception with native memory or native functions
     */
    NATIVE,

    /**
     *   Arithmetic overflow
     */
    ARITHMETIC,

    /**
     *   Wrong usage
     */
    MISUSE
}

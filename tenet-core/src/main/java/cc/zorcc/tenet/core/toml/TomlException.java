package cc.zorcc.tenet.core.toml;

/**
 *   Representing a toml parsing exception during deserialization process
 *   TomlException is not a checked exception, you could let it fail since configuration is corrupted, it is not forced to catch it.
 */
public final class TomlException extends RuntimeException {
    /**
     *   Constructing a TomlException with a single message
     */
    public TomlException(String message) {
        super(message);
    }
}

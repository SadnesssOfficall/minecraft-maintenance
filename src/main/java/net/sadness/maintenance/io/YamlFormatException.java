package net.sadness.maintenance.io;

public final class YamlFormatException extends Exception {

    private static final long serialVersionUID = 1L;

    public YamlFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public YamlFormatException(String message) {
        super(message);
    }
}

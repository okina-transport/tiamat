package org.rutebanken.tiamat.importer.handler;

public class ProviderException extends RuntimeException{
    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }
    public ProviderException(String message) {
        super(message);
    }
}


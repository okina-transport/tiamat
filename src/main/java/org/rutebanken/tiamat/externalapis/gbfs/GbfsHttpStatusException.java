package org.rutebanken.tiamat.externalapis.gbfs;

import java.io.IOException;
import java.net.URI;

/**
 * Raised when a GBFS server answers with an HTTP status other than 200 (ex: 503 Service Unavailable).
 */
public class GbfsHttpStatusException extends IOException {

    private final int statusCode;
    private final URI uri;

    public GbfsHttpStatusException(int statusCode, URI uri) {
        super("Got HTTP status " + statusCode + " from " + uri);
        this.statusCode = statusCode;
        this.uri = uri;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public URI getUri() {
        return uri;
    }
}

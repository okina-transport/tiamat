package org.rutebanken.tiamat.externalapis.gbfs;

import org.entur.gbfs.http.GBFSHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * {@link GBFSHttpClient} returns null when the server does not answer 200, so the HTTP status is lost.
 * This client throws a {@link GbfsHttpStatusException} giving the status instead.
 */
public class StatusAwareGbfsHttpClient extends GBFSHttpClient {

    private static final long DEFAULT_CONNECT_TIMEOUT_MS = 5000;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    @Override
    public InputStream getData(URI uri, Long timeout, Map<String, String> requestHeaderValues) throws IOException {
        long connectTimeout = timeout != null ? timeout : DEFAULT_CONNECT_TIMEOUT_MS;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .proxy(ProxySelector.getDefault())
                .build();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri).timeout(REQUEST_TIMEOUT).GET();
        if (requestHeaderValues != null) {
            requestHeaderValues.forEach(requestBuilder::header);
        }

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while requesting " + uri, e);
        }

        if (response.statusCode() != 200) {
            response.body().close();
            throw new GbfsHttpStatusException(response.statusCode(), uri);
        }
        return response.body();
    }
}

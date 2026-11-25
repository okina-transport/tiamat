package org.rutebanken.tiamat.externalapis.gbfs;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusAwareGbfsHttpClientTest {

    private HttpServer server;
    private URI baseUri;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/ok/gbfs.json", exchange -> respond(exchange, 200, "{\"version\":\"3.0\"}"));
        server.createContext("/ko/gbfs.json", exchange -> respond(exchange, 503, "{\"error\":\"Service Unavailable\"}"));
        server.start();
        baseUri = URI.create("http://localhost:" + server.getAddress().getPort());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void returnsContentWhenServerAnswers200() throws IOException {
        try (InputStream is = new StatusAwareGbfsHttpClient().getData(baseUri.resolve("/ok/gbfs.json"))) {
            assertThat(new String(is.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("{\"version\":\"3.0\"}");
        }
    }

    @Test
    void throwsHttpStatusWhenServerAnswers503() {
        URI uri = baseUri.resolve("/ko/gbfs.json");

        assertThatThrownBy(() -> new StatusAwareGbfsHttpClient().getData(uri))
                .isInstanceOfSatisfying(GbfsHttpStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(503);
                    assertThat(ex.getUri()).isEqualTo(uri);
                });
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

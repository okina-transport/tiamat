package org.rutebanken.tiamat.client.mdm;

import org.jetbrains.annotations.NotNull;
import org.rutebanken.tiamat.security.TokenService;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TokenRelayInterceptor implements ClientHttpRequestInterceptor {

    private final TokenService tokenService;

    public TokenRelayInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public @NotNull ClientHttpResponse intercept(HttpRequest request, byte @NotNull [] body, ClientHttpRequestExecution execution) throws IOException {
        String token = tokenService.getToken();
        request.getHeaders().add("Authorization", "Bearer " + token);
        return execution.execute(request, body);
    }
}

package org.rutebanken.tiamat.feign.mdm;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.rutebanken.tiamat.security.TokenService;
import org.springframework.stereotype.Component;

@Component
public class TokenRelayInterceptor implements RequestInterceptor {

    private final TokenService tokenService;

    public TokenRelayInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void apply(RequestTemplate requestTemplate) {
        String token = tokenService.getToken();
        requestTemplate.header("Authorization", "Bearer " + token);
    }
}

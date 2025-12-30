/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.auth;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static org.rutebanken.tiamat.config.JerseyConfig.ET_CLIENT_ID_HEADER;
import static org.rutebanken.tiamat.config.JerseyConfig.ET_CLIENT_NAME_HEADER;


@Configuration
@EnableWebSecurity
public class TiamatSecurityConfig {



	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/health/**").permitAll()
						.anyRequest().authenticated()
				)
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
				);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedMethods(Arrays.asList( "GET", "OPTIONS", "POST", "DELETE", "PUT"));
		configuration.setAllowedOriginPatterns(Arrays.asList("*"));
		configuration.setAllowedHeaders(Arrays.asList("Access-Control-Allow-Headers", "X-Requested-With", "Content-Type", "Authorization", "Accept", "X-Correlation-Id", "entur-pos", "x-okina-referential",
				"maxNbResults", "providerName", "RutebankenUser", ET_CLIENT_ID_HEADER ,ET_CLIENT_NAME_HEADER));
		configuration.setAllowCredentials(false);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
		return converter;
	}


}

/**
 * Class to convert roles from keycloak json
 */
class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		Map<String, Object> realmAccess = jwt.getClaim("realm_access");

		if (realmAccess == null || realmAccess.isEmpty()) {
			return new ArrayList<>();
		}

		Collection<String> roles = (Collection<String>) realmAccess.get("roles");

		return roles.stream()
				.map(roleName -> "ROLE_" + roleName)
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());
	}
}


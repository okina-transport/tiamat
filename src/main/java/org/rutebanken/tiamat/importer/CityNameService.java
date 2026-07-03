package org.rutebanken.tiamat.importer;

import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.externalapis.GouvApiReverseGeocoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Service
public class CityNameService {

    private static final Logger logger = LoggerFactory.getLogger(CityNameService.class);

    private static final String GEO_API_GOUV_COMMUNE_BY_CODE_ENDPOINT = "https://geo.api.gouv.fr/communes/{code}?fields=nom&format=json";

    private final RestTemplate restTemplate = new RestTemplate();

    @Cacheable(value = "cityNamesByInseeCode", unless = "#result == null || #result.isEmpty()")
    public Optional<String> getCityNameFromInseeCode(String inseeCode) {
        if (StringUtils.isBlank(inseeCode)) {
            return Optional.empty();
        }

        final String url = UriComponentsBuilder.fromUriString(GEO_API_GOUV_COMMUNE_BY_CODE_ENDPOINT).buildAndExpand(inseeCode).toUriString();

        try {
            GouvApiReverseGeocoding commune = restTemplate.getForObject(url, GouvApiReverseGeocoding.class);
            if (commune != null && StringUtils.isNotBlank(commune.getNom())) {
                return Optional.of(commune.getNom());
            }
        } catch (Exception e) {
            logger.error("Error on city name recovering from insee code {}", inseeCode, e);
            logger.error("url : {}", url);
        }
        return Optional.empty();
    }
}
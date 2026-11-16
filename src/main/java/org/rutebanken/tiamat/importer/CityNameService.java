package org.rutebanken.tiamat.importer;

import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.externalapis.GouvApiReverseGeocoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class CityNameService {

    private static final Logger logger = LoggerFactory.getLogger(CityNameService.class);

    private static final String GEO_API_GOUV_ALL_COMMUNES_ENDPOINT = "https://geo.api.gouv.fr/communes?fields=nom&format=json";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);
    /**
     * With timeouts so that geo.api.gouv.fr not responding cannot block the refill (and the scheduler thread) forever.
     * Read timeout is generous because the endpoint returns all French communes in one response.
     */
    private final RestTemplate restTemplate = createRestTemplate();
    /**
     * Immutable snapshot, replaced as a whole on each successful refill: reads are lock-free and never see a partial cache.
     */
    private volatile Map<String, String> cityNamesByInseeCode = Map.of();

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return new RestTemplate(requestFactory);
    }

    public Optional<String> getCityNameFromInseeCode(String inseeCode) {
        if (StringUtils.isBlank(inseeCode)) {
            return Optional.empty();
        }
        return Optional.ofNullable(cityNamesByInseeCode.get(inseeCode));
    }

    @Scheduled(fixedDelay = 3, timeUnit = TimeUnit.MINUTES, initialDelay = 3)
    public void refillCacheIfEmpty() {
        if (cityNamesByInseeCode.isEmpty()) {
            logger.info("City names cache is empty. Refill it");
            refillCache();
        }
    }

    @Scheduled(fixedRate = 24, timeUnit = TimeUnit.HOURS)
    public void refillCache() {
        logger.info("Refilling city names cache");
        Map<String, String> tmpCache = new HashMap<>();
        try {
            GouvApiReverseGeocoding[] communes = restTemplate.getForObject(GEO_API_GOUV_ALL_COMMUNES_ENDPOINT, GouvApiReverseGeocoding[].class);
            if (communes == null || communes.length == 0) {
                logger.error("No communes found from {}", GEO_API_GOUV_ALL_COMMUNES_ENDPOINT);
                return;
            }
            for (GouvApiReverseGeocoding commune : communes) {
                // Map.copyOf rejects null keys and values
                if (commune != null && StringUtils.isNotBlank(commune.getCode()) && StringUtils.isNotBlank(commune.getNom())) {
                    tmpCache.put(commune.getCode(), commune.getNom());
                }
            }
            cityNamesByInseeCode = Map.copyOf(tmpCache);
            logger.info("City names cache refilled with {} communes", tmpCache.size());
        } catch (Exception e) {
            logger.error("Error while refilling city names cache", e);
        }
    }
}

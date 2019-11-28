package org.rutebanken.tiamat.externalapis;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.math.MathContext;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiProxyServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(ApiProxyServiceTest.class);


    ApiProxyService apiProxyService = new ApiProxyService();

    @Test
    public void apiGouvTest(){
        String citycodeReverseGeocoding = null;
        try {
            citycodeReverseGeocoding = apiProxyService.getCitycodeByReverseGeocoding(new BigDecimal(43.72042, MathContext.DECIMAL64), new BigDecimal(-1.0512559, MathContext.DECIMAL64));
        } catch (Exception e) {
            logger.error("Impossible de récupérer le code postal de cette position à partir de l'api public gouv", e);
        }

        assertThat(citycodeReverseGeocoding).isNotNull();
    }

    @Test
    public void twoApiGouvTest(){
        String citycodeReverseGeocoding = null;
        try {
            citycodeReverseGeocoding = apiProxyService.getCitycodeByReverseGeocoding(new BigDecimal(49.102073, MathContext.DECIMAL64), new BigDecimal(2.041524, MathContext.DECIMAL64));
        } catch (Exception e) {
            logger.error("Impossible de récupérer le code postal de cette position à partir de l'api public gouv", e);
        }

        assertThat(citycodeReverseGeocoding).isNotNull();
    }

}
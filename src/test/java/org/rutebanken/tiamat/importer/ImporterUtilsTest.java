package org.rutebanken.tiamat.importer;

import org.junit.Test;
import org.rutebanken.tiamat.externalapis.DtoGeocode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;


import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ImporterUtilsTest {
    private static final Logger logger = LoggerFactory.getLogger(ImporterUtilsTest.class);

    @Test
    public void getCityCodeFromThreeApis(){
        DtoGeocode dtoGeocode = null;
        try {
            dtoGeocode = ImporterUtils.getGeocodeDataByReverseGeocoding(-1.0512559, 43.72042);
        } catch (Exception e) {
            logger.error("Impossible de récupérer le code postal de cette position à partir de l'api public gouv", e);
        }

        assertThat(dtoGeocode).isNotNull();
        assertThat(dtoGeocode.getCityCode()).isNotNull();
    }

}
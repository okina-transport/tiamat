package org.rutebanken.tiamat.externalapis.gbfs.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mobilitydata.gbfs.v3_0.system_information.GBFSSystemInformation;
import org.rutebanken.tiamat.model.Organisation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SystemInformationMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String GBFS_STATION_INFORMATION_RAW_JSON = """
            {
              "last_updated": "2023-07-17T13:34:13+02:00",
              "ttl": 1800,
              "version": "3.0",
              "data": {
                "system_id": "nantes",
                "languages": ["fr"],
                "name": [
                  {
                    "text": "NANTES - Naolib",
                    "language": "fr"
                  }
                ],
                "short_name": [
                  {
                    "text": "Naolib",
                    "language": "fr"
                  }
                ],
                "operator": [
                  {
                    "text": "NAOLIB",
                    "language": "fr"
                  }
                ],
                "opening_hours": "Apr 1-Nov 3 00:00-24:00",
                "start_date": "2010-06-10",
                "url": "https://velo.naolib.fr/",
                "purchase_url": "https://velo.naolib.fr/fr/offers/groups",
                "phone_number": "+33130793344",
                "email": "developer@jcdecaux.com",
                "feed_contact_email": "datafeed@example.com",
                "timezone": "Europe/Paris",
                "license_url": "https://www.example.com/data-license.html",
                "terms_url": [
                  {
                     "text": "https://www.example.com/en/terms",
                     "language": "en"
                  }
                ],
                "terms_last_updated": "2021-06-21",
                "privacy_url": [
                  {
                     "text": "https://www.example.com/en/privacy-policy",
                     "language": "en"
                  }
                ],
                "privacy_last_updated": "2019-01-13",
                "rental_apps": {
                  "android": {
                    "discovery_uri": "com.jcdecaux.vls.nantes.android://",
                    "store_uri": "https://play.google.com/store/apps/details?id=com.jcdecaux.vls.nantes"
                  },
                  "ios": {
                    "store_uri": "https://itunes.apple.com/app/id1414197331",
                    "discovery_uri": "com.jcdecaux.vls.nantes.ios://"
                  }
                },
                "brand_assets": {
                    "brand_last_modified": "2021-06-15",
                    "brand_image_url": "https://www.example.com/assets/brand_image.svg",
                    "brand_image_url_dark": "https://www.example.com/assets/brand_image_dark.svg",
                    "color": "#C2D32C",
                    "brand_terms_url": "https://www.example.com/assets/brand.pdf"
                  }
            
              }
            }""";

    private final SystemInformationMapper tested = new SystemInformationMapper();

    @Test
    public void test_toOrganisation_whenInputGBFSIsValid_thenShouldConvertProperly() throws JsonProcessingException {
        // Arrange
        GBFSSystemInformation si = MAPPER.readValue(GBFS_STATION_INFORMATION_RAW_JSON, GBFSSystemInformation.class);

        // Act
        Organisation output = tested.toOrganisation(si);

        // Assert
        assertNull(output.getNetexId());
        assertEquals("fr", output.getLanguage());
        assertEquals("NANTES - Naolib", output.getName());
        assertEquals("Naolib", output.getShortName());
        assertEquals("NAOLIB", output.getOperator());
        assertEquals("https://velo.naolib.fr/", output.getOrganisationUrl());
        assertEquals("https://velo.naolib.fr/fr/offers/groups", output.getPurchaseUrl());
        assertEquals("+33130793344", output.getPhoneNumber());
        assertEquals("developer@jcdecaux.com", output.getEmail());
        assertEquals("Europe/Paris", output.getTimezone());
        assertEquals("com.jcdecaux.vls.nantes.android://", output.getAndroidDiscoveryUri());
        assertEquals("https://play.google.com/store/apps/details?id=com.jcdecaux.vls.nantes", output.getAndroidStoreUri());
        assertEquals("com.jcdecaux.vls.nantes.ios://", output.getIosDiscoveryUri());
        assertEquals("https://itunes.apple.com/app/id1414197331", output.getIosStoreUri());

    }

}
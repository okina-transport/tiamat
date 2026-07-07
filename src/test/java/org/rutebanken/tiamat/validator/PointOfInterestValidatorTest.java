package org.rutebanken.tiamat.validator;

import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.PointOfInterest;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.SiteFrame;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryUnmarshaller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.rutebanken.tiamat.config.Messages.VALIDATION_INVALID_ID_FORMAT;
import static org.rutebanken.tiamat.config.Messages.VALIDATION_POI_CENTROID_REQUIRED;

class PointOfInterestValidatorTest {

    private static final File NETEX_POI_MISSING_CENTROID = new File("src/test/resources/manualImports/poiNetex/errorClassification/poi_missing_centroid.xml");
    private static final File NETEX_POI_INVALID_ID = new File("src/test/resources/manualImports/poiNetex/errorClassification/poi_invalid_id.xml");
    private static final File NETEX_POI_TWO_VALID = new File("src/test/resources/manualImports/poiNetex/poi_two_valid.xml");

    private final PublicationDeliveryUnmarshaller unmarshaller = new PublicationDeliveryUnmarshaller();
    private final PointOfInterestValidator tested = new PointOfInterestValidator();

    PointOfInterestValidatorTest() throws IOException, SAXException {
    }

    @Test
    void givenCentroidIsMissing_whenValidatingPoi_thenProducesValidationError() throws IOException, JAXBException, SAXException {
        PointOfInterest poi = retrievePointsOfInterest(NETEX_POI_MISSING_CENTROID).get(0);
        Errors errors = new BeanPropertyBindingResult(poi, "");

        tested.validate(poi, errors);

        assertNotNull(errors.getFieldError("centroid.location.longitude"));
        assertEquals(VALIDATION_POI_CENTROID_REQUIRED, errors.getFieldError("centroid.location.longitude").getCode());
        assertNotNull(errors.getFieldError("centroid.location.latitude"));
        assertEquals(VALIDATION_POI_CENTROID_REQUIRED, errors.getFieldError("centroid.location.latitude").getCode());
    }

    @Test
    void givenIdFormatIsInvalid_whenValidatingPoi_thenProducesValidationError() throws IOException, JAXBException, SAXException {
        PointOfInterest poi = retrievePointsOfInterest(NETEX_POI_INVALID_ID).get(0);
        Errors errors = new BeanPropertyBindingResult(poi, "");

        tested.validate(poi, errors);

        assertNotNull(errors.getFieldError("id"));
        assertEquals(VALIDATION_INVALID_ID_FORMAT, errors.getFieldError("id").getCode());
    }

    @Test
    void givenPoiIsValid_whenValidatingPoi_thenDoesNotProduceValidationError() throws IOException, JAXBException, SAXException {
        for (PointOfInterest poi : retrievePointsOfInterest(NETEX_POI_TWO_VALID)) {
            Errors errors = new BeanPropertyBindingResult(poi, "");

            tested.validate(poi, errors);

            assertFalse(errors.hasErrors(), "there should be no validation error for a valid POI");
        }
    }

    private List<PointOfInterest> retrievePointsOfInterest(File inputNetexXml) throws JAXBException, IOException, SAXException {
        try (InputStream inputStream = new FileInputStream(inputNetexXml)) {
            PublicationDeliveryStructure pd = unmarshaller.unmarshal(inputStream);
            SiteFrame siteFrame = pd.getDataObjects().getCompositeFrameOrCommonFrame().stream()
                    .map(jaxbElement -> jaxbElement.getValue())
                    .filter(SiteFrame.class::isInstance)
                    .map(SiteFrame.class::cast)
                    .findFirst()
                    .orElseThrow();
            return siteFrame.getPointsOfInterest().getPointOfInterest();
        }
    }
}
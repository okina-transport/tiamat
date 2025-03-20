package org.rutebanken.tiamat.service.parking;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.externalapis.gbfs.GbfsClient;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.model.SpecificParkingAreaUsageEnumeration;
import org.rutebanken.tiamat.model.gbfs.GbfsFeedItem;
import org.rutebanken.tiamat.model.gbfs.GbfsImportLinks;
import org.rutebanken.tiamat.model.gbfs.GbfsValidationOutput;
import org.rutebanken.tiamat.model.gbfs.api.FeedContainer;
import org.rutebanken.tiamat.model.gbfs.api.GbfsDataApiResponse;
import org.rutebanken.tiamat.service.parking.gbfs.GbfsFeedValidator;
import org.rutebanken.tiamat.service.parking.gbfs.GbfsFeedValidatorFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GbfsInputValidator {

    private final GbfsClient gbfsClient;

    public GbfsInputValidator(GbfsClient gbfsClient) {
        this.gbfsClient = gbfsClient;
    }

    private static boolean isInvalidGbfsFeedFile(GbfsDataApiResponse<FeedContainer> response) {
        return response == null || response.getData() == null || response.getData().getFeedData() == null ||
                CollectionUtils.isEmpty(response.getData().getFeedData().getFeeds());
    }

    public GbfsValidationOutput validateInput(GbfsImportLinks links) {
        GbfsValidationOutput validationOutput = new GbfsValidationOutput();
        if (links == null || StringUtils.isBlank(links.getGlobalUrl())) {
            validationOutput.setErrors(List.of("Target url is invalid"));
            return validationOutput;
        }
        if (StringUtils.isBlank(links.getParkingType())) {
            validationOutput.setErrors(List.of("Parking type is blank"));
            return validationOutput;
        } else {
            try {
                ParkingTypeEnumeration.fromValue(links.getParkingType());
            } catch (Exception e) {
                validationOutput.setErrors(List.of("Parking type is invalid"));
                return validationOutput;
            }
        }
        if (StringUtils.isBlank(links.getParkingAreaType())) {
            validationOutput.setErrors(List.of("Parking area type is blank"));
            return validationOutput;
        } else {
            try {
                SpecificParkingAreaUsageEnumeration.fromValue(links.getParkingAreaType());
            } catch (IllegalArgumentException e) {
                validationOutput.setErrors(List.of("Parking area type is invalid"));
                return validationOutput;
            }
        }
        GbfsDataApiResponse<FeedContainer> response = gbfsClient.getData(links.getGlobalUrl(), FeedContainer.class);
        if (isInvalidGbfsFeedFile(response)) {
            validationOutput.setErrors(List.of("GBFS feed file is invalid"));
            return validationOutput;
        } else {
            List<GbfsFeedItem> feedItems = response.getData().getFeedData().getFeeds();
            for (GbfsFeedItem feedItem : feedItems) {
                GbfsFeedValidator validator = GbfsFeedValidatorFactory.init(gbfsClient, feedItem.getServiceName()).build();
                validationOutput.updateValidationState(validator.validateFeed(feedItem.getServiceUrl()));
            }
        }
        return validationOutput;
    }
}

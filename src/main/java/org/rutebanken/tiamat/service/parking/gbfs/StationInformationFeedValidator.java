package org.rutebanken.tiamat.service.parking.gbfs;

import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.model.gbfs.GbfsValidationOutput;
import org.rutebanken.tiamat.model.gbfs.api.GbfsDataApiResponse;
import org.rutebanken.tiamat.model.gbfs.api.StationData;

import java.util.List;

public class StationInformationFeedValidator extends GbfsFeedValidator {
    @Override
    public GbfsValidationOutput validateFeed(String url) {
        GbfsValidationOutput validationOutput = new GbfsValidationOutput();
        if (StringUtils.isBlank(url)) {
            validationOutput.setErrors(List.of("Invalid station information url"));
            return validationOutput;
        }
        GbfsDataApiResponse<StationData> stationResponse = gbfsClient.getData(url, StationData.class);
        if (stationResponse == null || stationResponse.getData() == null) {
            validationOutput.setErrors(List.of("Invalid station information data"));
        } else {
            validationOutput.setStations(stationResponse.getData().getStations());
        }
        return validationOutput;
    }
}

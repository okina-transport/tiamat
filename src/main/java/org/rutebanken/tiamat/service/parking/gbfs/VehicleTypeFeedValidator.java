package org.rutebanken.tiamat.service.parking.gbfs;

import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.model.gbfs.GbfsValidationOutput;
import org.rutebanken.tiamat.model.gbfs.api.GbfsDataApiResponse;
import org.rutebanken.tiamat.model.gbfs.api.VehicleData;

import java.util.List;

public class VehicleTypeFeedValidator extends GbfsFeedValidator {
    @Override
    public GbfsValidationOutput validateFeed(String url) {
        GbfsValidationOutput validationOutput = new GbfsValidationOutput();
        if (StringUtils.isBlank(url)) {
            validationOutput.setErrors(List.of("Invalid vehicle type url"));
            return validationOutput;
        }
        GbfsDataApiResponse<VehicleData> vehicleResponse = gbfsClient.getData(url, VehicleData.class);
        if (vehicleResponse == null || vehicleResponse.getData() == null) {
            validationOutput.setErrors(List.of("Invalid vehicle type data"));
        } else {
            validationOutput.setVehicleTypes(vehicleResponse.getData().getVehicleTypes());
        }
        return validationOutput;

    }
}

package org.rutebanken.tiamat.service.parking.gbfs;

import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.model.gbfs.GbfsValidationOutput;
import org.rutebanken.tiamat.model.gbfs.SystemInformation;
import org.rutebanken.tiamat.model.gbfs.api.GbfsDataApiResponse;

import java.util.List;

public class SystemInformationFeedValidator extends GbfsFeedValidator {
    @Override
    public GbfsValidationOutput validateFeed(String url) {
        GbfsValidationOutput validationOutput = new GbfsValidationOutput();
        if (StringUtils.isBlank(url)) {
            validationOutput.setErrors(List.of("Invalid system information url"));
            return validationOutput;
        }
        GbfsDataApiResponse<SystemInformation> systemResponse = gbfsClient.getData(url, SystemInformation.class);
        if (systemResponse == null || systemResponse.getData() == null) {
            validationOutput.setErrors(List.of("Invalid system information data"));
        } else {
            validationOutput.setSystemInformation(systemResponse.getData());
        }
        return validationOutput;
    }
}

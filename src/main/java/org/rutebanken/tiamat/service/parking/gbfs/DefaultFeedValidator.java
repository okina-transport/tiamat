package org.rutebanken.tiamat.service.parking.gbfs;

import org.rutebanken.tiamat.model.gbfs.GbfsValidationOutput;

public class DefaultFeedValidator extends GbfsFeedValidator{
    @Override
    public GbfsValidationOutput validateFeed(String url) {
        return new GbfsValidationOutput();
    }
}

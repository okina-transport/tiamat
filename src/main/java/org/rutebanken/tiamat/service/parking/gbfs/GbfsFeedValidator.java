package org.rutebanken.tiamat.service.parking.gbfs;

import org.rutebanken.tiamat.externalapis.gbfs.GbfsClient;
import org.rutebanken.tiamat.model.gbfs.GbfsValidationOutput;

public abstract class GbfsFeedValidator {

    protected GbfsClient gbfsClient;

    public abstract GbfsValidationOutput validateFeed(String url);
}

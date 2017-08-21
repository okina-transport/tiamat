package org.rutebanken.tiamat.rest.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.rutebanken.tiamat.model.EntityInVersionStructure;
import org.rutebanken.tiamat.model.GeneralSign;
import org.rutebanken.tiamat.model.GroupOfEntities_VersionStructure;
import org.rutebanken.tiamat.model.PrivateCodeStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrivateCodeFetcher implements DataFetcher<PrivateCodeStructure> {

    private static final Logger logger = LoggerFactory.getLogger(PrivateCodeFetcher.class);

    @Override
    public PrivateCodeStructure get(DataFetchingEnvironment dataFetchingEnvironment) {

        logger.trace("Fetching private code from source {}", (Object[]) dataFetchingEnvironment.getSource());

        PrivateCodeStructure privateCode = null;
        if(dataFetchingEnvironment.getSource() instanceof GeneralSign) {
            privateCode = ((GeneralSign) dataFetchingEnvironment.getSource()).getPrivateCode();
        } else if(dataFetchingEnvironment.getSource() instanceof GroupOfEntities_VersionStructure) {
            privateCode = ((GroupOfEntities_VersionStructure) dataFetchingEnvironment.getSource()).getPrivateCode();
        }

        return privateCode;
    }
}

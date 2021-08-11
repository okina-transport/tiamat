/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.rest.graphql.fetchers;


import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.service.mainti4.IServiceTiamatApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Mainti4UrlFetcher implements DataFetcher {

    @Autowired
    @Qualifier("mainti4serviceapilogin")
    IServiceTiamatApi mainti4ServiceLogin;

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private QuayRepository quayRepository;

    private static final Logger logger = LoggerFactory.getLogger(Mainti4UrlFetcher.class);

    @Override
    public Object get(DataFetchingEnvironment environment) {
        String url;
        String id = environment.getArgument("id");
        String type = environment.getArgument("type");
        Map<String, String> data = null;
        switch (type) {
            case "quay":
                url = getUrlQuay(id);
                break;
            default:
                url = getUrlStopPlace(id);
                break;
        }
        if (url != null) {
            data = new HashMap<>();
            data.put("id", id);
            data.put("type", type);
            data.put("url", url);
        }
        return data;
    }

    private String getUrlQuay(String rId) {
        logger.debug("Demande url pour quai avec id {}", rId);
        List<Quay> lQuay = quayRepository.findByNetexId(rId);
        if (lQuay != null) {
            return this.mainti4ServiceLogin.getUrlFromIdQuay(lQuay.get(0));
        }
        logger.debug("Pas de quai trouve avec id {}. On ne peut pas calculer l'url", rId);
        return null;
    }

    private String getUrlStopPlace(String rId) {
        logger.debug("Demande url pour stopplace avec id {}", rId);
        List<StopPlace> lStop = stopPlaceRepository.findByNetexId(rId);
        if (lStop != null) {
            return this.mainti4ServiceLogin.getUrlFromIdStopPlace(lStop.get(0));
        }
        logger.debug("Pas de stop place trouve avec id {}. On ne peut pas calculer l'url", rId);
        return null;
    }

}

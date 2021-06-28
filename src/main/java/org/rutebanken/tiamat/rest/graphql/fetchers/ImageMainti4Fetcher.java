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
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.Zone_VersionStructure;
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

import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.*;

@Component
public class ImageMainti4Fetcher implements DataFetcher {

    @Autowired
    @Qualifier("mainti4serviceapilogin")
    IServiceTiamatApi mainti4ServiceLogin;

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    private static final Logger logger = LoggerFactory.getLogger(ImageMainti4Fetcher.class);

    @Override
    public Object get(DataFetchingEnvironment environment) {
        String id = environment.getArgument("id");
        logger.debug("Demande image pour stop place avec id {}", id);
        //Recupere le stopplace
        List<StopPlace> lStop = stopPlaceRepository.findByNetexId(id);
        if (lStop != null) {
            logger.debug("Recuperation de l'image pour le stop place avec id {}", id);
            //Demande l'image cote mainti4
            //FIXED: on prend premier resultat
            BufferedImage lImage = this.mainti4ServiceLogin.getPhoto(lStop.get(0));
            //Met l'image en base64 dans une map pour repondre
            Map<String, String> data = new HashMap<>();
            data.put("id", id);
            data.put("image", (lImage != null) ? imageToBase64String(lImage, "png") : "");
            return data;
        }
        logger.debug("Pas de stop place trouve avec id {}", id);
        return null;
    }

    /**
     * Transforme le buffer d'image en base64
     * @param image : buffer d'image
     * @param type : type d'image (png, jpg, etc.)
     * @return l'image en base64
     */
    public static String imageToBase64String(BufferedImage image, String type) {
        String imageString = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, type, bos);
            byte[] imageBytes = bos.toByteArray();
            imageString = Base64.getEncoder().encodeToString(imageBytes);
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageString;
    }
}

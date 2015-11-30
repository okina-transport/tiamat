package no.rutebanken.tiamat.nvdb.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.rutebanken.tiamat.nvdb.model.Egenskaper;
import no.rutebanken.tiamat.nvdb.model.VegObjekt;
import no.rutebanken.tiamat.nvdb.model.VegobjekterResultat;
import no.rutebanken.tiamat.repository.ifopt.StopPlaceRepository;
import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.org.netex.netex.MultilingualString;
import uk.org.netex.netex.StopPlace;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This service is a temporary solution for retrieval of stop place data from NVDB.
 */
@Service
public class NvdbSync {

    private static final Logger logger = LoggerFactory.getLogger(NvdbSync.class);
    private static final int EGENSKAP_HOLDEPLASS_NAVN = 3957;

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    public void fetchNvdb() {
        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(factory);
        try {
            InputStream inputStream = Request.Get("https://www.vegvesen.no/nvdb/api/vegobjekter/487")
                    .connectTimeout(1000)
                    .socketTimeout(1000)
                    .execute().returnContent().asStream();


            VegobjekterResultat result = mapper.readValue(inputStream, VegobjekterResultat.class);
            logger.info("Got {} objects", result.getVegObjekter().size());

            List<StopPlace> stopPlaces = result.getVegObjekter().parallelStream()
                    .filter(Objects::nonNull)
                    .map(this::mapToStopPlace)
                    .map(stopPlace ->  stopPlaceRepository.save(stopPlace))
                    .collect(Collectors.toList());


            logger.info("Saved {} stop places", stopPlaces.size());
        } catch (IOException e) {
            logger.warn("Could not fetch data from nvdb", e);
        }
    }


    public StopPlace mapToStopPlace(VegObjekt vegObjekt) {
        logger.info("Mapping object {}", vegObjekt);

        StopPlace stopPlace = new StopPlace();


        for(Egenskaper egenskap : vegObjekt.getEgenskaper()) {
            if (egenskap.getId().equals(EGENSKAP_HOLDEPLASS_NAVN)) {
                stopPlace.setName(new MultilingualString(egenskap.getVerdi(), "no", ""));
            }
        }

        return stopPlace;
    }

}

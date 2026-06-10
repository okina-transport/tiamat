package org.rutebanken.tiamat.rest.netex.publicationdelivery.mapper;

import org.hibernate.Hibernate;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.QuayView;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.EXTERNAL_REF;
import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.ORIGINAL_ID_KEY;

@Component
public class QuayViewMapper {

    private final StopPlaceRepository stopPlaceRepository;

    public QuayViewMapper(StopPlaceRepository stopPlaceRepository) {
        this.stopPlaceRepository = stopPlaceRepository;
    }

    public QuayView toQuayView(Quay quay) {
        QuayView view = new QuayView();

        view.setImportedId(extractImportedId(quay));
        view.setNetexId(quay.getNetexId());
        view.setName(quay.getName() == null ? null : quay.getName().toString());

        if (quay.getCentroid() != null) {
            view.setLongitude(BigDecimal.valueOf(quay.getCentroid().getX()));
            view.setLatitude(BigDecimal.valueOf(quay.getCentroid().getY()));
        }

        enrichWithStopPlace(view, quay);
        return view;
    }

    private String extractImportedId(Quay quay) {
        return Optional.ofNullable(quay.getKeyValues())
                .map(m -> m.get(ORIGINAL_ID_KEY))
                .map(val -> val.getItems().isEmpty() ? null : val.getItems().stream().findFirst().orElse(null))
                .orElse(null);
    }

    private void enrichWithStopPlace(QuayView view, Quay quay) {
        StopPlace stopPlace = stopPlaceRepository.findByQuay(quay);

        if (stopPlace != null) {
            Hibernate.initialize(stopPlace.getKeyValues());
            stopPlace.getKeyValues().values().forEach(value -> Hibernate.initialize(value.getItems()));

            org.rutebanken.tiamat.model.Value val = stopPlace.getKeyValues().get(EXTERNAL_REF);
            String imported = val != null ? val.getItems().stream().findFirst().orElse(null) : null;

            view.setNetexStopPlaceId(stopPlace.getNetexId());
            view.setStopPlaceImportedId(imported);
        }
    }
}


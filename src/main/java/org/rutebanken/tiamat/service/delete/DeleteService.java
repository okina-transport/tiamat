package org.rutebanken.tiamat.service.delete;

import org.rutebanken.tiamat.model.PointOfInterestClassification;
import org.rutebanken.tiamat.model.identification.IdentifiedEntity;
import org.rutebanken.tiamat.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeleteService {

    private static final Logger logger = LoggerFactory.getLogger(DeleteService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PointOfInterestRepository pointOfInterestRepository;

    @Autowired
    private PointOfInterestOpeningHoursRepository pointOfInterestOpeningHoursRepository;

    @Autowired
    PointOfInterestFacilitySetRepository pointOfInterestFacilitySetRepository;

    @Autowired
    PointOfInterestClassificationRepository pointOfInterestClassificationRepository;

    @Autowired
    ParkingRepository parkingRepository;


    public boolean deleteAllPoi(){

        List<Long> listPoiId = pointOfInterestRepository.findAll().stream().map(IdentifiedEntity::getId).collect(Collectors.toList());

        List<String> listValue = new ArrayList<>();
        List<String> listEquipmentPlace = new ArrayList<>();
        List<String> listClassification = new ArrayList<>();

        if(!listPoiId.isEmpty()){
            final Query query = entityManager.createNativeQuery("SELECT poikv.key_values_id FROM point_of_interest_key_values poikv WHERE poikv.point_of_interest_id IN :poilist");
            query.setParameter("poilist", listPoiId);
            listValue.addAll(query.getResultList());

            final Query query3 = entityManager.createNativeQuery("SELECT poics.classifications_id FROM point_of_interest_classifications poics WHERE poics.point_of_interest_id IN :poilist");
            query3.setParameter("poilist", listPoiId);
            listClassification.addAll(query3.getResultList());
        }

        if(!listClassification.isEmpty()){
            final Query query4 = entityManager.createNativeQuery("SELECT poickv.key_values_id FROM point_of_interest_classification_key_values poickv WHERE poickv.point_of_interest_classification_id IN :listClassification");
            query4.setParameter("listClassification", listClassification);
            listValue.addAll(query4.getResultList());
        }

        List<Long> listPoiOhDt = pointOfInterestOpeningHoursRepository.findAll().stream().map(IdentifiedEntity::getId).collect(Collectors.toList());

        pointOfInterestRepository.deleteAll();

        if(!listValue.isEmpty()){
            final Query query5 = entityManager.createNativeQuery("DELETE FROM value_items vi WHERE vi.value_id IN :valuelist");
            query5.setParameter("valuelist", listValue);
            query5.executeUpdate();
        }

        List<PointOfInterestClassification> listPoicParent = pointOfInterestClassificationRepository.findAll().stream()
                .filter(pointOfInterestClassification -> pointOfInterestClassification.getParent() != null).collect(Collectors.toList());
        pointOfInterestClassificationRepository.deleteAll(listPoicParent);

        pointOfInterestClassificationRepository.deleteAll();
        pointOfInterestOpeningHoursRepository.deleteAll();
        pointOfInterestFacilitySetRepository.deleteAll();

        return true;
    }

    public boolean deleteAllParkings(){

        parkingRepository.deleteAll();
        return true;
    }
}

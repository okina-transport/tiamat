package org.rutebanken.tiamat.service.parking;

import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.model.Organisation;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.repository.OrganisationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ParkingOperatorOrganisationService {

    private static final Logger logger = LoggerFactory.getLogger(ParkingOperatorOrganisationService.class);

    public static final String OPERATOR_ORGANISATION_TYPE = "operator";

    private final OrganisationRepository organisationRepository;
    private final String superIdPrefix;

    public ParkingOperatorOrganisationService(OrganisationRepository organisationRepository,
                                              @Value("${netex.validPrefix:MOBIITI}") String superIdPrefix) {
        this.organisationRepository = organisationRepository;
        this.superIdPrefix = superIdPrefix;
    }

    public void attachOperatorOrganisation(Parking parking) {
        String operatorName = StringUtils.trimToNull(parking.getOperator());
        if (operatorName == null) {
            return;
        }
        parking.setOrganisation(findOrCreateOperatorOrganisation(operatorName));
    }

    private Organisation findOrCreateOperatorOrganisation(String operatorName) {
        return organisationRepository.findFirstByNameOrderByIdAsc(operatorName)
                .orElseGet(() -> {
                    Organisation organisation = new Organisation();
                    organisation.setNetexId(superIdPrefix + ":Organisation:" + UUID.randomUUID());
                    organisation.setName(operatorName);
                    organisation.setOperator(operatorName);
                    organisation.setType(OPERATOR_ORGANISATION_TYPE);
                    logger.info("Création de l'organisation operator '{}'", operatorName);
                    return organisationRepository.save(organisation);
                });
    }
}

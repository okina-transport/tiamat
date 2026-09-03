package org.rutebanken.tiamat.service.parking;

import groovy.util.logging.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.client.mdm.OkinaIdentifier;
import org.rutebanken.tiamat.importer.mdm.MdmService;
import org.rutebanken.tiamat.model.Organisation;
import org.rutebanken.tiamat.repository.OrganisationRepository;
import org.rutebanken.tiamat.versioning.VersionIncrementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@Slf4j
public class OrganisationsImportedService {

    private static final Logger log = LoggerFactory.getLogger(OrganisationsImportedService.class);
    private final MdmService mdmService;
    private final OrganisationRepository organisationRepository;
    private final VersionIncrementor versionIncrementor;
    private final UsernameFetcher usernameFetcher;

    @Value("${netex.validPrefix:MOBIITI}")
    private String validNetexPrefix;

    public OrganisationsImportedService(MdmService mdmService, OrganisationRepository organisationRepository, VersionIncrementor versionIncrementor, UsernameFetcher usernameFetcher) {
        this.mdmService = mdmService;
        this.organisationRepository = organisationRepository;
        this.versionIncrementor = versionIncrementor;
        this.usernameFetcher = usernameFetcher;
    }

    public Organisation createOrUpdateOrganisation(Organisation newOrganisation) {
        newOrganisation.setChangedBy(usernameFetcher.getUserNameForAuthenticatedUser());
        Optional<Organisation> existingOrganisation = findExistingOrganisation(newOrganisation);
        if (existingOrganisation.isPresent()) {
            return update(newOrganisation, existingOrganisation.get());
        } else {
            return create(newOrganisation);
        }
    }

    public Optional<Organisation> findExistingOrganisation(Organisation organisation) {
        String importedId = CollectionUtils.isNotEmpty(organisation.getOriginalIds()) ? organisation.getOriginalIds().iterator().next() : organisation.getOriginalId();
        Optional<OkinaIdentifier> existingMdmId = mdmService.getExistingOrganisationMdmIdsFromImportedId(importedId);
        return existingMdmId.map(okinaIdentifier -> organisationRepository.findFirstByNetexIdOrderByVersionDesc(validNetexPrefix + ":Organisation:" + okinaIdentifier.getSuperId()));
    }

    private Organisation update(Organisation newOrganisation, Organisation existingOrganisation) {
        boolean updated = false;
        if (StringUtils.isNotBlank(newOrganisation.getChangedBy()) && !StringUtils.equals(newOrganisation.getChangedBy(), existingOrganisation.getChangedBy())) {
            existingOrganisation.setChangedBy(newOrganisation.getChangedBy()); // if only changedBy is updated do not update version
        }
        if (StringUtils.isNotBlank(newOrganisation.getName()) && !StringUtils.equals(newOrganisation.getName(), existingOrganisation.getName())) {
            existingOrganisation.setName(newOrganisation.getName());
            updated = true;
        }
        if (StringUtils.isNotBlank(newOrganisation.getShortName()) && !StringUtils.equals(newOrganisation.getShortName(), existingOrganisation.getShortName())) {
            existingOrganisation.setShortName(newOrganisation.getShortName());
            updated = true;
        }
        if (StringUtils.isNotBlank(newOrganisation.getOperator()) && !StringUtils.equals(newOrganisation.getOperator(), existingOrganisation.getOperator())) {
            existingOrganisation.setOperator(newOrganisation.getOperator());
            updated = true;
        }
        if (StringUtils.isNotBlank(newOrganisation.getOrganisationUrl()) && !StringUtils.equals(newOrganisation.getOrganisationUrl(), existingOrganisation.getOrganisationUrl())) {
            existingOrganisation.setOrganisationUrl(newOrganisation.getOrganisationUrl());
            updated = true;
        }
        if (StringUtils.isNotBlank(newOrganisation.getPurchaseUrl()) && !StringUtils.equals(newOrganisation.getPurchaseUrl(), existingOrganisation.getPurchaseUrl())) {
            existingOrganisation.setPurchaseUrl(newOrganisation.getPurchaseUrl());
            updated = true;
        }
        if (StringUtils.isNotBlank(newOrganisation.getPhoneNumber()) && !StringUtils.equals(newOrganisation.getPhoneNumber(), existingOrganisation.getPhoneNumber())) {
            existingOrganisation.setPhoneNumber(newOrganisation.getPhoneNumber());
            updated = true;
        }
        if (StringUtils.isNotBlank(newOrganisation.getEmail()) && !StringUtils.equals(newOrganisation.getEmail(), existingOrganisation.getEmail())) {
            existingOrganisation.setEmail(newOrganisation.getEmail());
            updated = true;
        }
        if (StringUtils.isNotBlank(newOrganisation.getAndroidStoreUri()) && !StringUtils.equals(newOrganisation.getAndroidStoreUri(), existingOrganisation.getAndroidStoreUri())) {
            existingOrganisation.setAndroidStoreUri(newOrganisation.getAndroidStoreUri());
            updated = true;
        }
        if (StringUtils.isNotBlank(newOrganisation.getAndroidDiscoveryUri()) && !StringUtils.equals(newOrganisation.getAndroidDiscoveryUri(), existingOrganisation.getAndroidDiscoveryUri())) {
            existingOrganisation.setAndroidDiscoveryUri(newOrganisation.getAndroidDiscoveryUri());
            updated = true;
        }
        if (StringUtils.isNotBlank(newOrganisation.getIosStoreUri()) && !StringUtils.equals(newOrganisation.getIosStoreUri(), existingOrganisation.getIosStoreUri())) {
            existingOrganisation.setIosStoreUri(newOrganisation.getIosStoreUri());
            updated = true;
        }
        if (StringUtils.isNotBlank(newOrganisation.getIosDiscoveryUri()) && !StringUtils.equals(newOrganisation.getIosDiscoveryUri(), existingOrganisation.getIosDiscoveryUri())) {
            existingOrganisation.setIosDiscoveryUri(newOrganisation.getIosDiscoveryUri());
            updated = true;
        }
        if (StringUtils.isNotBlank(newOrganisation.getLanguage()) && !StringUtils.equals(newOrganisation.getLanguage(), existingOrganisation.getLanguage())) {
            existingOrganisation.setLanguage(newOrganisation.getLanguage());
            updated = true;
        }
        if (StringUtils.isNotBlank(newOrganisation.getTimezone()) && !StringUtils.equals(newOrganisation.getTimezone(), existingOrganisation.getTimezone())) {
            existingOrganisation.setTimezone(newOrganisation.getTimezone());
            updated = true;
        }
        if (StringUtils.isNotBlank(newOrganisation.getOrganisationUrl()) && !StringUtils.equals(newOrganisation.getOrganisationUrl(), existingOrganisation.getOrganisationUrl())) {
            existingOrganisation.setOrganisationUrl(newOrganisation.getOrganisationUrl());
            updated = true;
        }
        if (StringUtils.isNotBlank(newOrganisation.getOrganisationUrl()) && !StringUtils.equals(newOrganisation.getOrganisationUrl(), existingOrganisation.getOrganisationUrl())) {
            existingOrganisation.setOrganisationUrl(newOrganisation.getOrganisationUrl());
            updated = true;
        }
        if (updated) {
            existingOrganisation.setChanged(Instant.now());
            versionIncrementor.initiateOrIncrement(existingOrganisation);
            existingOrganisation = organisationRepository.save(existingOrganisation);
            log.info("Updated organisation {}, version {}, name {}", existingOrganisation.getNetexId(), existingOrganisation.getVersion(), existingOrganisation.getName());
        }
        return existingOrganisation;
    }

    private Organisation create(Organisation newOrganisation) {
        mdmService.generateIdentifier(newOrganisation);
        newOrganisation.setCreated(Instant.now());
        versionIncrementor.initiateOrIncrement(newOrganisation);
        newOrganisation.getOriginalIds().add(newOrganisation.getOriginalId());
        log.info("Created organisation {}, version {}, name {}", newOrganisation.getNetexId(), newOrganisation.getVersion(), newOrganisation.getName());
        return organisationRepository.save(newOrganisation);
    }

}

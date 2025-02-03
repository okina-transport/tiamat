package org.rutebanken.tiamat.externalapis.gbfs.mapper;

import org.rutebanken.tiamat.model.Organisation;
import org.rutebanken.tiamat.model.gbfs.SystemInformation;

public class SystemInformationMapper {

    public Organisation toOrganisation(SystemInformation systemInformation) {
        Organisation organisation = new Organisation();
        if (systemInformation != null) {
            organisation.setNetexId("MOBIITI:ORGANISATION:"+ systemInformation.getSystemId());
            organisation.setLanguage(systemInformation.getLanguage());
            organisation.setName(systemInformation.getName());
            organisation.setShortName(systemInformation.getShortName());
            organisation.setOperator(systemInformation.getOperator());
            organisation.setOrganisationUrl(systemInformation.getUrl());
            organisation.setPurchaseUrl(systemInformation.getPurchaseUrl());
            organisation.setPhoneNumber(systemInformation.getPhoneNumber());
            organisation.setEmail(systemInformation.getEmail());
            organisation.setTimezone(systemInformation.getTimezone());
            if (systemInformation.getRentalApps() != null) {
                if (systemInformation.getRentalApps().getAndroidUrl() != null) {
                    organisation.setAndroidDiscoveryUri(systemInformation.getRentalApps().getAndroidUrl().getAndroidDiscoveryUri());
                    organisation.setAndroidStoreUri(systemInformation.getRentalApps().getAndroidUrl().getAndroidStoreUri());
                }
                if (systemInformation.getRentalApps().getIosUrl() != null) {
                    organisation.setIosDiscoveryUri(systemInformation.getRentalApps().getIosUrl().getIosDiscoveryUri());
                    organisation.setIosStoreUri(systemInformation.getRentalApps().getIosUrl().getIosStoreUri());
                }
            }

        }
        return organisation;
    }
}

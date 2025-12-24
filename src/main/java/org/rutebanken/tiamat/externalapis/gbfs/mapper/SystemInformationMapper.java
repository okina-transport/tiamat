package org.rutebanken.tiamat.externalapis.gbfs.mapper;

import org.apache.commons.collections4.CollectionUtils;
import org.mobilitydata.gbfs.v3_0.system_information.GBFSData;
import org.mobilitydata.gbfs.v3_0.system_information.GBFSOperator;
import org.mobilitydata.gbfs.v3_0.system_information.GBFSShortName;
import org.mobilitydata.gbfs.v3_0.system_information.GBFSSystemInformation;
import org.rutebanken.tiamat.model.Organisation;

public class SystemInformationMapper {

    private final String superIdPrefix;

    public SystemInformationMapper(String superIdPrefix) {
        this.superIdPrefix = superIdPrefix;
    }

    public Organisation toOrganisation(GBFSSystemInformation si) {
        Organisation organisation = new Organisation();
        if (si == null) {
            return organisation;
        }
        GBFSData data = si.getData();
        organisation.setNetexId(this.superIdPrefix + ":Organisation:" + data.getSystemId());
        String language = data.getLanguages().getFirst();
        organisation.setOriginalId(data.getSystemId());
        organisation.setLanguage(language);
        organisation.setName(data.getName().getFirst().getText());
        if (CollectionUtils.isNotEmpty(data.getShortName())) {
            data.getShortName().stream()
                    .filter(sn -> language.equals(sn.getLanguage()))
                    .map(GBFSShortName::getText)
                    .findFirst()
                    .ifPresent(organisation::setShortName);
        }
        if (CollectionUtils.isNotEmpty(data.getOperator())) {
            data.getOperator().stream()
                    .filter(o -> language.equals(o.getLanguage()))
                    .map(GBFSOperator::getText)
                    .findFirst()
                    .ifPresent(organisation::setOperator);
        }
        organisation.setOrganisationUrl(data.getUrl());
        organisation.setPurchaseUrl(data.getPurchaseUrl());
        organisation.setPhoneNumber(data.getPhoneNumber());
        organisation.setEmail(data.getEmail());
        organisation.setTimezone(data.getTimezone().value());
        if (data.getRentalApps() != null) {
            if (data.getRentalApps().getAndroid() != null) {
                organisation.setAndroidDiscoveryUri(data.getRentalApps().getAndroid().getDiscoveryUri());
                organisation.setAndroidStoreUri(data.getRentalApps().getAndroid().getStoreUri());
            }
            if (data.getRentalApps().getIos() != null) {
                organisation.setIosDiscoveryUri(data.getRentalApps().getIos().getDiscoveryUri());
                organisation.setIosStoreUri(data.getRentalApps().getIos().getStoreUri());
            }
        }
        return organisation;
    }
}

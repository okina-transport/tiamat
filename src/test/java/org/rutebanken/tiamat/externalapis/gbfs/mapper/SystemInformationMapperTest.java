package org.rutebanken.tiamat.externalapis.gbfs.mapper;

import org.junit.Test;
import org.rutebanken.tiamat.model.Organisation;
import org.rutebanken.tiamat.model.gbfs.AndroidUrl;
import org.rutebanken.tiamat.model.gbfs.IosUrl;
import org.rutebanken.tiamat.model.gbfs.RentalApps;
import org.rutebanken.tiamat.model.gbfs.SystemInformation;

import static org.assertj.core.api.Assertions.assertThat;

public class SystemInformationMapperTest {

    private static final String SYSTEM_ID = "nantes";
    private static final String LANGUAGE = "fr";
    private static final String NAME = "Naolib";
    private static final String OPERATOR = "Naolib";
    private static final String ORGANISATION_URL = "https://velo.naolib.fr/";
    private static final String PURCHASE_URL = "https://velo.naolib.fr/fr/offers/groups";
    private static final String PHONE_NUMBER = "+33130793344";
    private static final String EMAIL = "developer@jcdecaux.com";
    private static final String TIMEZONE = "Europe/Paris";
    private static final String ANDROID_DISCOVERY_URL = "com.jcdecaux.vls.nantes://";
    private static final String ANDROID_STORE_URL = "https://play.google.com/store/apps/details?id=com.jcdecaux.vls.nantes";
    private static final String IOS_DISCOVERY_URL = "https://itunes.apple.com/app/id1414197331";
    private static final String IOS_STORE_URL = "com.jcdecaux.vls.nantes://";

    private final SystemInformationMapper systemInformationMapper = new SystemInformationMapper();

    @Test
    public void systemInformationToOrganisation_organisationIdMapping_test() {
        SystemInformation systemInformation = new SystemInformation();
        systemInformation.setSystemId(SYSTEM_ID);

        Organisation result = systemInformationMapper.toOrganisation(systemInformation);

        assertThat(result.getNetexId()).isNotNull();
        assertThat(result.getNetexId()).isEqualTo("MOBIITI:ORGANISATION:"+systemInformation.getSystemId());
    }

    @Test
    public void systemInformationToOrganisation_languageMapping_test() {
        SystemInformation systemInformation = new SystemInformation();
        systemInformation.setLanguage(LANGUAGE);

        Organisation result = systemInformationMapper.toOrganisation(systemInformation);

        assertThat(result.getLanguage()).isNotNull();
        assertThat(result.getLanguage()).isEqualTo(systemInformation.getLanguage());
    }

    @Test
    public void systemInformationToOrganisation_nameMapping_test() {
        SystemInformation systemInformation = new SystemInformation();
        systemInformation.setName(NAME);

        Organisation result = systemInformationMapper.toOrganisation(systemInformation);

        assertThat(result.getName()).isNotNull();
        assertThat(result.getName()).isEqualTo(systemInformation.getName());
    }

    @Test
    public void systemInformationToOrganisation_shortNameMapping_test() {
        SystemInformation systemInformation = new SystemInformation();
        systemInformation.setShortName(NAME);

        Organisation result = systemInformationMapper.toOrganisation(systemInformation);

        assertThat(result.getShortName()).isNotNull();
        assertThat(result.getShortName()).isEqualTo(systemInformation.getShortName());
    }

    @Test
    public void systemInformationToOrganisation_operatorMapping_test() {
        SystemInformation systemInformation = new SystemInformation();
        systemInformation.setOperator(OPERATOR);

        Organisation result = systemInformationMapper.toOrganisation(systemInformation);

        assertThat(result.getOperator()).isNotNull();
        assertThat(result.getOperator()).isEqualTo(systemInformation.getOperator());
    }

    @Test
    public void systemInformationToOrganisation_organisationUrlMapping_test() {
        SystemInformation systemInformation = new SystemInformation();
        systemInformation.setUrl(ORGANISATION_URL);

        Organisation result = systemInformationMapper.toOrganisation(systemInformation);

        assertThat(result.getOrganisationUrl()).isNotNull();
        assertThat(result.getOrganisationUrl()).isEqualTo(systemInformation.getUrl());
    }

    @Test
    public void systemInformationToOrganisation_purchaseUrlMapping_test() {
        SystemInformation systemInformation = new SystemInformation();
        systemInformation.setPurchaseUrl(PURCHASE_URL);

        Organisation result = systemInformationMapper.toOrganisation(systemInformation);

        assertThat(result.getPurchaseUrl()).isNotNull();
        assertThat(result.getPurchaseUrl()).isEqualTo(systemInformation.getPurchaseUrl());
    }

    @Test
    public void systemInformationToOrganisation_phoneNumberMapping_test() {
        SystemInformation systemInformation = new SystemInformation();
        systemInformation.setPhoneNumber(PHONE_NUMBER);

        Organisation result = systemInformationMapper.toOrganisation(systemInformation);

        assertThat(result.getPhoneNumber()).isNotNull();
        assertThat(result.getPhoneNumber()).isEqualTo(systemInformation.getPhoneNumber());
    }

    @Test
    public void systemInformationToOrganisation_emailMapping_test() {
        SystemInformation systemInformation = new SystemInformation();
        systemInformation.setEmail(EMAIL);

        Organisation result = systemInformationMapper.toOrganisation(systemInformation);

        assertThat(result.getEmail()).isNotNull();
        assertThat(result.getEmail()).isEqualTo(systemInformation.getEmail());
    }

    @Test
    public void systemInformationToOrganisation_timezoneMapping_test() {
        SystemInformation systemInformation = new SystemInformation();
        systemInformation.setTimezone(TIMEZONE);

        Organisation result = systemInformationMapper.toOrganisation(systemInformation);

        assertThat(result.getTimezone()).isNotNull();
        assertThat(result.getTimezone()).isEqualTo(systemInformation.getTimezone());
    }

    @Test
    public void systemInformationToOrganisation_androidStoreUriMapping_test() {
        SystemInformation systemInformation = new SystemInformation();
        RentalApps rentalApps = new RentalApps();
        AndroidUrl androidUrl = new AndroidUrl();
        androidUrl.setAndroidStoreUri(ANDROID_STORE_URL);
        rentalApps.setAndroidUrl(androidUrl);
        systemInformation.setRentalApps(rentalApps);

        Organisation result = systemInformationMapper.toOrganisation(systemInformation);

        assertThat(result.getAndroidStoreUri()).isNotNull();
        assertThat(result.getAndroidStoreUri()).isEqualTo(systemInformation.getRentalApps().getAndroidUrl().getAndroidStoreUri());
    }

    @Test
    public void systemInformationToOrganisation_androidDiscoveryUriMapping_test() {
        SystemInformation systemInformation = new SystemInformation();
        RentalApps rentalApps = new RentalApps();
        AndroidUrl androidUrl = new AndroidUrl();
        androidUrl.setAndroidDiscoveryUri(ANDROID_DISCOVERY_URL);
        rentalApps.setAndroidUrl(androidUrl);
        systemInformation.setRentalApps(rentalApps);

        Organisation result = systemInformationMapper.toOrganisation(systemInformation);

        assertThat(result.getAndroidDiscoveryUri()).isNotNull();
        assertThat(result.getAndroidDiscoveryUri()).isEqualTo(systemInformation.getRentalApps().getAndroidUrl().getAndroidDiscoveryUri());
    }

    @Test
    public void systemInformationToOrganisation_iosStoreUriMapping_test() {
        SystemInformation systemInformation = new SystemInformation();
        RentalApps rentalApps = new RentalApps();
        IosUrl iosUrl = new IosUrl();
        iosUrl.setIosStoreUri(IOS_STORE_URL);
        rentalApps.setIosUrl(iosUrl);
        systemInformation.setRentalApps(rentalApps);

        Organisation result = systemInformationMapper.toOrganisation(systemInformation);

        assertThat(result.getIosStoreUri()).isNotNull();
        assertThat(result.getIosStoreUri()).isEqualTo(systemInformation.getRentalApps().getIosUrl().getIosStoreUri());
    }

    @Test
    public void systemInformationToOrganisation_iosDiscoveryUriMapping_test() {
        SystemInformation systemInformation = new SystemInformation();
        RentalApps rentalApps = new RentalApps();
        IosUrl iosUrl = new IosUrl();
        iosUrl.setIosDiscoveryUri(IOS_DISCOVERY_URL);
        rentalApps.setIosUrl(iosUrl);
        systemInformation.setRentalApps(rentalApps);

        Organisation result = systemInformationMapper.toOrganisation(systemInformation);

        assertThat(result.getIosDiscoveryUri()).isNotNull();
        assertThat(result.getIosDiscoveryUri()).isEqualTo(systemInformation.getRentalApps().getIosUrl().getIosDiscoveryUri());
    }

}
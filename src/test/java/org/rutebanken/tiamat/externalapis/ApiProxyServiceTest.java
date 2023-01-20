package org.rutebanken.tiamat.externalapis;


import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;


public class ApiProxyServiceTest {

    @Test
    public void apis() {
        Optional<String> citycodeReverseGeocoding = ApiProxyService.getInseeFromLatLng(2.1444338, 48.7152755);
        Assert.assertTrue(citycodeReverseGeocoding.isPresent());
    }
}
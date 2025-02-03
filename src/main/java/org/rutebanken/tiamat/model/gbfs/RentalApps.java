package org.rutebanken.tiamat.model.gbfs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RentalApps {
    @JsonProperty("android")
    private AndroidUrl androidUrl;

    @JsonProperty("ios")
    private IosUrl iosUrl;

    public AndroidUrl getAndroidUrl() {
        return androidUrl;
    }

    public void setAndroidUrl(AndroidUrl androidUrl) {
        this.androidUrl = androidUrl;
    }

    public IosUrl getIosUrl() {
        return iosUrl;
    }

    public void setIosUrl(IosUrl iosUrl) {
        this.iosUrl = iosUrl;
    }
}

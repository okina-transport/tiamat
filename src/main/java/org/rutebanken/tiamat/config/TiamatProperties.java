package org.rutebanken.tiamat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tiamat")
public class TiamatProperties {

    private boolean mdmEnabled = false;

    public boolean isMdmEnabled() {
        return mdmEnabled;
    }

    public void setMdmEnabled(boolean mdmEnabled) {
        this.mdmEnabled = mdmEnabled;
    }
}

/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.hazelcast;


import com.hazelcast.core.HazelcastInstance;
import org.hibernate.cache.spi.RegionFactory;
import org.rutebanken.hazelcasthelper.service.KubernetesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is implemented because we want to run our own configuration of HazelCast, and we want to make
 * sure that not two instances of Hazelcast is running. (Hazelcast reads default config xml if this class is not used)
 * Because this class is initialized before spring DI, we read properties from System.properties.
 */
@Component
public class TiamatHazelcastCacheRegionFactory extends com.hazelcast.hibernate.HazelcastCacheRegionFactory implements RegionFactory{

    private static final Logger logger = LoggerFactory.getLogger(TiamatHazelcastCacheRegionFactory.class);


    @Autowired
    private ExtendedHazelcastService extendedHazelcastService;


    public HazelcastInstance getHazelCastInstance() {
        return extendedHazelcastService.getHazelcastInstance();
    }

    private static String getProperty(String key, boolean required) {
        String value = System.getProperty(key);
        logger.info("Loaded {}: {}", key, value);
        if(required && value == null) {
            throw new RuntimeException("Property " + key + " cannot be null");
        }
        if(value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private static boolean getBooleanProperty(String key, boolean required) {
        String value = getProperty(key, required);
        if(value == null) {
            return false;
        }
        return value.equalsIgnoreCase("true");
    }

    /**
     * Must be configured in properties file. Like this:
     * spring.jpa.properties.hibernate.cache.region.factory_class=org.rutebanken.tiamat.hazelcast.TiamatHazelcastCacheRegionFactory
     */
    public TiamatHazelcastCacheRegionFactory() {
        logger.info("Created factory with: {}", getHazelcastInstance());
    }

}

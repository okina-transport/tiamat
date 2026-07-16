package org.rutebanken.tiamat.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.locationtech.jts.geom.Point;
import org.rutebanken.tiamat.jackson.EmbeddableMultilingualStringSerializer;
import org.rutebanken.tiamat.jackson.PointSerializer;
import org.rutebanken.tiamat.jackson.QuaySerializer;
import org.rutebanken.tiamat.jackson.StopPlaceSerializer;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonCustomConfig {

    @Bean
    public com.fasterxml.jackson.databind.Module pointModule() {
        SimpleModule module = new SimpleModule();
        // Point serialization produces stack overflow without this
        module.addSerializer(Point.class, new PointSerializer());
        module.addSerializer(EmbeddableMultilingualString.class, new EmbeddableMultilingualStringSerializer());
        module.addSerializer(Quay.class, new QuaySerializer());
        module.addSerializer(StopPlace.class, new StopPlaceSerializer());
        return module;
    }

}

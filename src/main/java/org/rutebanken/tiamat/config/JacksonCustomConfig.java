package org.rutebanken.tiamat.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.locationtech.jts.geom.Point;
import org.rutebanken.tiamat.jackson.PointSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonCustomConfig {

    @Bean
    public com.fasterxml.jackson.databind.Module pointModule(){
        SimpleModule module = new SimpleModule();
        // Point serialization produces stack overflow without this
        module.addSerializer(Point.class, new PointSerializer());
        return module;
    }

}

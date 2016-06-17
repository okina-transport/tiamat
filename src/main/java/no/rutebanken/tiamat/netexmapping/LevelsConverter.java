package no.rutebanken.tiamat.netexmapping;

import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.metadata.Type;
import no.rutebanken.netex.model.Levels_RelStructure;
import no.rutebanken.tiamat.model.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class LevelsConverter extends CustomConverter<ArrayList<Level>, Levels_RelStructure> {
    private static final Logger logger = LoggerFactory.getLogger(LevelsConverter.class);
    @Override
    public no.rutebanken.netex.model.Levels_RelStructure convert(ArrayList<Level> levels, Type<? extends Levels_RelStructure> type) {
        logger.debug("Mapping {} levels into levels_RelStructure", levels.size());

        no.rutebanken.netex.model.Levels_RelStructure levels_relStructure = new no.rutebanken.netex.model.Levels_RelStructure();

        levels.forEach(level -> {
                    levels_relStructure.getLevelRefOrLevel().add(
                            mapperFacade.map(level, no.rutebanken.netex.model.Level.class)
                    );
                }
        );
        return levels_relStructure;
    }
}

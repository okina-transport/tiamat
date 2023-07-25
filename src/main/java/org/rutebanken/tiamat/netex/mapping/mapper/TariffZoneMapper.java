package org.rutebanken.tiamat.netex.mapping.mapper;

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.rutebanken.netex.model.TariffZone;

public class TariffZoneMapper extends CustomMapper<TariffZone, org.rutebanken.tiamat.model.TariffZone> {

    @Override
    public void mapAtoB(TariffZone tariffZone, org.rutebanken.tiamat.model.TariffZone tariffZone2, MappingContext context){
        super.mapAtoB(tariffZone, tariffZone2, context);
        tariffZone2.setNetexId(tariffZone.getId());
    }
}

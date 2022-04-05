package org.rutebanken.tiamat.rest.netex.publicationdelivery;


import org.rutebanken.tiamat.model.StopPlace;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@XmlRootElement(name = "stop_place")
@XmlAccessorType(XmlAccessType.FIELD)
public class StopPlaceView {

    @XmlElement
    private Long id;

    @XmlElement
    private String name;

    @XmlElement
    private String netexId;

    @XmlElement
    private BigDecimal latitude;

    @XmlElement
    private BigDecimal longitude;

    @XmlElement
    private String importedId;

    @XmlElement
    private long version;

    @XmlElement
    private String created;

    @XmlElement
    private String fromDate;

    @XmlElement
    private String toDate;

    @XmlElement
    private List<QuayView> quays;

    public StopPlaceView(StopPlace sp){

        id = sp.getId();
        netexId = sp.getNetexId();
        latitude = new BigDecimal(sp.getCentroid().getCoordinate().y);
        longitude = new BigDecimal(sp.getCentroid().getCoordinate().x);
        importedId = String.join(",", sp.getOriginalIds());
        name = sp.getName().getValue();
        version = sp.getVersion();
        created = formatInstant(sp.getCreated());


        if (sp.getValidBetween() != null) {

            if (sp.getValidBetween().getFromDate() != null){
                fromDate = formatInstant(sp.getValidBetween().getFromDate());
            }

            if (sp.getValidBetween().getToDate() != null){
                toDate = formatInstant(sp.getValidBetween().getToDate());
            }

        }



        if (sp.getQuays() != null){
            quays = sp.getQuays().stream()
                                .map(QuayView::new)
                                .collect(Collectors.toList());
        }


    }

    private String formatInstant(Instant instant){
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime( FormatStyle.SHORT )
                                                        .withLocale( Locale.FRANCE )
                                                        .withZone( ZoneId.systemDefault() );


        return formatter.format( instant );
    }


}

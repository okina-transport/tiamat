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

package org.rutebanken.tiamat.geo;

import com.google.common.base.MoreObjects;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.operation.TransformException;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.Zone_VersionStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class QuayCentroidComputer {

    private static final Logger logger = LoggerFactory.getLogger(QuayCentroidComputer.class);

    /**
     * The threshold in meters for distance between quay and quay centroid.
     * If more than this limit, log a warning;
     */
    public static final int DISTANCE_WARNING_METERS = 400;

    private final CentroidComputer centroidComputer;


    @Autowired
    public QuayCentroidComputer(CentroidComputer centroidComputer) {
        this.centroidComputer = centroidComputer;
    }

    public boolean computeCentroidForQuay(Quay quay) {
        Optional<Point> optionalPoint = Optional.ofNullable(quay.getCentroid().getInteriorPoint());

        if(optionalPoint.isPresent()) {

            // Check each quay's distance to quay centroid.
            // Intention was to reveal quays far avay from eah other, but this incorrectly checks against quay centroid which has been generated.
            Point point = optionalPoint.get();
            boolean changed = quay.getCentroid() == null || !point.equals(quay.getCentroid());
            quay.setCentroid(point);
            if(changed) {
                logger.debug("Created centroid {} for quay. {}", point, quay);

                try {
                    if(quay.getCentroid() != null) {
                        double distanceInMeters = JTS.orthodromicDistance(
                                quay.getCentroid().getCoordinate(),
                                quay.getCentroid().getCoordinate(),
                                DefaultGeographicCRS.WGS84);

                        if (distanceInMeters > DISTANCE_WARNING_METERS) {
                            String stopPlaceString = MoreObjects.toStringHelper(quay)
                                    .omitNullValues()
                                    .add("name", quay.getName() == null ? null : quay.getName().getValue())
                                    .add("originalId", quay.getOriginalIds())
                                    .toString();

                            logger.warn("Calculated quay centroid with {} meters from quay. {} Quay {}",
                                    distanceInMeters, stopPlaceString, quay.getOriginalIds());
                        }
                    }
                } catch (TransformException e) {
                    logger.warn("Could not determine orthodromic distance between quay and quay {}", quay);
                }
            }
            return changed;
        }

        return false;
    }


}

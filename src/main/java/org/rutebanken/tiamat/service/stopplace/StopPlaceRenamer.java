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

package org.rutebanken.tiamat.service.stopplace;

import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.service.MutateLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Transactional
@Service
public class StopPlaceRenamer {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceRenamer.class);

    @Autowired
    StopPlaceRepository stopPlaceRepository;

    @Autowired
    private MutateLock mutateLock;

    public List<StopPlace> checkAllAndRename(boolean shouldSave) {

        return mutateLock.executeInLock(() -> {

            List<StopPlace> updatedStopPlaces = new ArrayList<>();

            logger.info("Lock acquired, start renaming stop places upon modalis recommendations");


            // TODO : agir sur la dernière version uniquement (doit déjà exister une méthode pour ça)
            stopPlaceRepository.findAll().forEach(stopPlace -> {

                String newName = renameIfNeeded(stopPlace.getName().getValue());

                if (newName != null) {

                    // Faire gaffe au cas des multimodal stop place; Si trop compliqué on ne s'en occupe pas.

                    if (shouldSave) {
                        // Utiliser StopPlaveVersionedSaverService#saveNewVersion#saveNewVersion ??

                    }
                }

            });

            logger.info("Done renaming {} stop places", 0);

            return updatedStopPlaces;
        });
    }

    /**
     * Apply various rules to a stop place name to make it compliant with Modalis recommendations
     *
     * @param name original name
     * @return modified name if so, null otherwise.
     */
    public String renameIfNeeded(String name) {

        // TODO : coder les règles de renommage
        return null;
    }


}

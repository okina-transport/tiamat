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

import org.apache.commons.lang3.text.WordUtils;
import org.rutebanken.tiamat.lock.MutateLock;
import org.rutebanken.tiamat.model.AlternativeName;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.NameTypeEnumeration;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.service.AlternativeNameUpdater;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.StopPlaceVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Transactional
@Service
public class StopPlaceRenamer {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceRenamer.class);

    @Autowired
    StopPlaceRepository stopPlaceRepository;

    @Autowired
    private StopPlaceVersionedSaverService stopPlaceVersionedSaverService;

    @Autowired
    private AlternativeNameUpdater alternativeNameUpdater;

    @Autowired
    private MutateLock mutateLock;

    @Autowired
    private VersionCreator versionCreator;



    /**
     * Update stop places with Modalis recommendations
     * @param shouldSave
     * @return
     */

    public Set<StopPlace> checkAllAndRename(boolean shouldSave) {

        return mutateLock.executeInLock(() -> {

            Set<StopPlace> lastVersionStopPlaces = new HashSet<>();
            Set<StopPlace> updatedStopPlaces = new HashSet<>();

            logger.info("Lock acquired, start renaming stop places upon modalis recommendations");

            // Get last version stop place

            stopPlaceRepository.findAll().forEach(stopPlace -> {
                lastVersionStopPlaces.add(stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(stopPlace.getNetexId()));
            });

            lastVersionStopPlaces.forEach(stopPlace -> {
                StopPlace existingStopPlace;
                StopPlace updatedStopPlace;

                existingStopPlace = stopPlace;
                updatedStopPlace = versionCreator.createCopy(existingStopPlace, StopPlace.class);

                AlternativeName otherAlternativeName = new AlternativeName();
                otherAlternativeName.setName(new EmbeddableMultilingualString(existingStopPlace.getName().getValue(), "fr"));
                otherAlternativeName.setNameType(NameTypeEnumeration.OTHER);
                otherAlternativeName.setLang("fra");

                // Update name

                String newName = renameIfNeeded(updatedStopPlace.getName().getValue());

                // Update alternative name

                if (newName != null && !updatedStopPlace.isParentStopPlace()) {
                    updatedStopPlace.setName(new EmbeddableMultilingualString(newName, "fr"));
                    if (updatedStopPlace.getAlternativeNames().isEmpty()) {
                        alternativeNameUpdater.updateAlternativeNames(updatedStopPlace, Arrays.asList(otherAlternativeName));
                    } else {
                        updatedStopPlace.getAlternativeNames().forEach(alternativeName -> {
                            if (!alternativeName.getNameType().equals(NameTypeEnumeration.OTHER)) {
                                alternativeNameUpdater.updateAlternativeNames(updatedStopPlace, Arrays.asList(otherAlternativeName));
                            }
                        });
                    }

                    if (shouldSave && updatedStopPlace.getParentSiteRef() == null) {
                        stopPlaceVersionedSaverService.saveNewVersion(existingStopPlace, updatedStopPlace);
                    }

                    // If stop place have a parent stop, update the parent stop place

                    else if (shouldSave && updatedStopPlace.getParentSiteRef() != null){
                        StopPlace existingStopPlaceParent = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(updatedStopPlace.getParentSiteRef().getRef());
                        StopPlace updatedStopPlaceParent = versionCreator.createCopy(existingStopPlaceParent, StopPlace.class);
                        updatedStopPlaceParent.getChildren().forEach(stopPlaceChildren -> {
                            if(stopPlaceChildren.getNetexId().equals(updatedStopPlace.getNetexId())){
                                updatedStopPlaceParent.getChildren().remove(stopPlaceChildren);
                                updatedStopPlaceParent.getChildren().add(updatedStopPlace);
                            }
                        });
                        stopPlaceVersionedSaverService.saveNewVersion(existingStopPlaceParent, updatedStopPlaceParent);
                    }
                    updatedStopPlaces.add(updatedStopPlace);
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

        String originName = name;

        name = WordUtils.capitalizeFully(name);

        name = name.replace("College", "Collège");
        name = name.replace("Collége", "Collège");

        name = name.replace(" A ", " à ");
        name = name.replace(" À ", " à ");

        name = name.replace("Z.A", "ZA");
        name = name.replace("ZAE.", "ZAE");
        name = name.replace("Z.I.", "ZI");

        name = name.replace(" De ", " de ");
        name = name.replace(" Le ", " le ");
        name = name.replace(" La ", " la ");
        name = name.replace(" Du ", " du ");
        name = name.replace(" Des ", " des ");
        name = name.replace("- la ", "- La ");

        name = name.replace("Rte ", "Route ");
        name = name.replace("Lot. ", "Lotissement ");
        name = name.replace("Imp. ", "Impasse ");
        name = name.replace("Av. ", "Avenue ");
        name = name.replace("Pl. ", "Place ");
        name = name.replace("Ch. ", "Chemin ");
        name = name.replace("St ", "Saint ");
        name = name.replace("St-", "Saint-");


        name = name.replace(" D'", " d'");
        name = name.replace(" L'", " l'");
        name = name.replace("-L'", "-l'");
        name = name.replace(" D ", " d'");
        name = name.replace("-D'", "-d'");

        name = name.replace("Quatre", "4");

        name = name.replace("Sncf", "SNCF");
        name = name.replace("Inra", "INRA");
        name = name.replace("Irsa", "IRSA");
        name = name.replace("Capc", "CAPC");
        name = name.replace(" Lpi", " LPI");
        name = name.replace("Ddass", "DDASS");
        name = name.replace("Aft", "AFT");
        name = name.replace(" Lep", " LEP");
        name = name.replace(" Cfa", " CFA");
        name = name.replace(" Hlm", " HLM");
        name = name.replace(" Zae", " ZAE");
        name = name.replace("Enap", " ENAP");
        name = name.replace("Edf", "EDF");
        name = name.replace("Min", "MIN");


        if (!originName.equals(name)) {
            return name;
        } else {
            return null;
        }
    }


}

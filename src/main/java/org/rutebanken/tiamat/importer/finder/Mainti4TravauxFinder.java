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

package org.rutebanken.tiamat.importer.finder;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.okina.mainti4.mainti4apiclient.model.BtDto;
import org.rutebanken.tiamat.service.mainti4.IServiceTiamatApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class Mainti4TravauxFinder {

    private static final Logger logger = LoggerFactory.getLogger(Mainti4TravauxFinder.class);

    //On considere qu'il n'y aura qu'un seul BT pour un point/quai donne
    //La cle c'est l'id du point/quai
    //pour l'instant on met le tps de rafraichissement en dur
    private Cache<String, Optional<BtDto>> mainti4TravauxCache = CacheBuilder.newBuilder()
            .maximumSize(300000)
            .refreshAfterWrite(15, TimeUnit.MINUTES)
            .build(CacheLoader.from(this::getBtDtoFromId));

    @Autowired
    @Qualifier("mainti4serviceapilogin")
    IServiceTiamatApi mainti4ServiceLogin;

    /**
     *         { "id": 0, "label": "A Planifier" },
     *         { "id": 10, "label":  "A Realiser"},
     *         { "id": 20, "label":  "En Cours"},
     *         { "id": 25, "label":  "En Cours Avec Reserve"},
     *         { "id": 30, "label":  "Realiser"},
     *         { "id": 40, "label":  "Valider Par Demandeur"},
     *         { "id": 50, "label":  "Valider Par Responsable"}
     */
    private final List<String> mstEtatsBT = Arrays.asList("0", "10", "20", "25", "30", "40", "50");

    /**
     * Le cache loader
     *
     * @return le cache loader
     */
//    private CacheLoader<String, Optional<BtDto>> getCacheLoader() {
//        return new CacheLoader<String, Optional<BtDto>>() {
//            @Override
//            public Optional<BtDto> load(String rId) {
//                logger.info("Passage dans cache loader !");
//                // Warning : must never return null  !
//                //Recupere tous les BTS selon les etats
//                List<BtDto> listeTravaux = mainti4ServiceLogin.searchBTFromIds(exportParamsBuilder.getBtStateList());
//                //S'il y a des travaux on ne garde celui qui correspond a l'id
//                if (listeTravaux != null && !listeTravaux.isEmpty()) {
//                    //Parcours les travaux
//                    for (BtDto trav : listeTravaux) {
//                        String lsCodeBT = trav.getTopologie().getCode(); //Code id
//                        //Si correspond a l'id donne
//                        if (rId.equals(lsCodeBT)) {
//                            return Optional.of(trav);
//                        }
//                    }
//                }
//                //Si on arrive la, alors l'id n'a pas ete trouve
//                return Optional.empty();
//            }
//        };
//    }

    @PostConstruct
    public void init() {
        //Mise a jour du cache
        updateCache();
    }

    /**
     * Cette fonction est utilisee pour la creaction du cacheloader.
     * Ce dernier va redemander les travaux et tenter de trouver l'id correspondant dans la liste s'il n'y est pas
     * @param rsId : id du PA/QU tel que attendu dans TIAMAT !
     * @return
     */
    private Optional<BtDto> getBtDtoFromId(String rsId) {
        logger.info("Passage dans cache loader !");
        //TODO: attention, methode searchBTFromCode non implementee pour l'instant, ca renvoie null
        //Recupere les BTs lie a un code donne
        List<BtDto> listeTravaux = mainti4ServiceLogin.searchBTFromCode(rsId);
        //S'il y a des travaux on ne garde celui qui correspond a l'id
        if (listeTravaux != null && !listeTravaux.isEmpty()) {
            //Parcours les travaux
            for (BtDto trav : listeTravaux) {
                String lsCodeBT = trav.getTopologie().getCode(); //Code id
                //Si correspond a l'id donne
                if (rsId.equals(lsCodeBT)) {
                    return Optional.of(trav);
                }
            }
        }
        //Si on arrive la, alors l'id n'a pas ete trouve
        return Optional.empty();
    }

    /**
     * Mise a jour du cache, on ne vide pas le cache avant
     */
    public void updateCache() {
        logger.info("Mise a jour du cache mainti4");
        List<BtDto> listeTravaux = mainti4ServiceLogin.searchBTFromIds(mstEtatsBT);
        //S'il y a des travaux on ne garde celui qui correspond a l'id
        if (listeTravaux != null && !listeTravaux.isEmpty()) {
            //Parcours les travaux
            for (BtDto trav : listeTravaux) {
                String lsCodeBT = trav.getTopologie().getCode(); //Code id
                mainti4TravauxCache.put(lsCodeBT, Optional.of(trav));
            }
        }
        logger.info("Fin de mise a jour du cache mainti4");
    }

}

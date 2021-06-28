package org.rutebanken.tiamat.service.mainti4;

import com.okina.mainti4.mainti4apiclient.ApiClient;
import com.okina.mainti4.mainti4apiclient.api.TopologiesApi;
import com.okina.mainti4.mainti4apiclient.model.BtDto;
import com.okina.mainti4.mainti4apiclient.model.EtatBT;
import com.okina.mainti4.mainti4apiclient.model.TopologieDto;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.service.IServiceApiLogin;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public interface IServiceTiamatApi extends IServiceApiLogin {
    /**
     * Recupere la classe ApiClient afin de generer les clients pour effectuer les appels l'API
     * @return la classe ApiClient
     */
    ApiClient getApi();

    /**
     * Cree un point d'arret (PA) dans Tiamat
     * Attention, un PA dans Tiamat correspond a un quai dans RIMO
     * @param rQuay : quai
     * @param rsCodeParent : code du parent (le code du StopPlace)
     * @return l'objet cree
     */
    TopologieDto createPA(Quay rQuay, String rsCodeParent);

    /**
     * Cherche les Bons de travaux selon une liste d'etats definis
     * @param rlstEtats : liste d'etats
     * @return renvoie la liste de bons de travaux
     */
    List<BtDto> searchBT(List<EtatBT> rlstEtats);

    /**
     * Cherche les Bons de travaux selon les codes de liste d'etats definis
     * @param rlstEtats avec uniquement les codes
     * @return renvoie la liste de bons de travaux
     */
    List<BtDto> searchBTFromIds(List<String> rlstEtats);

    /**
     * Recupere la photo d'un quai (PA cote MAINTI4)
     * @param rQuay : Le quai
     */
    BufferedImage photoByCode(Quay rQuay);

    /**
     * Recupere la photo d'un point d'arret (AR cote MAINTI4)
     * @param rStopPlace : Le point d'arret
     */
    BufferedImage photoByCode(StopPlace rStopPlace);
}

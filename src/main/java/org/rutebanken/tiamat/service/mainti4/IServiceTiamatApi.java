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

    TopologieDto getTopoPAFromQuay(Quay rQuay) throws Exception ;

    TopologieDto getTopoPAQUFromQuay(Quay rQuay) throws Exception ;

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
     * Cherche les travaux concernant un code donne
     * @param rCode : le code concerne
     * @return une liste d'etats travaux mainti4
     */
    List<BtDto> searchBTFromCode(String rCode);

    /**
     * Cherche les travaux concernant un id de topo donne
     * @param rIdTopo : id topologie
     * @return une liste d'etats travaux mainti4
     */
    List<BtDto> searchBTFromIdTopo(String rIdTopo);


    /**
     * Recupere la photo d'un quai (PA cote MAINTI4)
     * @param rQuay : Le quai
     */
    BufferedImage getPhoto(Quay rQuay);

    /**
     * Recupere la photo d'un point d'arret (AR cote MAINTI4)
     * @param rStopPlace : Le point d'arret
     */
    BufferedImage getPhoto(StopPlace rStopPlace);

    /**
     * Recupere l'url de la fiche concernee dans mainti4 d'apres l'id du point d'arret
     * @param rStopPlace : objet stopplace
     * @return l'url de la fiche de la topologie
     */
    String getUrlFromIdStopPlace(StopPlace rStopPlace);

    /**
     * Recupere l'url de la fiche concernee dans mainti4 d'apres l'id du quai
     * @param rQuay : objet quay
     * @return l'url de la fiche de la topologie
     */
    String getUrlFromIdQuay(Quay rQuay);

    String getARCodeNameFromCode(String rsCode);

    String getPACodeNameFromCode(String rsCode);

    String getPAQUCodeNameFromCode(String rsCode);
}

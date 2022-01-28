package org.rutebanken.tiamat.service.mainti4;

import com.okina.mainti4.mainti4apiclient.ApiClient;
import com.okina.mainti4.mainti4apiclient.api.AccountApi;
import com.okina.mainti4.mainti4apiclient.api.BTsApi;
import com.okina.mainti4.mainti4apiclient.api.DocumentsApi;
import com.okina.mainti4.mainti4apiclient.api.FilesApi;
import com.okina.mainti4.mainti4apiclient.api.TopologiesApi;
import com.okina.mainti4.mainti4apiclient.model.BTFilter;
import com.okina.mainti4.mainti4apiclient.model.BtDto;
import com.okina.mainti4.mainti4apiclient.model.DocumentFilter;
import com.okina.mainti4.mainti4apiclient.model.ETypeTopo;
import com.okina.mainti4.mainti4apiclient.model.EtatBT;
import com.okina.mainti4.mainti4apiclient.model.LeafletLatLng;
import com.okina.mainti4.mainti4apiclient.model.LoginModel;
import com.okina.mainti4.mainti4apiclient.model.PieceJointeDto;
import com.okina.mainti4.mainti4apiclient.model.TopologieDto;
import com.okina.mainti4.mainti4apiclient.model.TopologieFilter;
import com.okina.mainti4.mainti4apiclient.model.TypePieceJointe;
import com.okina.mainti4.mainti4apiclient.model.TypeTopoDto;
import org.locationtech.jts.geom.Point;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.rest.graphql.helpers.KeyValueWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service(value = "mainti4serviceapilogin")
public class Mainti4ServiceApiLogin implements IServiceTiamatApi {

    private static final Logger logger = LoggerFactory.getLogger(Mainti4ServiceApiLogin.class);

    private static final String BEARER = "Bearer";

    @Value("${mainti4-client.authname}")
    private String authname;

    @Value("${mainti4-client.username}")
    private String username;

    @Value("${mainti4-client.password}")
    private String password;

    private String token; //stockage du token d'authentification

    private ApiClient api; //Classe de type 'configuration' et 'factory' pour les interfaces d'appel a l'API MAINTI4
    private AccountApi apiAccount; //interface d'appel pour connexion a l'API MAINTI4

    @PostConstruct
    public void postConstruct() {
        logger.info("Demarrage Service Mainti4");
        initApi();
        try {
            initLogin();
        } catch (Exception e) {
            logger.error("Erreur de connexion a l'API Mainti4 : {}", e.getMessage());
        } finally {
            logger.info("Fin Demarrage Service Mainti4");
        }
    }

    @Override
    public void initLogin() throws Exception {
        //Cree les credentials
        LoginModel login = new LoginModel();
        login.setUserName(username);
        login.setPassword(password);
        //Effectue l'appel sur l'api distante
        File response;
        try {
            logger.info("Initialisation de la connexion vers l'API MAINTI4 pour le user : {}", login.getUserName());
            //Connexion et recuperation du token
            response = apiAccount.accountLogin(login);
            //Affecte la valeur du token
            token = response.toString();
            logger.info("Token recupere pour les appels vers l'api MAINTI4 : [{}]", token);
            //Initialisation de la factory
            initApi();
        } catch (Exception err) {
            //Si erreur login on aura "unknown login" quelque part dans la reponse
            //Si erreur password on aura "wrong password" quelque part dans la reponse
            throw new Exception("Probleme de connexion sur l'API MAINTI4 ! : " + err.getMessage());
        }
    }

    @PreDestroy
    @Override
    public void logout() throws Exception {
        try {
            logger.info("Deconnexion API MAINTI4");
            //Deconnexion
            apiAccount.accountLogOff();
        } catch (Exception err) {
            throw new Exception("Probleme de deconnexion sur l'API MAINTI4 ! : " + err.getMessage());
        }
    }

    @Override
    public boolean hasToken() {
        return token != null && !token.isEmpty();
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public ApiClient getApi() {
        return this.api;
    }

    @Override
    public TopologieDto createPA(Quay rQuay, String rsCodeParent) {
        TopologiesApi topologies;
        try {
            if (rsCodeParent == null) {
                logger.warn("Pas de code parent, impossible de creer le PA dans Mainti4");
                return null;
            }
            String lsCode = getARCodeNameFromCode(rsCodeParent);
            logger.debug("Cherche la topologie [{}]", lsCode);
            topologies = getApi().buildClient(TopologiesApi.class);
            TopologieDto topo = getTopoByCode(topologies, lsCode);
            //on attend qu'une seule topo sinon c'est pas normal
            if (topo != null) {
                //Cree le quai okina sous forme de point d'arret chez mainti4
                TopologieDto lPA = this.createPA(topologies, topo, rQuay);
                //Cree le quai fictif
                this.createQuai(topologies, lPA);
                //Renvoie le PA de base
                return lPA;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Erreur de traitement lors de l'appel api tiamat ", e);
            return null;
        }
    }

    @Override
    public TopologieDto getTopoPAFromQuay(Quay rQuay)  throws Exception  {
        //Tente de recuperer le code
        String lsCode = KeyValueWrapper.extractCodeFromKeyValues(rQuay.getKeyValues(), "P"+rQuay.getPublicCode());
        //Construit le code cote MAINTI4
        String lsCodePA = getPACodeNameFromCode(lsCode); // Code du PA ou se trouve la photo
        logger.debug("Cherche la topologie [{}]", lsCode);
        TopologiesApi topologies = getApi().buildClient(TopologiesApi.class);
        return getTopoByCode(topologies, lsCodePA);
    }

    @Override
    public TopologieDto getTopoPAQUFromQuay(Quay rQuay)  throws Exception  {
        //Tente de recuperer le code
        String lsCode = KeyValueWrapper.extractCodeFromKeyValues(rQuay.getKeyValues(), "P"+rQuay.getPublicCode());
        //Construit le code cote MAINTI4
        String lsCodePAQU = getPAQUCodeNameFromCode(lsCode); // Code du PA ou se trouve la photo
        logger.debug("Cherche la topologie [{}]", lsCode);
        TopologiesApi topologies = getApi().buildClient(TopologiesApi.class);
        return getTopoByCode(topologies, lsCodePAQU);
    }

    @Override
    public List<BtDto> searchBT(List<EtatBT> rlstEtats) {
        LocalDateTime debut = LocalDateTime.now().minusMonths(6); //debut = date actuelle - 6 mois
        LocalDateTime fin   = LocalDateTime.now().plusMonths(12); //fin = date actuelle + 12 mois
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        try {
            if (rlstEtats == null || rlstEtats.isEmpty()) {
                throw new Exception("searchBT-Aucun etat fourni en parametre !");
            }
            BTsApi bts = getApi().buildClient(BTsApi.class);
            //Filtre de recherche des bons de travaux uniquement par etat pour l'instant
            BTFilter filterBts = new BTFilter();
            //On alimente la liste d'etats
            List<EtatBT> lstEtats = new ArrayList<>(rlstEtats);
            //Affecte filtre de recherche
            filterBts.setEtats(lstEtats);
            //On doit specifier une date de debut et de fin pour que ca fonctionne
            //a noter que le champ DateFiltre n'est pas utilise. Il est cependant explique dans le fichier
            //mainti4-swagger-spec.json et pourra servir si besoin
            //(il definit a quoi correspondent ces dates de debut/fin dans la recherche)
            filterBts.setDateDebut(debut);
            filterBts.setDateFin(fin);
            //Effectue la recherche et renvoie le resultat
            return bts.bTsSearch(filterBts);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<BtDto> searchBTFromIds(List<String> rlstEtats) {
        if (rlstEtats == null) {
            return null;
        }
        List<EtatBT> llstBTs = new ArrayList<>();
        //Alimente liste d'etat
        for (String idState: rlstEtats) {
            llstBTs.add(EtatBT.fromValue(Integer.valueOf(idState)));
        }
        //Effectue la recherche
        return searchBT(llstBTs);
    }

    @Override
    public List<BtDto> searchBTFromCode(String rCode) {
        return null;
    }

    @Override
    public List<BtDto> searchBTFromIdTopo(String rIdTopo) {
        return null;
    }

    @Override
    public BufferedImage getPhoto(Quay rQuay) {
        //Tente de recuperer le code
        String lsCode = KeyValueWrapper.extractCodeFromKeyValues(rQuay.getKeyValues(), "P"+rQuay.getPublicCode());
        //Construit le code cote MAINTI4
        String lsCodePA = getPACodeNameFromCode(lsCode); // Code du PA ou se trouve la photo
        //Renvoie la photo
        return getPhotoByCode(lsCodePA);
    }

    @Override
    public BufferedImage getPhoto(StopPlace rStopPlace) {
        //Tente de recuperer le code
        String lsCode = KeyValueWrapper.extractCodeFromKeyValues(rStopPlace.getKeyValues(), "A"+rStopPlace.getPublicCode());
        //Construit le code cote MAINTI4
        String lsCodePA = getARCodeNameFromCode(lsCode); // Code du PA ou se trouve la photo
        //Renvoie la photo
        return getPhotoByCode(lsCodePA);
    }

    @Override
    public String getUrlFromIdStopPlace(StopPlace rStopPlace) {
        //Tente de recuperer le code
        String lsCode = KeyValueWrapper.extractCodeFromKeyValues(rStopPlace.getKeyValues(), "A"+rStopPlace.getPublicCode());
        //Construit le code cote MAINTI4
        String lsCodePA = getARCodeNameFromCode(lsCode); // Code du PA ou se trouve la photo
        return getUrlFromIdTopo(lsCodePA);
    }

    @Override
    public String getUrlFromIdQuay(Quay rQuay) {
        //Tente de recuperer le code
        String lsCode = KeyValueWrapper.extractCodeFromKeyValues(rQuay.getKeyValues(), "P"+rQuay.getPublicCode());
        //Construit le code cote MAINTI4
        String lsCodePA = getPACodeNameFromCode(lsCode); // Code du PA ou se trouve la photo
        //Renvoie la photo
        return getUrlFromIdTopo(lsCodePA);
    }


    /**
     * Construit une url selon l'id d'une topologie
     * Les url sont construites a partir de l'id technique
     * @param rIdTopo : id de la topologie
     * @return l'url correspondant à la topologie dans mainti4
     */
    private String getUrlFromIdTopo(String rIdTopo) {
        String lsURL = null;

        if (rIdTopo != null) {
            //Recupere la topo
            logger.debug("Cherche la topologie [{}]", rIdTopo);
            TopologiesApi topologies = getApi().buildClient(TopologiesApi.class);
            TopologieDto topo;
            try {
                topo = getTopoByCode(topologies, rIdTopo);
                if (topo != null) {
                    //Construit l'url sous la forme :
                    //"https://agglolarochelletest.mainti4.com/#/topologies/topologie/consulter/8030/0/"
                    lsURL = getApi().getBasePath()+"/#/topologies/topologie/consulter/"+topo.getId()+"/0/";
                    logger.debug("URL construite pour la topologie [{}] : [{}]", rIdTopo, lsURL);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return lsURL;
    }

    /**
     * Initialisation de la classe ApiClient sur le modele d'interface AccountApi
     */
    private void initApi() {
        //Si pas encore de token on initialise uniquement apiAccount avec une ApiClient vierge
        //on pourra ainsi recuperer le token
        if (token == null) {
            logger.info("Initialisation ApiAccount pour authentification");
            //Initialisation du client type ApiAccount
            apiAccount = new ApiClient().buildClient(AccountApi.class);
        } else {
            //Sinon on initialise le ApiClient et le ApiAccount d'apres le client configure !
            logger.info("Initialisation ApiClient et AccountApi sur base token [{}]", token);
            api = new ApiClient(authname, getApiKeyFromToken(token));
            apiAccount = api.buildClient(AccountApi.class);
        }
    }



    /**
     * Construit la cle d'api que l'on devra fournir a la classe ApiClient
     * Cette cle sera injectee dans le champ Authorization du header
     * @param rsToken Le token
     * @return la cle d'API
     */
    private String getApiKeyFromToken(String rsToken) {
        return (rsToken != null) ? BEARER + " " + rsToken : "";
    }

    /**
     * Renvoie le nom de code d'un Arrêt dans tiamat d'apres le code dans rimo
     * @param rsCode : code dans rimo
     * @return  nom de code dans tiamat d'apres le code public dans rimo
     */
    @Override
    public String getARCodeNameFromCode(String rsCode) {
        return (rsCode != null) ? rsCode + "AR" : "";
    }

    /**
     * Renvoie le nom de code d'un PA dans tiamat d'apres le code dans rimo
     * @param rsCode : code dans rimo
     * @return  nom de code dans tiamat d'apres le code public dans rimo
     */
    @Override
    public String getPACodeNameFromCode(String rsCode) {
        return (rsCode != null) ? rsCode + "PA" : "";
    }

    /**
     * Renvoie le nom de code d'un quai de PA dans tiamat d'apres le code dans rimo
     * @param rsCode : code dans rimo
     * @return  nom de code dans tiamat d'apres le code public dans rimo
     */
    @Override
    public String getPAQUCodeNameFromCode(String rsCode) {
        String lsCode = getPACodeNameFromCode(rsCode);
        return (lsCode != null) ? lsCode + "QU" : "";
    }

    /**
     * Cree le quai sous forme de point d'arret dans tiamat4
     * @param rTopologies : Topologie
     * @param rtopoParent : Topologie parente
     * @param rQuay : Le quai
     * @return renvoie la topo cree
     */
    private TopologieDto createPA(TopologiesApi rTopologies, TopologieDto rtopoParent, Quay rQuay) {
        try {
            String lsCodeQuay = KeyValueWrapper.extractCodeFromKeyValues(rQuay.getKeyValues(), "P"+rQuay.getPublicCode());
            lsCodeQuay = getPACodeNameFromCode(lsCodeQuay);
            logger.debug("Creation PA Rimo [{}] -> Quai Tiamat [{}]", rQuay.getPublicCode(), lsCodeQuay);
            TopologieDto newTopo = new TopologieDto();
            newTopo.setCode(lsCodeQuay);
            newTopo.setLibelleOrigine(rtopoParent.getLibelle());
            newTopo.setIdTopoParent(rtopoParent.getId());
            newTopo.setDateCreation(LocalDateTime.now().toString());
            newTopo.setFamilleTopologie(1L);
            Point location = rQuay.getCentroid();
            if (location != null) {
                newTopo.setGeolocation(new LeafletLatLng().alt(0.0E00).lat(location.getY()).lng(location.getX()));
            }
            //Il nous faut absolument une type de topo sinon on aura une valeur null qui sera rejetee par le serveur
            newTopo.setTypeTopo(getDefaultTypeTopo());
            //Creation de la topo enfant
            TopologieDto returnTopo = rTopologies.topologiesPost(newTopo);
            if (returnTopo != null) {
                logger.debug("Topologie enfant cree ! [" + returnTopo.getCodeLibelle() + "]");
            }
            return returnTopo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private TopologieDto createQuai(TopologiesApi rTopologies, TopologieDto rtopoParent) {
        try {
            logger.debug("Creation topo QUAI");
            logger.debug("    ==> " + rtopoParent.getCodeLibelle());
            TopologieDto newTopo = new TopologieDto();
            //a noter, on passe pas par getPAQUCodeNameFromCode, c'est volontaire car le code on le recupere deja formatte
            newTopo.setCode(rtopoParent.getCode()+"QU");
            newTopo.setLibelleOrigine("QUAI - " + rtopoParent.getLibelle());
            newTopo.setIdTopoParent(rtopoParent.getId());
            newTopo.setDateCreation(LocalDateTime.now().toString());
            //newTopo.setGeolocation(new LeafletLatLng().alt(0.0E00).lat(46.1598836962393).lng(-1.17963759925538));
            //Il nous faut absolument une type de topo sinon on aura une valeur null qui sera rejetee par le serveur
            newTopo.setTypeTopo(getDefaultTypeTopo());
            //Famille fonctionnelle
            newTopo.setFamilleTopologie(3L);
            //On ne cree pas les elements de la famille fonctionnelle car il faudrait renseigner les TechFicheDto
            //qu'il attend et on a pas l'info. Si on veut mettre un tableau vide ca marche pas
//            FamilleFonctionnelleDto familleFct = new FamilleFonctionnelleDto();
//            familleFct.setId(newTopo.getFamilleTopologie());
//            familleFct.setCode("QU");
//            familleFct.setLibelle("QUAI");
//            familleFct.setFichesTechnique(new ArrayList<TechFicheDto>());
//            newTopo.setFamilleFonctionnelle(familleFct);
            //Creation de la topo enfant
            TopologieDto returnTopo = rTopologies.topologiesPost(newTopo);
            if (returnTopo != null) {
                logger.debug("Topologie quai fictif cree ! [" + returnTopo.getCodeLibelle() + "]");
            }
            return returnTopo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Renvoie un type de topologie par défaut
     */
    private TypeTopoDto getDefaultTypeTopo() {
        TypeTopoDto typTopo = new TypeTopoDto();
        typTopo.setColor("#000000");
        typTopo.setDisplayName("lblEquipement");
        typTopo.setValue(ETypeTopo.NUMBER_0);
        return typTopo;
    }

    /**
     * Renvoie la topologie selon le code
     * Genere une exception s'il y a plus d'une topo et renvoie null si pas de topo trouvee
     * @param rCode : le code chez MAINTI4 (ex: 4900AR)
     * @return la topologie
     * @throws Exception
     */
    private TopologieDto getTopoByCode(TopologiesApi rTopologies, String rCode) throws Exception {
        //Filtre de recherche de la topologie
        TopologieFilter filter = new TopologieFilter();
        filter.setCode(rCode);
        filter.setTypeTopologie(ETypeTopo.NUMBER_0);
        filter.setAvecLocalisationTopo(true);
        logger.info("Topologie recherchée : " + filter);

        //Effectue la requete
        List<TopologieDto> listeTopo = rTopologies.topologiesSearch(filter);
        if (listeTopo != null && !listeTopo.isEmpty()) {
            //Si plus d'une topo c'est pas normal
            if (listeTopo.size() > 1) {
                throw new Exception("Plus d'une topo trouvee pour le code " + rCode);
            }
            logger.debug("Trouve topologie {}", rCode);
            return listeTopo.get(0);
        } else {
            logger.error("Aucune topologie trouvee");
            return null;
        }
    }


    /**
     * Recupere la photo correspondant au code dans Mainti4
     * @param rCode : Code tel qu'il est dans Mainti4
     * @return le buffer d'image
     */
    private BufferedImage getPhotoByCode(String rCode) {
        try {
            //Cree le client
            TopologiesApi topologies = getApi().buildClient(TopologiesApi.class);
            DocumentsApi docs = getApi().buildClient(DocumentsApi.class);
            FilesApi files = getApi().buildClient(FilesApi.class);
            //Recupere la topo
            TopologieDto topo = getTopoByCode(topologies, rCode);
            if (topo != null) {
                //Filtre pour recuperer infos sur la photo (le nom surtout !)
                DocumentFilter filterDoc = new DocumentFilter();
                filterDoc.setIdObjet(topo.getId());
                List<TypePieceJointe> lstTypesPJ = new ArrayList<>();
                lstTypesPJ.add(TypePieceJointe.NUMBER_4);
                filterDoc.setTypesDocuments(lstTypesPJ);
                //Recupere les infos de la photo
                List<PieceJointeDto> lstPJ = docs.documentsSearch(filterDoc);
                //Si pas de photo
                if (lstPJ == null || lstPJ.isEmpty()) {
                    System.out.println("Aucune photo trouvee pour le code " + rCode + " avec id " + topo.getId());
                    return null;
                }
                //On prend la premiere image...
                PieceJointeDto imagePA = lstPJ.get(0);
                //Recupere l'image et la renvoie
                //File image = files.filesDownload(imagePA.getFileName(), TypePieceJointe.NUMBER_4.getValue());
                return getPhotoFIXME(imagePA.getFileName(), TypePieceJointe.NUMBER_4.getValue());
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    //FIXME:pour le probleme d'image MAINTI4
    private BufferedImage getPhotoFIXME(String rFilename, Integer rTypePJ) throws Exception {
        if (rFilename == null || rTypePJ == null) {
            throw new Exception("getPhotoFIXME-parametre null");
        }
        //Prepare l'url pour l'appel
        URL apiUrl = new URL(this.getApi().getBasePath() + "/api/files/download?filename=" + rFilename + "&typePieceJointe=" + rTypePJ);
        HttpsURLConnection conn = (HttpsURLConnection) apiUrl.openConnection();
        conn.setRequestProperty("Authorization","Bearer "+getToken());
        conn.setRequestProperty("Content-Type","image/*");
        conn.setRequestProperty("Accept-Encoding","gzip, deflate, br");
        conn.setRequestMethod("GET");
        conn.connect(); //Connexion
        //Recupere l'image
        BufferedImage img = ImageIO.read(conn.getInputStream());
        conn.disconnect();
        return img;
    }

}

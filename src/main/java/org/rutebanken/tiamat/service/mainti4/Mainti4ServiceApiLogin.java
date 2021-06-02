package org.rutebanken.tiamat.service.mainti4;

import com.okina.mainti4.mainti4apiclient.ApiClient;
import com.okina.mainti4.mainti4apiclient.api.AccountApi;
import com.okina.mainti4.mainti4apiclient.model.LoginModel;
import org.rutebanken.tiamat.service.IServiceApiLogin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;

@Service(value = "mainti4serviceapilogin")
public class Mainti4ServiceApiLogin implements IServiceApiLogin {

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

}

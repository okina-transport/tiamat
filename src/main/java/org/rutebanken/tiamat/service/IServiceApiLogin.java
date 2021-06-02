package org.rutebanken.tiamat.service;

public interface IServiceApiLogin {

    /**
     * Connexion avec user/password pour obtenir le token
     * //TODO: voir si on genere des exceptions plus ciblees
     */
    void initLogin() throws Exception;

    /**
     * Deconnexion
     */
    void logout() throws Exception;

    /**
     * Determine si on a bien un token. Si ce n'est pas le cas il faudra appeler la methode initLogin
     * ATTENTION : le token peut etre tout de meme invalide
     * @return : true si on a bien un token, false sinon
     */
    boolean hasToken();

    /**
     * Renvoie le token
     * @return la chaine token
     */
    String getToken();
}

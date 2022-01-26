package org.rutebanken.tiamat.rest.exception;



public class TiamatBusinessException extends Exception {

    public static final int CODE_NON_PRECISE = 0;

    /**
     * 2 points share the same imported-id. This is forbidden
     */
    public static final int DUPLICATE_IMPORTED_ID = 1;

    /**
     * Mismatch between stop point in database and transport mode from incoming point
     */
    public static final int TRANSPORT_MODE_MISMATCH = 2;


    /**
     * Code erreur.
     */

    private final int code;

    /**
     * Constucteur avec un code erreur.
     *
     * @param code    le code technique d'erreur.
     * @param message le message d'erreur à logger.
     */
    public TiamatBusinessException(int code, String message) {
        this(code, message, false);
    }

    /**
     * Constucteur avec un code erreur.
     *
     * @param code    le code technique d'erreur.
     * @param message le message d'erreur à logger.
     */
    public TiamatBusinessException(int code, String message, boolean printStackTrace) {
        super(message);
        this.code = code;
        if (printStackTrace) {
            this.printStackTrace();
        }
    }

    /**
     * Constucteur avec un code erreur + message.
     *
     * @param code    le code technique d'erreur.
     * @param message le message d'erreur à logger.
     * @param error   l'erreur originelle
     */
    public TiamatBusinessException(int code, String message, Throwable error) {
        super(message, error);
        this.code = code;
        this.printStackTrace();
    }

    /**
     * Constucteur avec exception : on récupère le message.
     *
     * @param code le code technique d'erreur
     * @param e    l'exception à logger.
     */
    public TiamatBusinessException(int code, Exception e) {
        super(e.getMessage());
        this.code = code;
        this.printStackTrace();
    }

    public int getCode() {
        return code;
    }
}

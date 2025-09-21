package hearsay.idevice_decryption.api;

public class DatabaseConnectionException extends Exception {

    public DatabaseConnectionException() {
        super("Database connection failed");
    }

    public DatabaseConnectionException(Throwable cause) {
        this();
        initCause(cause);
    }

}

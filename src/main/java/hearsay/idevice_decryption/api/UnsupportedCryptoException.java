package hearsay.idevice_decryption.api;

public class UnsupportedCryptoException extends Exception {

    public UnsupportedCryptoException(Throwable cause) {
        super("This system does not support necessary cryptography", cause);
    }

}

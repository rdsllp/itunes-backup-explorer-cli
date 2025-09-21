package hearsay.idevice_decryption.api;

public class NotUnlockedException extends Exception {

    public NotUnlockedException() {
        super("Key bag was never unlocked");
    }

}

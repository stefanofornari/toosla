package ste.toosla.zefiro;

/**
 * An exception thrown when a login attempt to Zefiro fails.
 */
public class ZefiroLoginException extends ZefiroException {
    public ZefiroLoginException() {
        super("Not authorized");
    }

    public ZefiroLoginException(final String reason) {
        super(reason);
    }
}
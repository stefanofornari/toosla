package ste.toosla.zefiro;

/**
 * An exception thrown when a file or path is not found in Zefiro.
 */
public class ZefiroFileNotFoundException extends ZefiroException {
    public ZefiroFileNotFoundException(String message) {
        super(message);
    }
}

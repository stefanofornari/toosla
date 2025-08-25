package ste.toosla.zefiro;

/**
 * A generic exception for errors that occur while interacting with the Zefiro service.
 */
public class ZefiroException extends RuntimeException {
    public ZefiroException(String message) {
        super(message);
    }

    public ZefiroException(String message, Throwable cause) {
        super(message, cause);
    }
}
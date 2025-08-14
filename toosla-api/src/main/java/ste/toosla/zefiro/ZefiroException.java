package ste.toosla.zefiro;

public class ZefiroException extends RuntimeException {
    public ZefiroException(String message) {
        super(message);
    }

    public ZefiroException(String message, Throwable cause) {
        super(message, cause);
    }
}
package ste.toosla.zefiro;

public class ZefiroLoginException extends ZefiroException {
    public ZefiroLoginException() {
        super("Not authorized");
    }
}
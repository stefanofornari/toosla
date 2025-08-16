package ste.toosla.zefiro;

import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

public class ZefiroFileNotFoundExceptionTest {

    @Test
    public void exception_creation() {
        ZefiroFileNotFoundException exception = new ZefiroFileNotFoundException("File not found");
        then(exception.getMessage()).isEqualTo("File not found");
    }
}

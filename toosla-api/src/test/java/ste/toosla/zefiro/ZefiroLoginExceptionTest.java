package ste.toosla.zefiro;

import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

public class ZefiroLoginExceptionTest {

    @Test
    public void exception_creation() {
        then(new ZefiroLoginException()).hasMessage("Not authorized");
        then(new ZefiroLoginException("the reason")).hasMessage("the reason");
    }
}

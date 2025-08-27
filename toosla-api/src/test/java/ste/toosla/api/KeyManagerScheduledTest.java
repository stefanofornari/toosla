package ste.toosla.api;

import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ste.xtest.logging.ListLogHandler;
import ste.xtest.logging.LogAssertions;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import ste.xtest.concurrent.WaitFor;

@SpringBootTest(
    properties = {
        "toosla.keymanager.cleanup-period-ms=150",
        "toosla.keymanager.expiration-time-ms=100"
    }
)
public class KeyManagerScheduledTest {

    @Autowired
    private KeyManager keyManager;

    @BeforeEach
    public void beforeEach() {
        keyManager.clear();
    }

    @Test
    @DisplayName("expired keys are cleaned up automatically")
    public void expired_keys_are_cleaned_up_automatically() throws InterruptedException {
        System.out.println("keyManager1: " + keyManager);
        String key = keyManager.newKey("user", "secret", "validation");
        then(keyManager.containsKey(key)).isTrue();

        //
        // wait for the key to be expired and the scheduler to run
        //
        new WaitFor(1000, () -> {
            return keyManager.containsKey(key);
        });
    }

    @Test
    @DisplayName("cleanup logs expired keys")
    public void cleanup_logs_expired_keys() throws InterruptedException {
        System.out.println("keyManager2: " + keyManager);
        ListLogHandler logHandler = new ListLogHandler();
        Logger logger = Logger.getLogger(KeyManager.class.getName());
        logger.addHandler(logHandler);
        logger.setLevel(Level.INFO);

        String key = keyManager.newKey("user", "secret", "validation");
        then(keyManager.containsKey(key)).isTrue();

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            then(keyManager.containsKey(key)).isFalse();
        });

        String partialKey = key.substring(0, 3) + "..." + key.substring(key.length() - 3);
        LogAssertions.then(logHandler.getRecords())
                .containsINFO("Cleaning up expired keys")
                .containsINFO("Keys [" + partialKey + "] were expired and have been removed");
    }
}

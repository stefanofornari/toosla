/*
 * Toosla.me
 * ---------
 *
 * Copyright (C) 2025 Stefano Fornari. Licensed under the
 * EUPL-1.2 or later (see LICENSE).
 *
 * All Rights Reserved.  No use, copying or distribution of this
 * work may be made except in accordance with a valid license
 * agreement from Stefano Fornari.  This notice must be
 * included on all copies, modifications and derivatives of this
 * work.
 *
 * STEFANO FORNARI MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY
 * OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. STEFANO FORNARI SHALL NOT BE LIABLE FOR ANY
 * DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 */
package ste.toosla.api;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class KeyManagerTest {
    @Test
    public void access_key_is_expired_after_inactivity() throws Exception {
        // Given: expiry = 100ms for this test
        KeyManager keyManager = new KeyManager(100);

        String key = keyManager.newKey("user", "secret", "validation");
        then(keyManager.get(key)).isNotNull();

        // Simulate last used time long enough ago
        Thread.sleep(200);

        keyManager.cleanUp();

        // When & Then: key should be expired
        then(keyManager.get(key)).isNull();
    }

    @Test
    public void creating_key_manager_with_invalid_expiry_throws_exception() {
        // When & Then
        thenThrownBy(() -> new KeyManager(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("expirationTime must be greater than zero");

        thenThrownBy(() -> new KeyManager(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("expirationTime must be greater than zero");
    }

    @Test
    public void cleanup_removes_only_expired_keys() throws Exception {
        KeyManager keyManager = new KeyManager(100);

        String expiredKey = keyManager.newKey("expired_user", "expired_secret", "validation_expired");
        String freshKey = keyManager.newKey("fresh_user", "fresh_secret", "validation_fresh");

        // Sleep so expiredKey is expired, but freshKey is still within lifetime
        Thread.sleep(60);

        // Access freshKey now, so its lastUsed becomes "now"
        keyManager.get(freshKey);

        // Sleep enough so expiredKey is well expired, but freshKey's lastUsed is still recent (if you use 40ms, total is 100ms from creation, but only 40ms from lastUsed of freshKey)
        Thread.sleep(40);

        keyManager.cleanUp();

        then(keyManager.containsKey(expiredKey)).isFalse();
        then(keyManager.containsKey(freshKey)).isTrue();
    }

    @Test
    public void clear_clears_all_keys() {
        KeyManager keyManager = new KeyManager(10*5*1000);

        keyManager.newKey("account1", "secret1", "validationkey1");
        keyManager.newKey("account2", "secret2", "validationkey2");

        then(keyManager.entries).hasSize(2);

        keyManager.clear();

        then(keyManager.entries).isEmpty();
    }
}

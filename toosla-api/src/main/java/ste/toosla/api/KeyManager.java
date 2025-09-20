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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KeyManager {
    private static final Logger LOGGER = Logger.getLogger(KeyManager.class.getName());

    final protected List<KeyMetadata> entries = Collections.synchronizedList(new ArrayList());
    final protected long expirationTime;

    // Internal class to hold entry metadata
    private static class KeyMetadata {
        final KeyEntry entry;
        long lastUsed;

        public KeyMetadata(KeyEntry entry) {
            this.entry = entry;
            this.lastUsed = System.currentTimeMillis();
        }
    }

    /**
     * Represents an entry for an access key.
     *
     * @param accessKey The access key.
     * @param account The user account.
     * @param secret The user secret.
     * @param validationKey The validation key from Zefiro.
     */
    public static record KeyEntry(
        String accessKey,
        String account,
        String secret,
        String validationKey) {
    }

    public String newKey(final String account, final String secret, final String validationKey) {
        final String uuid = UUID.randomUUID().toString();
        entries.add(
            new KeyMetadata(
                new KeyEntry(uuid, account, secret, validationKey)
            )
        );

        return uuid;
    }

    public KeyManager(@org.springframework.beans.factory.annotation.Value("${toosla.keymanager.expiration-time-ms:300000}") long expirationTime) {
        if (expirationTime <= 0) {
            throw new IllegalArgumentException("expirationTime must be greater than zero");
        }
        this.expirationTime = expirationTime;
    }

    public long expirationTime() {
        return expirationTime;
    }

    public KeyEntry get(final String key) {
        for (KeyMetadata e: entries) {
            if (e.entry.accessKey.equals(key)) {
                e.lastUsed = System.currentTimeMillis();
                return e.entry;
            }
        }
        return null;
    }

    public boolean containsKey(String key) {
        for (KeyMetadata e: entries) {
            if (e.entry.accessKey.equals(key)) {
                return true;
            }
        }
        return false;
    }

    public void clear() {
        entries.clear();
    }

    /**
     * Removes expired keys from the internal cache.
     * This method is invoked by both tests and (optionally) the Spring @Scheduled task.
     */
    @Scheduled(fixedRateString = "${toosla.keymanager.cleanup-period-ms:300000}") // 5 min
    public void cleanUp() {
        LOGGER.info("Cleaning up expired keys");

        final long now = System.currentTimeMillis();
        List<String> expiredKeys = new ArrayList<>();

        entries.removeIf(e -> {
            if (now - e.lastUsed >= expirationTime) {
                expiredKeys.add(e.entry.accessKey());
                return true;
            }
            return false;
        });

        if (!expiredKeys.isEmpty()) {
            String partialKeys = expiredKeys.stream()
                .map(key -> key.substring(0, 3) + "..." + key.substring(key.length() - 3))
                .collect(Collectors.joining(", "));
            LOGGER.info("Keys [" + partialKeys + "] were expired and have been removed");
        }
    }

}

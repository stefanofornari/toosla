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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Manages access keys for the API.
 * This class is responsible for creating, storing, and retrieving access keys.
 */
public class KeyManager {

    final private Set<KeyEntry> entries = new HashSet();

    /**
     * Represents an entry for an access key.
     *
     * @param accessKey The access key.
     * @param account The user account.
     * @param secret The user secret.
     * @param validationKey The validation key from Zefiro.
     */
    public static record KeyEntry(String accessKey, String account, String secret, String validationKey) {
    }

    public String newKey(final String account, final String secret, final String validationKey) {
        final String uuid = UUID.randomUUID().toString();
        entries.add(new KeyEntry(uuid, account, secret, validationKey));

        return uuid;
    }

    public KeyEntry get(final String key) {
        for (KeyEntry e: entries) {
            if (e.accessKey.equals(key)) {
                return e;
            }
        }
        return null;
    }

}

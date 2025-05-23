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
package ste.toosla.ui;

import java.io.File;
import org.apache.commons.io.FileUtils;
import static org.assertj.core.api.BDDAssertions.then;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import ste.xtest.json.api.JSONAssertions;

/**
 *
 */
public class PasswordManagerTest extends TooslaTestBase {

    @Before
    @Override
    public void before() throws Exception {
        super.before();
        FileUtils.copyDirectory(new File("src/main/webapp"), localFileServer.root.toFile());
        FileUtils.copyDirectory(new File("src/test/webapp"), localFileServer.root.toFile());
        loadPage("passwd.html");
    }

    @Test
    public void save_secret() throws Exception {
        final int n = (int)exec("localStorage.length");

        exec("passwd.saveSecret('1234', { key:'key1', data: 'hello world'});");

        then(exec("localStorage.length")).isEqualTo(n+1);
        JSONObject secret1 = new JSONObject((String)exec("localStorage.getItem('toosla.passwd.secret.key1');"));
        JSONAssertions.then(secret1).contains("iv").contains("secret");

        exec("passwd.saveSecret('5678', { key:'key2', data: 'hello world'});");

        then(exec("localStorage.length")).isEqualTo(n+2);
        JSONObject secret2 = new JSONObject((String)exec("localStorage.getItem('toosla.passwd.secret.key2');"));
        JSONAssertions.then(secret2).contains("iv").contains("secret");

        //
        // iv shall be randomly generated each time
        //
        then(secret1.getJSONArray("iv").toString()).isNotEqualTo(secret2.getJSONArray("iv").toString());

        //
        // using a different pin the encrypted content shall be different
        //
        then(secret1.getJSONArray("secret").toString()).isNotEqualTo(secret2.getJSONArray("secret").toString());
    }

    @Test
    public void save_secret_sanity_check() throws Exception {
        final String[] INVALID = new String[] {"undefined", "null", "\"\"", "\" \t\"" };

        for (final String VALUE: INVALID) {
            exec(String.format("""
                e = "";
                passwd.saveSecret(%s, { key:'key1', data: 'hello world'})
                    .catch((error) => {
                        e = error.toString();
                });
            """, VALUE));
            then(exec("e")).isEqualTo("Error: pin can not be null or empty");
        }

        for (final String VALUE: INVALID) {
            exec(String.format("""
                e = "";
                passwd.saveSecret("1234", { key:%s, data: 'hello world'})
                    .catch((error) => {
                        e = error.toString();
                });
            """, VALUE));
            then(exec("e")).isEqualTo("Error: secret.key can not be null or empty");
        }
        exec("""
            e = "";
            passwd.saveSecret("1234", { data: 'hello world'})
                .catch((error) => {
                    e = error.toString();
            });
        """);
        then(exec("e")).isEqualTo("Error: secret.key can not be null or empty");

        for (final String VALUE: INVALID) {
            exec(String.format("""
                e = "";
                passwd.saveSecret("1234", { key: "key1", data: %s})
                    .catch((error) => {
                        e = error.toString();
                });
            """, VALUE));
            then(exec("e")).isEqualTo("Error: secret.data can not be null or empty");
        }
        exec("""
            e = "";
            passwd.saveSecret("1234", { key: "key1" })
                .catch((error) => {
                    e = error.toString();
            });
        """);
        then(exec("e")).isEqualTo("Error: secret.data can not be null or empty");

        for (final String VALUE: new String[] {"undefined", "null"}) {
            exec(String.format("""
                e = "";
                passwd.saveSecret("1234", %s)
                    .catch((error) => {
                        e = error.toString();
                });
            """, VALUE));
            then(exec("e")).isEqualTo("Error: secret can not be null or empty");
        }
    }

    @Test
    public void load_secret() {
    }

    @Test
    public void remove_secret() {
    }

    @Test
    public void exception_if_pin_is_too_short_or_too_long() {

    }

    @Test
    public void wrong_pin_error() {

    }

    @Test
    public void wrong_secret_object() {

    }

    @Test
    public void passwd_service_in_angular() {

    }
}
// -----------------------------------------------------------------------------
// PasswordManager
//
/*
class PasswordManager {
    constructor() {
        console.debug("PasswordManager created!");
    }

    async saveSecret(pin, secret) {
        //
        // TODO: check parameters
        //
        try {
            let encrypted = await encryptText(pin, secret.secret); // {iv: <iv>, data: <encrypted data>}
            localStorage.setItem("toosla.passwd.secrets", {
                key: secret.key,
                iv: encrypted.iv,
                data: encrypted.data
            });
        } catch (error) {
            console.error("Error encrypting secret", secret.key, error);
            console.error(error.stack);
        }
    }

    async deriveKeyFromPin(pin) {
        // Use SHA-256 to create a key from the PIN
        const pin = await crypto.subtle.digest('SHA-256', encoder.encode(pin));

        // Import the hashed PIN as a CryptoKey
        return crypto.subtle.importKey(
            'raw',
            pin,
            { name: 'AES-GCM' },
            false,
            ['encrypt', 'decrypt']
        );
    }

    async encryptText(pin, text) {
        // Generate a random IV (Initialization Vector)
        const iv = crypto.getRandomValues(new Uint8Array(12));

        // Convert text to ArrayBuffer
        const encoder = new TextEncoder();
        const data = encoder.encode(text);

        // Derive key from PIN if not already derived
        const derivedKey = await this.deriveKeyFromPin(pin);

        // Encrypt the data
        const encryptedBuffer = await crypto.subtle.encrypt(
            {
                name: 'AES-GCM',
                iv: iv
            },
            derivedKey,
            data
        );

        // Return encrypted data
        return {
            iv: iv,
            data: encryptedBuffer
        };
    }

    async decryptText(encryptedData, pin, iv) {
        // Use existing derivedKey or create a new one from the PIN
        let key = derivedKey;
        if (!key) {
            key = await this.deriveKeyFromPin(pin);
            derivedKey = key; // Store for future use
        }

        // Decrypt the data
        const decryptedBuffer = await crypto.subtle.decrypt(
            {
                name: 'AES-GCM',
                iv: iv
            },
            key,
            encryptedData
        );

        // Convert ArrayBuffer back to text
        const decoder = new TextDecoder();
        return decoder.decode(decryptedBuffer);
    }

    async saveSecret(pin, secret) {
        //
        // TODO: check parameters
        //
        try {
            let encrypted = await this.encryptText(pin, secret.secret); // {iv: <iv>, data: <encrypted data>}
            localStorage.setItem("toosla.passwd.secrets", {
                key: secret.key,
                iv: encrypted.iv,
                data: encrypted.data
            });
        } catch (error) {
            console.error("Error encrypting secret", secret.key, error);
            console.error(error.stack);
        }
    }
}

const passwd1 = new PasswordManager();
*/
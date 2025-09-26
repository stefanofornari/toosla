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
import { Utils } from "./Utils.js";

export class PasswordManager {
    static ENCODER = new TextEncoder();
    static DECODER = new TextDecoder();

    static KEY_PIN = "passwd.pin";
    static KEY_SECRET_PREFIX = "passwd.secret";

    #pinUpdateListeners = [];

    constructor() {
    }

    addPinUpdateListener(listener) {
        this.#pinUpdateListeners.push(listener);
    }

    removePinUpdateListener(listener) {
        const index = this.#pinUpdateListeners.indexOf(listener);
        if (index > -1) {
            this.#pinUpdateListeners.splice(index, 1);
        }
    }

    get pin() {
        console.log(JSON.stringify(sessionStorage, null, 2));
        return sessionStorage.getItem(PasswordManager.KEY_PIN);
    }

    set pin(value) {
        sessionStorage.setItem(PasswordManager.KEY_PIN, value);
        this.#pinUpdateListeners.forEach(listener => listener(value));
    }

    includes(key) {
        const item = localStorage.getItem(PasswordManager.KEY_SECRET_PREFIX + "." + key);
        return (item !== null);
    }

    async saveSecret(pin, secret) {
        Utils.checkValue("pin", pin);
        Utils.checkObject("secret", secret);
        Utils.checkValue("secret.label", secret.label);
        Utils.checkValue("secret.data", secret.data);

        const iv = crypto.getRandomValues(new Uint8Array(12));

        const key = await this.deriveKeyFromPin(pin);

        const encrypted = new Uint8Array(await crypto.subtle.encrypt(
            {
                name: "AES-GCM",
                iv: iv
            },
            key,
            PasswordManager.ENCODER.encode(secret.data)
        ));

        localStorage.setItem(
            PasswordManager.KEY_SECRET_PREFIX + "." + secret.label, JSON.stringify({iv: [...iv], secret: [...encrypted]})
        );
    }

    /**
     * Load the secret identified by label if the PIN is correct; if provided
     * PIN is incorrect an exception is thrown
     *
     * @param {string} pin PIN used when saving the secrect
     * @param {string} label the secret identifier
     *
     * @returns {string} the unencrypted value if PIN is correct
     *
     * @throws an exception if it was not possible to load the secret (not
     *         existing, PIN incorrect, other errors)
     */
    async loadSecret(pin, label) {
        Utils.checkValue("pin", pin);
        Utils.checkValue("label", label);

        const secretPlain = localStorage.getItem(PasswordManager.KEY_SECRET_PREFIX + "." + label);

        if (!secretPlain) {
            throw new Error(`secret '${label}' not found`);
        }

        try {
            const secret = JSON.parse(secretPlain);
            const key = await this.deriveKeyFromPin(pin);
            const clearArray = new Uint8Array(await crypto.subtle.decrypt(
                {
                    name: "AES-GCM",
                    iv: new Uint8Array(secret.iv).buffer
                },
                key,
                new Uint8Array(secret.secret).buffer
            ));

            return PasswordManager.DECODER.decode(clearArray);
        } catch (error) {
            throw new Error(`unable to load secret '${label}' (${error.message})`);
        }
    }

    removeSecret(label) {
        Utils.checkValue("label", label);
        localStorage.removeItem(PasswordManager.KEY_SECRET_PREFIX + "." + label);
    }

    async deriveKeyFromPin(pin) {
        const hash = await crypto.subtle.digest("SHA-256", PasswordManager.ENCODER.encode(pin));
        return await crypto.subtle.importKey(
            "raw",
            hash,
            { name: "AES-GCM" },
            false,
            ["encrypt", "decrypt"]
        );
    }

    /**
     * Return an array containing the labels in the passwd namespace.
     * If <codep>refix</code> is provided, prefix's subkeys are returned.
     */
    labels(namespace) {
        if (!namespace) {
            namespace = "";
        } else {
            namespace = namespace.trim();
        }
        if (namespace.length>0) {
            if (!namespace.endsWith(".")) {
                namespace += ".";
            }
        }
        const prefix = PasswordManager.KEY_SECRET_PREFIX + "." + namespace;

        const ret = [];
        for (let i=0; i<localStorage.length; ++i) {
            const l = localStorage.key(i);

            if (l.startsWith(prefix)) {
                ret.push(l.substring(prefix.length));
            }
        }

        return ret;
    }
}

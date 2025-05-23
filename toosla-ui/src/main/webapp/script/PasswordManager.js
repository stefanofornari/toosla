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
    constructor() {
    }

    async saveSecret(pin, secret) {
        Utils.checkValue("pin", pin);
        Utils.checkObject("secret", secret);
        Utils.checkValue("secret.key", secret.key);
        Utils.checkValue("secret.data", secret.data);

        const iv = crypto.getRandomValues(new Uint8Array(12));

        const encoder = new TextEncoder();
        const key = await crypto.subtle.importKey(
            "raw",
            encoder.encode(pin),
            { name: "AES-GCM" },
            false,
            ["encrypt", "decrypt"]
        );

        const encrypted = new Uint8Array(await crypto.subtle.encrypt(
            { name: "AES-GCM", iv: iv },
            key,
            encoder.encode(secret.data)
        ));

        localStorage.setItem("toosla.passwd.secret." + secret.key, JSON.stringify({iv: [...iv], secret: [...encrypted]}));
    }

}
console.info("PasswordManager done!");
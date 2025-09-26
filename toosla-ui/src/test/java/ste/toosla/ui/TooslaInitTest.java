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

import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;

/**
 *
 */
public class TooslaInitTest extends TooslaTestBase {

    public TooslaInitTest() {
        page = "tooslainit.html";
    }

    @Test
    public void setup_and_sync_at_load_when_pin_already_in_session_and_correct() {
        //
        // Given PIN 123abc
        //
        exec("""
            const PIN = "123abc";
            sessionStorage.setItem('passwd.pin', PIN)
            const passwd = new PasswordManager();
            passwd.pin = PIN;
            passwd.saveSecret(PIN, {
                label: "storage.credentials",
                data: "username123:password1"
            });
        """);

        //
        // given some items in the local storage
        //
        exec("localStorage.setItem('toosla.localKey1', 'localValue1')");
        exec("localStorage.setItem('another.key', 'anotherValue')");

        //
        // and a remote storage with some content
        //
        exec("localStorage.setItem('/OneMediaHub/Toosla/data.json', '{\"toosla.remoteKey1\":\"remote value 1\"}');");

        loadPage();

        waitUntilTrue(250, "toosla.storage.getItem('remoteKey1') === 'remote value 1'");
        then((String)await("toosla.storage.changeStatus")).isEqualTo("[\"clean\"]");
        then((String)exec("toosla.storage.getItem('remoteKey1');")).isEqualTo("remote value 1");
    }

    @Test
    public void setup_and_sync_at_load_when_pin_not_in_session_and_correct() {
        //
        // given remote data
        //
        exec("localStorage.setItem('/OneMediaHub/Toosla/data.json', '{\"toosla.remoteKey1\":\"remote value 1\"}');");

        await("""
            new PasswordManager().saveSecret("123", {
                label: "pin",
                data: "123"
            })
        """);
        await("""
            new PasswordManager().saveSecret("123", {
                label: "storage.credentials",
                data: "username123:password1"
            })
        """);

        loadPage();

        then(visible("#insert-pin-dialog"));

        exec("Metro.getPlugin('#insert-pin-dialog input', 'keypad').val('123')");
        click("#insert-pin-dialog .btn-confirm");

        waitUntilTrue(250, "localStorage.getItem('toosla.remoteKey1') === 'remote value 1'");
        then((String)await("toosla.storage.changeStatus")).isEqualTo("[\"clean\"]");
    }

}

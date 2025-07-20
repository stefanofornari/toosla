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
import ste.xtest.concurrent.WaitFor;

/**
 *
 */
public class PINManagementTest extends TooslaTestBase {
    /**
     * At page load:
     * - if no pin is found in sessionStorage and no encrypted
     * pin is found in toosla.storage, show the create-pin dialog.
     * - pressing Create stores the pin encrypted in toosla.storage
     *   and in clear in locaStorage
     */
    @Test
    public void create_pin_if_not_available_ok() {
        then(visible("#create-pin-dialog")).isTrue();
        exec("Metro.getPlugin('#create-pin-dialog input', 'keypad').val('1234')");
        click("#create-pin-dialog .btn-confirm");
        then(exec("sessionStorage.getItem('toosla.passwd.pin')")).isEqualTo("1234");
        then(exec("JSON.parse(toosla.storage.getItem('toosla.passwd.secret.pin')).secret")).isNotNull();
        then(visible("#create-pin-dialog")).isFalse();

        exec("$('#create-pin-dialog')[0].showModal();"); // show the dialog again

        exec("Metro.getPlugin('#create-pin-dialog input', 'keypad').val('67890')");
        click("#create-pin-dialog .btn-confirm");
        then(exec("sessionStorage.getItem('toosla.passwd.pin')")).isEqualTo("67890");
        then(exec("JSON.parse(toosla.storage.getItem('toosla.passwd.secret.pin')).secret")).isNotNull();
        then(visible("#create-pin-dialog")).isFalse();
    }

    /**
     * If no pin is provided make sure sessionStorage has an
     * empty not null pin. The PIN is toosla.storage shall not
     * change.
     */
    @Test
    public void create_pin_if_not_available_cancel() {
        then(visible("#create-pin-dialog")).isTrue();
        exec("Metro.getPlugin('#create-pin-dialog input', 'keypad').val('1234')");
        click("#create-pin-dialog .btn-cancel");
        then(exec("sessionStorage.getItem('toosla.passwd.pin')")).isEqualTo("");
        then(visible("#create-pin-dialog")).isFalse();

        //
        // save an arbitrarily PIN
        //
        final String PIN = String.valueOf(System.currentTimeMillis());
        exec(String.format(
            "angular.element($('#create-pin-dialog')).controller('toosla').passwd.saveSecret('%1$s', {label:'pin', data:'%1$s'})",
            PIN
        ));
        exec("$('#create-pin-dialog')[0].showModal();"); // show the dialog again
        click("#create-pin-dialog .btn-cancel");
        exec(String.format("""
           pin = '';
           angular.element($('#create-pin-dialog')).controller('toosla').passwd.loadSecret('%s', 'pin')
           .then((result) => {
               pin = result;
        });
        """, PIN));

        new WaitFor(250, () -> {
            return PIN.equals(exec("pin"));
        });
        then(exec("sessionStorage.getItem('toosla.passwd.pin')")).isEqualTo("");
        then(visible("#create-pin-dialog")).isFalse();
    }

    /**
     * At page load:
     * - if pin is found in sessionStorage proceed without requesting any PIN
     */
    @Test
    public void no_pin_requested_when_in_session() {
        exec("sessionStorage.setItem('toosla.passwd.pin', '123')");
        loadPage();
        then(visible("#create-pin-dialog")).isFalse();
        then(visible("#insert-pin-dialog")).isFalse();
    }

    /**
     * At page load:
     * - if pin is not found in sessionStorage but found in toosla.storage,
     *   prompt to insert the pin
     */
    @Test
    public void pin_requested_when_not_in_session_but_in_storage() {
        final String PIN = String.valueOf(System.currentTimeMillis());
        exec(String.format(
            "angular.element($('#insert-pin-dialog')).controller('toosla').passwd.saveSecret('%1$s', {label:'pin', data:'%1$s'})",
            PIN
        ));
        loadPage();
        then(visible("#insert-pin-dialog")).isTrue();

        //
        // Invalid PIN
        //
        exec("Metro.getPlugin('#insert-pin-dialog input', 'keypad').val('1234')");
        click("#insert-pin-dialog .btn-confirm");
        then(visible("#insert-pin-dialog")).isTrue();
        new WaitFor(250, () ->
            "Insert PIN  - Incorrect PIN, try again". equals(text("#insert-pin-dialog .dialog-title"))
        );

        //
        // Valid PIN
        //
        exec("Metro.getPlugin('#insert-pin-dialog input', 'keypad').val('" + PIN + "')");
        click("#insert-pin-dialog .btn-confirm");
        then(visible("#insert-pin-dialog")).isFalse();
    }
}

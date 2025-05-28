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
public class PINManagementTest extends TooslaTestBase {
    /**
     * At page load:
     * - if no pin is found in sessionStorage and no encrypted
     * pin is found in localStorage, show the create-pin dialog.
     * - pressing Create stores the pin encrypted in localStorage
     *   and in clear in locaStorage
     */
    @Test
    public void create_pin_if_not_available_ok() {
        then(visible("#create-pin-dialog")).isTrue();
        exec("$('create-pin-dialog input').val('1234)')");
        click("#create-pin-dialog .btn-confirm");
        then(exec("sessionStorage.getItem('toosla.passwd.pin')")).isEqualTo("1234");
        then(exec("localStorage.getItem('toosla.passwd.pin')")).isEqualTo("aabbcc");
    }

    /**
     * At page load:
     * - if no pin is found in sessionStorage and no encrypted
     * pin is found in localStorage, show the create-pin dialog.
     * - pressing cancel button leaves local and session storage unchanged
     */
    @Test
    public void create_pin_if_not_available_cancel() {
    }
}

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
public class ZefiroTest extends TooslaTestBase {
    @Test
    public void zefiro_displayed_on_page() {
        then(visible("#zefiro")).isTrue();
        then((boolean)exec("toosla.getModuleController('zefiro') !== undefined;")).isTrue();
        //
        // starting in view mode
        //
        then(visible("#zefiro")).isTrue();
    }

    @Test
    public void available_toosla_buttons() {
        then(visible("#zefiro .application-action-bar button.button.toosla-btn-fullscreen")).isTrue();
        then(visible("#zefiro .application-action-bar button.button.toosla-btn-settings")).isTrue();
    }

    @Test
    public void settings_button_opens_settings_div() {
        then(visible("#zefiro .settings")).isFalse();
        click("#zefiro button.toosla-btn-settings");
        then(visible("#toosla-settings .settings")).isTrue();
        then(visible("#toosla-settings .settings input[name=\"username\"]")).isTrue();
        then(visible("#toosla-settings .settings input[name=\"password\"]")).isTrue();
    }

    @Test
    public void save_credentials_on_settings_done() {
        click("#zefiro button.toosla-btn-settings");
        exec("""
c
            $("#toosla-settings .settings input[name='username']").val("user1")
            $("#toosla-settings .settings input[name='password']").val("password1")
        """); // pick timezone
    }
}

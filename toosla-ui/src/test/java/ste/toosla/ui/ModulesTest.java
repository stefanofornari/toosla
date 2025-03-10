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
import org.junit.Before;
import org.junit.Test;
import ste.xtest.web.BugFreeWeb;

/**
 *
 */
public class ModulesTest extends BugFreeWeb {

    @Before
    public void before() throws Exception {
        super.before();
        FileUtils.copyDirectory(new File("src/main/webapp"), localFileServer.root.toFile());
        FileUtils.copyDirectory(new File("src/test/webapp"), localFileServer.root.toFile());

        loadPage("test.html");
    }

    @Test
    public void module_is_initialized() throws Exception {
        then(exec("toosla.modules()")).isNotNull().isNotEqualTo("undefined");
        then(exec("toosla.modules().length")).isEqualTo(4); // demo1 + demo2 + demo3 + toosla
        then(exec("toosla.modules()[0].demo1")).isNotNull();
        then(exec("toosla.modules()[0].demo2")).isNotNull();
        then(exec("toosla.modules()[0].demo3")).isNotNull();
        then(exec("typeof angular.element($('#demo1')).controller('demo1')")).isNotEqualTo("undefined");
        then(exec("typeof angular.element($('#demo2')).controller('demo2')")).isNotEqualTo("undefined");
        then(exec("typeof angular.element($('#demo3')).controller('demo3')")).isNotEqualTo("undefined");
    }

    @Test
    public void toosla_button_bar() throws Exception {
        then(exec("$('#demo1 .toosla-btn-settings').length")).isEqualTo(1);
        then(exec("$('#demo1 .toosla-btn-fullscreen').length")).isEqualTo(1);

        then(exec("$('#demo2 .toosla-btn-settings').length")).isEqualTo(0);
        then(exec("$('#demo2 .toosla-btn-fullscreen').length")).isEqualTo(1);
    }

    @Test
    public void move_module_settings_into_toosla_settings() throws Exception {
        then(exec("$('#toosla-settings .settings').length")).isEqualTo(2);
        then(visible("#toosla-settings .settings")).isFalse();
    }

    @Test
    public void open_settings_charms_on_click_and_show_proper_settings() throws Exception {
        //
        // Initial state
        //
        then((Boolean)exec("Metro.charms.isOpen('#toosla-settings');")).isFalse();

        //
        // Open with demo1
        //
        click("#demo1 .toosla-btn-settings");
        then((Boolean)exec("Metro.charms.isOpen('#toosla-settings');")).isTrue();
        then(visible("#toosla-settings div.settings[module|=\\'demo1\\']")).isTrue();
        then(visible("#toosla-settings div.settings[module|=\\'demo2\\']")).isFalse();
        then(visible("#toosla-settings div.settings[module|=\\'demo3\\']")).isFalse();

        //
        // Open with demo3
        //
        click("#demo3 .toosla-btn-settings");
        then((Boolean)exec("Metro.charms.isOpen('#toosla-settings');")).isTrue();
        then(visible("#toosla-settings div.settings[module|=\\'demo1\\']")).isFalse();
        then(visible("#toosla-settings div.settings[module|=\\'demo2\\']")).isFalse();
        then(visible("#toosla-settings div.settings[module|=\\'demo3\\']")).isTrue();
    }

    @Test
    public void trigger_settings_open_and_close() throws Exception {
        click("#demo1 .toosla-btn-settings");
        then(exec("angular.element($('#demo1')).controller('demo1').lastSettingsEvent")).isEqualTo("load");
        exec("angular.element($('#demo1')).controller('demo1').lastSettingsEvent = null");
        then((Boolean)exec("Metro.charms.isOpen('#toosla-settings');")).isTrue();

        click("#toosla-settings-toolbar button:nth-child(2)"); // pressing Cancel...
        then(exec("angular.element($('#demo1')).controller('demo1').lastSettingsEvent")).isNull();
        then((Boolean)exec("Metro.charms.isOpen('#toosla-settings');")).isFalse();

        click("#demo1 .toosla-btn-settings"); // open again
        click("#toosla-settings-toolbar button:nth-child(1)"); // pressing OK...
        then(exec("angular.element($('#demo1')).controller('demo1').lastSettingsEvent")).isEqualTo("save");
    }
}

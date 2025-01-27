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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ste.xtest.web.BugFreeWeb;

/**
 *
 */
public class ClockTest extends BugFreeWeb {

    @Before
    public void beforeEach() {
        errors.clear();
    }

    @After
    public void afterEach() {
        if (!errors.isEmpty()) {
            System.out.println("ERRORS\n----------\n" + errors + "\n----------");
        }
    }

    @Test
    public void clock_diplayed_on_page() {
        loadPage("src/main/webapp/index.html");
        then(exec("toosla.modules['clock']")).isNotEqualTo("undefined");
        then(visible("#clock")).isTrue();
    }

    @Test
    public void initial_timezone_from_settings_or_default() {
        loadPage("src/main/webapp/index.html");

        final String DEFAULT = (String) exec("Intl.DateTimeFormat().resolvedOptions().timeZone");
        then(exec("toosla.modules['clock'].currentTimezone")).isEqualTo(DEFAULT);

        exec("""
            toosla.storage = new StorageStub();
            toosla.storage.setItem(CONFIG_CLOCK_TIMEZONE, "Europe/Paris");
            toosla.modules.clock.setup();
        """);
        then(exec("toosla.modules.clock.currentTimezone")).isEqualTo("Europe/Paris");
    }

    @Test
    public void console_text_component() throws Exception {
        loadPage("src/main/webapp/index.html");

        exec("DateStub.fixedDate = new Date(1736676010098);");
        Thread.sleep(750); // clock updates every 500ms
        then(text(".current-date")).isEqualTo("Sunday, 12 January 2025");
        exec("DateStub.fixedDate = new Date(1718961726000);");
        Thread.sleep(750);
        then(text(".current-date")).isEqualTo("Friday, 21 June 2024");
    }

    @Test
    public void settings_button_opens_settings_div() {
        loadPage("src/main/webapp/index.html");

        then(visible("#clock .settings")).isFalse();
        click("#clock button.btn-settings");
        then(visible("#clock .settings")).isTrue();
        then((String) exec("$('#clock .settings .timezones').html()")).contains("Europe/Rome");
    }

    @Test
    public void change_timezone_settings_changes_the_time_and_save_settings() throws Exception {
        loadPage("src/main/webapp/index.html");

        exec("toosla.storage = new StorageStub(); toosla.modules.clock.setup()"); // set sturage stub and setup the module again

        exec("DateStub.fixedDate = new Date(1735686060000);"); // 20250101 000101 UTC
        click("#clock button.btn-settings");  // open settings panel
        then(visible("#clock .settings")).isTrue();
        exec("$('#clock .settings select.timezones').data('select').val('America/New_York');"); // pick timezone
        click("#clock .settings button.btn-done"); // click done button
        then(visible("#clock .settings")).isFalse();
        Thread.sleep(750); // clock updates every 500ms
        then(exec("toosla.storage.getItem(CONFIG_CLOCK_TIMEZONE)"))
                .isEqualTo("America/New_York");
        then(text(".current-date")).isEqualTo("Tuesday, 31 December 2024");
    }
}

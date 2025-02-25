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
public class ClockTest extends BugFreeWeb {

    @Before
    public void before() throws Exception {
        super.before();
        FileUtils.copyDirectory(new File("src/main/webapp"), localFileServer.root.toFile());
    }

    @Test
    public void clock_displayed_on_page() {
        loadPage("index.html");
        then(exec("toosla.modules['clock']")).isNotEqualTo("undefined");
        then(visible("#clock")).isTrue();
    }

    @Test
    public void initial_timezone_from_settings_or_default() {
        loadPage("index.html");

        final String DEFAULT = (String) exec("Intl.DateTimeFormat().resolvedOptions().timeZone");
        then(exec("toosla.modules['clock'].currentTimezone")).isEqualTo(DEFAULT);

        exec("""
            window.localStorage.setItem(CONFIG_CLOCK_TIMEZONE, "Europe/Paris");
            toosla.modules.clock.setup();
        """);
        then(exec("toosla.modules.clock.currentTimezone")).isEqualTo("Europe/Paris");
    }

    @Test
    public void set_current_date() throws Exception {
        loadPage("index.html");

        exec("DateStub.fixedDate = new Date(1736676010098);");
        Thread.sleep(750); // clock updates every 500ms
        then(exec("toosla.modules.clock.todayDate")).isEqualTo("Sunday, 12 January 2025");
        then(text(".current-date")).isEqualTo("Sunday, 12 January 2025");
        exec("DateStub.fixedDate = new Date(1718961726000);");
        Thread.sleep(750);
        then(exec("toosla.modules.clock.todayDate")).isEqualTo("Friday, 21 June 2024");
        then(text(".current-date")).isEqualTo("Friday, 21 June 2024");
    }

    @Test
    public void settings_button_opens_settings_div() {
        loadPage("index.html");

        then(visible("#clock .settings")).isFalse();
        click("#clock button.btn-settings");
        then(visible("#clock .settings")).isTrue();
        then((String) exec("$('#clock .settings .timezones').html()")).contains("Europe/Rome");
    }

    @Test
    public void change_timezone_settings_changes_the_time_and_save_settings() throws Exception {
        loadPage("index.html");

        exec("DateStub.fixedDate = new Date(1735686060000);"); // 20250101 000101 UTC
        click("#clock button.btn-settings");  // open settings panel
        then(visible("#clock .settings")).isTrue();
        exec("""
            const S = $('#clock .settings select.timezones');
            S.data('select').val('America/New_York');
            S.trigger("itemselect");
        """); // pick timezone
        click("#clock .settings button.btn-done"); // click done button
        then(visible("#clock .settings")).isFalse();
        Thread.sleep(750); // clock updates every 500ms
        then(exec("window.localStorage.getItem(CONFIG_CLOCK_TIMEZONE)"))
                .isEqualTo("America/New_York");
        then(text(".current-date")).isEqualTo("Tuesday, 31 December 2024");

        printConsole();
    }

    @Test
    public void change_timezone_settings_does_not_save_if_cancel_is_pressed() throws Exception {
        loadPage("index.html");

        exec("""
            DateStub.fixedDate = new Date(1735686060000); // 20250101 000101 UTC
            window.localStorage.setItem(CONFIG_CLOCK_TIMEZONE, "Europe/Rome");
            toosla.modules.clock.setup();
        """); // set stubs and setup the module again

        click("#clock button.btn-settings");  // open settings panel
        then(visible("#clock .settings")).isTrue();
        exec("""
            const S = $('#clock .settings select.timezones');
            S.data('select').val('America/New_York');
            S.trigger("itemselect");
        """); // pick timezone
        Thread.sleep(750); // clock updates every 500ms
        then(text(".current-date")).isEqualTo("Tuesday, 31 December 2024");
        click("#clock .settings button.btn-cancel"); // click done button
        then(visible("#clock .settings")).isFalse();
        then(exec("window.localStorage.getItem(CONFIG_CLOCK_TIMEZONE)"))
            .isEqualTo("Europe/Rome");
        then(text(".current-date")).isEqualTo("Wednesday, 1 January 2025");
    }

    @Test
    public void initialize_settings_form_with_stored_values() throws Exception {
        loadPage("index.html");
        exec("window.localStorage.setItem(CONFIG_CLOCK_TIMEZONE, 'Europe/Rome');");
        click("#clock button.btn-settings");  // open settings panel

        then(exec("$('#clock .settings select.timezones').val()")).isEqualTo("Europe/Rome");
    }
}

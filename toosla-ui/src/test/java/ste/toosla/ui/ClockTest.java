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
import ste.xtest.concurrent.WaitFor;

/**
 *
 */
public class ClockTest extends TooslaTestBase {

    @Before
    public void before() throws Exception {
        super.before();
        FileUtils.copyDirectory(new File("src/main/webapp"), localFileServer.root.toFile());
        FileUtils.copyDirectory(new File("src/test/webapp"), localFileServer.root.toFile());

        loadPage("index.html");
    }

    @Test
    public void clock_displayed_on_page() {
        then(exec("toosla.modules.getController('clock')")).isNotEqualTo("undefined");
        then(visible("#clock")).isTrue();
    }

    @Test
    public void initial_timezone_from_settings_or_default() {
        final String DEFAULT = (String) exec("Intl.DateTimeFormat().resolvedOptions().timeZone");
        then(exec("angular.element($(`#clock`)).controller('clock').currentTimezone")).isEqualTo(DEFAULT);

        exec("""
            window.localStorage.setItem("toosla.clock.timezone", "Europe/Paris");
            angular.element($(`#clock`)).controller('clock').setup();
        """);
        then(exec("angular.element($(`#clock`)).controller('clock').currentTimezone")).isEqualTo("Europe/Paris");
    }

    @Test
    public void set_current_date() throws Exception {
        exec("DateStub.fixedDate = new Date(1736676010098);");
        // clock updates every 500ms
        new WaitFor(1000, () -> {
            return "Sunday 12 January 2025".equals(exec("angular.element($(`#clock`)).controller('clock').todayDate"));
        });
        then(text(".current-date")).isEqualTo("Sunday 12 January 2025");
        exec("DateStub.fixedDate = new Date(1718961726000);");
        // clock updates every 500ms
        new WaitFor(1000, () -> {
            return "Friday 21 June 2024".equals(exec("angular.element($(`#clock`)).controller('clock').todayDate"));
        });
        then(text(".current-date")).isEqualTo("Friday 21 June 2024");
    }

    @Test
    public void settings_button_opens_settings_div() {
        then(visible("#clock .settings")).isFalse();
        click("#clock button.toosla-btn-settings");
        then(visible("#toosla-settings .settings")).isTrue();
        then((String) exec("$('#toosla-settings .settings .timezones').html()")).contains("Europe/Rome");
    }

    @Test
    public void change_timezone_settings_changes_the_time_and_save_settings() throws Exception {
        exec("DateStub.fixedDate = new Date(1735686060000);"); // 20250101 000101 UTC
        click("#clock button.toosla-btn-settings");  // open settings panel
        then(visible("#toosla-settings .settings")).isTrue();
        exec("""
            const S = $('#toosla-settings .settings select.timezones');
            S.data('select').val('America/New_York');
            S.trigger("itemselect");
        """); // pick timezone
        click("#toosla-settings button.btn-done"); // click done button
        // clock updates every 500ms
        new WaitFor(1000, () -> {
            return
                "America/New_York".equals(String.valueOf(exec("window.localStorage.getItem('toosla.clock.timezone')")))
                &&
                "Tuesday 31 December 2024".equals(text(".current-date"));
        });
    }

    @Test
    public void change_timezone_settings_does_not_save_if_cancel_is_pressed() throws Exception {
        exec("""
            DateStub.fixedDate = new Date(1735686060000); // 20250101 000101 UTC
            window.localStorage.setItem("toosla.clock.timezone", "Europe/Rome");
            angular.element($(`#clock`)).controller('clock').setup();
        """); // set stubs and setup the module again

        click("#clock button.toosla-btn-settings");  // open settings panel
        exec("""
            const S = $('#toosla-settings .settings select.timezones');
            S.data('select').val('America/New_York');
            S.trigger("itemselect");
        """); // pick timezone
        // clock updates every 500ms
        new WaitFor(1000, () -> {
            return "America/New_York".equals(exec("angular.element($(`#clock`)).controller('clock').currentTimezone"));
        });
        click("#toosla-settings button.btn-cancel"); // click cancel button
        then(exec("window.localStorage.getItem('toosla.clock.timezone')"))
            .isEqualTo("Europe/Rome");
        then(exec("angular.element($(`#clock`)).controller('clock').currentTimezone")).isEqualTo("Europe/Rome");
    }

    @Test
    public void initialize_settings_form_with_stored_values() throws Exception {
        exec("window.localStorage.setItem'toosla.clock.timezone', 'Europe/Rome');");
        click("#clock button.toosla-btn-settings");  // open settings panel
        new WaitFor(1000, () -> {
            System.out.println(exec("$('#toosla-settings .settings select.timezones').val()"));
            return "Europe/Rome".equals(exec("$('#toosla-settings .settings select.timezones').val()"));
        });
    }
}

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
public class MainPageTest extends TooslaTestBase {
    @Test
    public void version_displayed_in_main_page() throws Exception {
        loadPage();
        then(visible("#toosla")).isTrue();
        then(text("#toosla #version")).isEqualTo("@project.parent.version@,@project.version@,@maven.build.timestamp@");
    }

    @Test
    public void console_text_component() {
        loadPage();
        then(visible("#console")).isTrue();

        exec("console.log('log1', 'log2')");
        String[] logs = text("#console .content").split(" ");
        then(logs[0]).matches("[0-9]+"); then(logs[1]).isEqualTo("(L)");
        then(logs[2]).isEqualTo("log1"); then(logs[3]).isEqualTo("log2");

        exec("console.info('info1', 'info2')");
        logs = text("#console .content").split(" ");
        then(logs[0]).matches("[0-9]+"); then(logs[1]).isEqualTo("(I)");
        then(logs[2]).isEqualTo("info1"); then(logs[3]).isEqualTo("info2");

        exec("console.error('err1', 'err2')");
        logs = text("#console .content").split(" ");
        then(logs[0]).matches("[0-9]+"); then(logs[1]).isEqualTo("(E)");
        then(logs[2]).isEqualTo("err1"); then(logs[3]).isEqualTo("err2");

        exec("console.warn('warn1', 'warn2')");
        logs = text("#console .content").split(" ");
        then(logs[0]).matches("[0-9]+"); then(logs[1]).isEqualTo("(W)");
        then(logs[2]).isEqualTo("warn1"); then(logs[3]).isEqualTo("warn2");

        exec("console.debug('dbg1', 'dbg2')");
        logs = text("#console .content").split(" ");
        then(logs[0]).matches("[0-9]+"); then(logs[1]).isEqualTo("(D)");
        then(logs[2]).isEqualTo("dbg1"); then(logs[3]).isEqualTo("dbg2");
    }

    @Test
    public void light_and_dark_mode() throws Exception {
        //
        // Initial media
        //
        initialMedia("{'prefers-color-scheme': 'light'}");
        loadPage();
        then(classes("html")).doesNotContain("dark-side");

        initialMedia("{'prefers-color-scheme': 'dark'}");
        loadPage();
        then(classes("html")).contains("dark-side");

        //
        // Switch media
        //
        darkMode(false);
        then(classes("html")).doesNotContain("dark-side");
        darkMode(true);
        then(classes("html")).contains("dark-side");
    }
}


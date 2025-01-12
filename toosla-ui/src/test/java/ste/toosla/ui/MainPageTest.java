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
import ste.xtest.web.BugFreeWeb;

/**
 *
 */
public class MainPageTest extends BugFreeWeb {

    @Test
    public void version_displayed_in_main_page() {
        loadPage("src/main/webapp/index.html");
        then(visible("#toosla")).isTrue();
        then(text("#toosla #version")).isEqualTo("${project.version}");
    }

    @Test
    public void console_text_component() {
        loadPage("src/main/webapp/index.html");
        then(visible("#console")).isTrue();

        exec("console.log('log1', 'log2')");
        then(text("#console #content")).endsWith("(L) log1 log2");
        exec("console.info('info1', 'info2')");
        then(text("#console #content")).endsWith("(I) info1 info2");
        exec("console.error('err1', 'err2')");
        then(text("#console #content")).endsWith("(E) err1 err2");
        exec("console.warn('warn1', 'warn2')");
        then(text("#console #content")).endsWith("(W) warn1 warn2");
        exec("console.debug('dbg1', 'dbg2')");
        then(text("#console #content")).endsWith("(D) dbg1 dbg2");
    }

    @Test
    public void light_and_dark_mode() throws Exception {
        loadPage("src/main/webapp/index.html");
        darkMode(true);
        then(classes("html")).contains("dark-side");
        darkMode(false);
        then(classes("html")).doesNotContain("dark-side");
    }
}

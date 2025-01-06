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
        then((boolean)exec("$('#toosla').is(':visible')")).isTrue();
        then(exec("$('#toosla #version').text()")).isEqualTo("${project.version}");
    }

}

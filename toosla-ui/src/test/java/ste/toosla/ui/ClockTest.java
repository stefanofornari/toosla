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
public class ClockTest extends BugFreeWeb {

    @Test
    public void clock_diplayed_on_page() {
        loadPage("src/main/webapp/index.html");
        then(visible("#clock")).isTrue();
    }

    @Test
    public void console_text_component() throws Exception {
        loadPage("src/main/webapp/index.html");

        exec("DateStub.fixedDate = new Date(1736676010098);");
        Thread.sleep(25);
        then(text(".current-date")).isEqualTo("Sunday, 12 January 2025");
        exec("DateStub.fixedDate = new Date(1718961726000);");
        Thread.sleep(25);
        then(text(".current-date")).isEqualTo("Friday, 21 June 2024");
    }
}

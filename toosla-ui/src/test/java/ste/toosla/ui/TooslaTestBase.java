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
import org.junit.Before;
import ste.xtest.web.BugFreeWeb;

/**
 *
 */
public class TooslaTestBase extends BugFreeWeb {

    protected String page = "index.html";

    @Before
    @Override
    public void before() throws Exception {
        super.before();
        FileUtils.copyDirectory(new File("src/main/webapp"), localFileServer.root.toFile());
        FileUtils.copyDirectory(new File("src/test/webapp"), localFileServer.root.toFile());
        loadPage(page);
    }

}

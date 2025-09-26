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
import ste.xtest.concurrent.WaitFor;
import ste.xtest.web.BugFreeWeb;

public class TooslaTestBase extends BugFreeWeb {

    private static final int DEFAULT_TIMEOUT = 250;

    protected static final String[] INVALID = new String[]{"undefined", "null", "\"\"", "\" \t\""};

    protected String page = "index.html";

    @Before
    @Override
    public void before() throws Exception {
        super.before();
        FileUtils.copyDirectory(new File("src/main/webapp"), localFileServer.root.toFile());
        FileUtils.copyDirectory(new File("src/test/webapp"), localFileServer.root.toFile());
        loadPage();
    }

    public Object await(long timeout, final String script) {
        exec(String.format("""
            __XTEST__.asyncDone = false;
            __XTEST__.asyncRet = null;

            p = (%s);

            Promise.all([p]).then((o) => {
                __XTEST__.asyncRet = o;
                __XTEST__.asyncDone = true;
            }).catch((error) => {
                __XTEST__.asyncRet = error.message;
                __XTEST__.asyncDone = true;
            });
        """, script));

        waitUntilTrue(timeout, "__XTEST__.asyncDone === true");

        return exec("__XTEST__.asyncRet");
    }

    public Object await(final String script) {
        return this.await(DEFAULT_TIMEOUT, script);
    }

    public void waitUntilTrue(final String condition) {
        waitUntilTrue(DEFAULT_TIMEOUT, condition);
    }

    public void waitUntilTrue(final long timeout, final String condition) {
        new WaitFor(timeout, () -> (Boolean)exec(condition));
    }

    /**
     * Load the page in @{code page} and wait until toosla.ready is true (within
     * a timeout).
     */
    public void loadPage() {
        loadPage(page);

        new WaitFor(()-> {
            Object o = exec("toosla.ready");
            return (o != null && (boolean)o);
        });
    }
}

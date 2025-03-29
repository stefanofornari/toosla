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
public class CalcTest extends BugFreeWeb {

    @Before
    public void before() throws Exception {
        super.before();
        FileUtils.copyDirectory(new File("src/main/webapp"), localFileServer.root.toFile());
        loadPage("index.html");
    }

    @Test
    public void clock_displayed_on_page() {
        then(visible("#calc")).isTrue();
        then((boolean)exec("toosla.getModuleController('calc') !== undefined;")).isTrue();
    }

    @Test
    public void available_toosla_buttons() {
        then(visible("#calc .application-action-bar button.button.toosla-btn-fullscreen")).isTrue();
    }

    @Test
    public void typing_keys_updates_expression() throws Exception {
        exec("$('#calc .calc-button')[1].click()");  // 8
        then(val(".calc-typed input")).isEqualTo("8");
        exec("$('#calc .calc-button')[8].click()");  // 1
        then(val(".calc-typed input")).isEqualTo("81");
        exec("""
             $('#calc .calc-button')[7].click();   // +
             $('#calc .calc-button')[6].click();   // 6
             $('#calc .calc-button')[2].click();   // 9
             $('#calc .calc-button')[19].click();  // /
             $('#calc .calc-button')[9].click();   // 2
             $('#calc .calc-button')[11].click();  // -
             $('#calc .calc-button')[10].click();  // 3
             $('#calc .calc-button')[15].click();  // *
             $('#calc .calc-button')[4].click();   // 4
             $('#calc .calc-button')[12].click();  // .
             $('#calc .calc-button')[5].click();   // 5
             $('#calc .calc-button')[0].click();   // 7
             $('#calc .calc-button')[19].click();  // /
             $('#calc .calc-button')[16].click();  // (
             $('#calc .calc-button')[0].click();   // 7
             $('#calc .calc-button')[11].click();  // -
             $('#calc .calc-button')[4].click();   // 4
             $('#calc .calc-button')[17].click();  // )
        """);
        then(val(".calc-typed input")).isEqualTo("81+69/2-3*4.57/(7-4)");
    }

    @Test
    public void pressing_equal_calculates_the_result() throws Exception {
        exec("""
             $('#calc .calc-button')[9].click();  // 2
             $('#calc .calc-button')[15].click(); // *
             $('#calc .calc-button')[5].click();  // 5
             $('#calc .calc-button')[20].click(); // =
             """);

        Thread.sleep(500); // angular takes some time to update the ui...

        then(val(".calc-typed input")).isEqualTo("2*5");
        then(text("#calc .calc-operation")).isEqualTo("10");
    }

    @Test
    public void correct_floating_point_calculation() throws Exception {
        exec("""
             $('#calc .calc-button')[13].click(); // 0
             $('#calc .calc-button')[12].click(); // .
             $('#calc .calc-button')[1].click();  // 8
             $('#calc .calc-button')[11].click(); // -
             $('#calc .calc-button')[13].click(); // 0
             $('#calc .calc-button')[12].click(); // .
             $('#calc .calc-button')[8].click();  // 1
             $('#calc .calc-button')[20].click(); // =
             """);

        Thread.sleep(500); // angular takes some time to update the ui...

        then(val(".calc-typed input")).isEqualTo("0.8-0.1");
        then(text("#calc .calc-operation")).isEqualTo("0.7");
    }

    @Test
    public void pressing_C_cleans_expressions_and_result() throws Exception {
        exec("""
             $('#calc .calc-button')[9].click();  // 2
             $('#calc .calc-button')[15].click(); // *
             $('#calc .calc-button')[5].click();  // 5
             $('#calc .calc-button')[3].click();  // C
             """);

        Thread.sleep(250); // angular takes some time to update the ui...

        then(val(".calc-typed input")).isEqualTo("");
        then(text("#calc .calc-operation")).isEqualTo("0");
    }

    @Test
    public void percent_sign_calculation() throws Exception {
        exec("""
             $('#calc .calc-button')[9].click();  // 2
             $('#calc .calc-button')[14].click(); // %
             $('#calc .calc-button')[20].click(); // =
             """);

        Thread.sleep(250); // angular takes some time to update the ui...

        then(val(".calc-typed input")).isEqualTo("2%");
        then(text("#calc .calc-operation")).isEqualTo("0.02");

        exec("""
             $('#calc .calc-button')[3].click();  // C
             $('#calc .calc-button')[9].click();  // 2
             $('#calc .calc-button')[11].click(); // -
             $('#calc .calc-button')[8].click();  // 1
             $('#calc .calc-button')[13].click(); // 0
             $('#calc .calc-button')[14].click(); // %
             $('#calc .calc-button')[20].click(); // =
             """);

        Thread.sleep(250); // angular takes some time to update the ui...

        then(val(".calc-typed input")).isEqualTo("2-10%");
        then(text("#calc .calc-operation")).isEqualTo("1.8");
    }

    @Test
    public void switch_sign() throws Exception {
        //
        // empty input
        //
        exec("$('#calc .calc-button')[18].click();");
        then(val(".calc-typed input")).isEqualTo("(-");

        //
        // once adds (-
        //
        exec("""
             $('#calc .calc-button')[ 3].click();  // C
             $('#calc .calc-button')[ 9].click();  // 2
             $('#calc .calc-button')[18].click();  // +/-
            """);

        then(val(".calc-typed input")).isEqualTo("(-2");

        //
        // pressing it again, removes (-
        //
        exec("$('#calc .calc-button')[18].click();");  // +/-
        then(val(".calc-typed input")).isEqualTo("2");

        //
        // in the middle of an expression
        //
        exec("""
             $('#calc .calc-button')[15].click();  // *
             $('#calc .calc-button')[1].click();   // 8
             $('#calc .calc-button')[1].click();   // 8
             $('#calc .calc-button')[18].click();; // +/-
            """);
        then(val(".calc-typed input")).isEqualTo("2*(-88");
    }

    @Test
    public void backspace_deletes_character_at_cursor() throws Exception {
        exec("""
             $('#calc .calc-button')[ 9].click(); // 2
             $('#calc .mif-arrow-left').click();  // backspace
            """);

        Thread.sleep(250); then(val(".calc-typed input")).isEqualTo("");

        exec("""
             $('#calc .calc-button')[ 9].click(); // 2
             $('#calc .calc-button')[ 9].click(); // 2
             $(".calc-typed input")[0].setSelectionRange(1, 1);
             $('#calc .mif-arrow-left').click();  // backspace
            """);

        Thread.sleep(250); then(val(".calc-typed input")).isEqualTo("2");
    }

    @Test
    public void show_error_if_expression_is_invalid() throws Exception {
        exec("""
             $('#calc .calc-button')[16].click(); // (
             $('#calc .calc-button')[ 9].click(); // 2
             $('#calc .calc-button')[20].click(); // =
            """);

        Thread.sleep(250);
        then(text("#calc .calc-operation")).isEqualTo("Error");
    }
}

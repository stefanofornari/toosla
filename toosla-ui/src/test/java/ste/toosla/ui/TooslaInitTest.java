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
import org.junit.Before;
import org.junit.Test;
import ste.xtest.concurrent.WaitFor;

/**
 *
 */
public class ZefiroTest extends TooslaTestBase {

    @Override
    @Before
    public void before() throws Exception {
        super.before();
        exec("sessionStorage.setItem('passwd.pin', '1234');");
    }

    @Test
    public void zefiro_displayed_on_page() {
        then(visible("#zefiro")).isTrue();
        then((boolean)exec("toosla.getModuleController('zefiro') !== undefined;")).isTrue();
        //
        // starting in view mode
        //
        then(visible("#zefiro")).isTrue();
    }

    @Test
    public void available_toosla_buttons() {
        then(visible("#zefiro .application-action-bar button.button.toosla-btn-fullscreen")).isTrue();
        then(visible("#zefiro .application-action-bar button.button.toosla-btn-settings")).isTrue();
    }

    @Test
    public void settings_button_opens_settings_div() {
        then(visible("#zefiro .settings")).isFalse();
        click("#zefiro button.toosla-btn-settings");
        then(visible("#toosla-settings .settings")).isTrue();
        then(visible("#toosla-settings .settings input[name=\"username\"]")).isTrue();
        then(visible("#toosla-settings .settings input[name=\"password\"]")).isTrue();
    }

    @Test
    public void load_existing_credentials_on_opening_settings() throws Exception {
        exec("""
            const passwd = angular.element($('#zefiro')).controller('zefiro').passwd;
            passwd.saveSecret('1234', {label: 'zefiro.user1', data: 'password1'});
        """);
        Thread.sleep(250);
        click("#zefiro button.toosla-btn-settings");
        Thread.sleep(250);
        then(val("#toosla-settings .settings input[name=username]")).isEqualTo("user1");
        then(val("#toosla-settings .settings input[name=password]")).isEqualTo("password1");

        exec("passwd.removeSecret('zefiro.user1')");
        click("#zefiro button.toosla-btn-settings");
        Thread.sleep(250);
        then(val("#toosla-settings .settings input[name=username]")).isEmpty();
        then(val("#toosla-settings .settings input[name=password]")).isEmpty();
    }

    @Test
    public void save_credentials_on_settings_done() throws Exception {
        click("#zefiro button.toosla-btn-settings");
        exec("""
            const ctrl = angular.element($('#zefiro')).controller('zefiro');
            angular.element($("#toosla-settings .settings input[name='username']")).val("user1").triggerHandler('input');
            angular.element($("#toosla-settings .settings input[name='password']")).val("password1").triggerHandler('input');
        """); // set credentials
        click("#toosla-settings button.btn-done"); // click done button
        then((Boolean)exec("Metro.charms.isOpen(\"#toosla-settings\");")).isFalse();
        exec("""
           var pwd = null;
           ctrl.passwd.loadSecret('1234', 'zefiro.user1')
           .then((p) => { pwd = p })
           .catch((e) => console.error(e.message, e.stack));
        """);
        new WaitFor(250, () -> {
            return "password1".equals(exec("pwd"));
        });
    }

    @Test
    public void save_credentials_replaces_existing() throws Exception {
        exec("""
            const ctrl = angular.element($('#zefiro')).controller('zefiro');
            ctrl.passwd.saveSecret('1234', { label: 'zefiro.user0', data: 'something' });
        """);
        click("#zefiro button.toosla-btn-settings");
        exec("""
            angular.element($("#toosla-settings .settings input[name='username']")).val("user1").triggerHandler('input');
            angular.element($("#toosla-settings .settings input[name='password']")).val("password1").triggerHandler('input');
        """); // set credentials
        click("#toosla-settings button.btn-done"); // click done button
        then((Boolean)exec("Metro.charms.isOpen(\"#toosla-settings\");")).isFalse();
        then(exec("ctrl.passwd.labels()")).isEqualTo("[\"zefiro.user1\",\"storage.credentials\"]");
        then(async("ctrl.passwd.loadSecret('1234', 'storage.credentials')")).isEqualTo("[\"user1:password1\"]");
    }

    @Test
    public void set_status_configured_on_settings_done() throws Exception {
        exec("const ctrl = angular.element($('#zefiro')).controller('zefiro');");
        click("#zefiro button.toosla-btn-settings");
        exec("""
            angular.element($("#toosla-settings .settings input[name='username']")).val("user1").triggerHandler('input');
            angular.element($("#toosla-settings .settings input[name='password']")).val("password1").triggerHandler('input');
        """);
        click("#toosla-settings button.btn-done"); // click done button
        then(visible("#zefiro .card-content .configure-message")).isFalse();
    }

    @Test
    public void load_credentials_at_page_load() throws Exception {
        then(visible("#zefiro .card-content .configure-message")).isTrue();
        exec("""
            const ctrl = angular.element($('#zefiro')).controller('zefiro');
            ctrl.passwd.saveSecret('1234', { label: 'zefiro.user0', data: 'something' });
        """);
        loadPage();
        then(visible("#zefiro .card-content .configure-message")).isFalse();
    }


}

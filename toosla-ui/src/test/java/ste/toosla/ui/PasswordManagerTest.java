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
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import ste.xtest.concurrent.WaitFor;
import ste.xtest.json.api.JSONAssertions;

/**
 *
 */
public class PasswordManagerTest extends TooslaTestBase {

    private static final String[] INVALID = new String[] {"undefined", "null", "\"\"", "\" \t\"" };

    @Before
    @Override
    public void before() throws Exception {
        super.before();
        loadPage("passwd.html");
    }

    @Test
    public void save_secret() throws Exception {
        final int n = (int)exec("localStorage.length");

        exec("passwd.saveSecret('1234', { label:'label1', data: 'hello world'});");

        then(exec("localStorage.length")).isEqualTo(n+1);
        JSONObject secret1 = new JSONObject((String)exec("localStorage.getItem('toosla.passwd.secret.label1');"));
        JSONAssertions.then(secret1).contains("iv").contains("secret");

        exec("passwd.saveSecret('5678', { label:'label2', data: 'hello world'});");

        then(exec("localStorage.length")).isEqualTo(n+2);
        JSONObject secret2 = new JSONObject((String)exec("localStorage.getItem('toosla.passwd.secret.label2');"));
        JSONAssertions.then(secret2).contains("iv").contains("secret");

        //
        // iv shall be randomly generated each time
        //
        then(secret1.getJSONArray("iv").toString()).isNotEqualTo(secret2.getJSONArray("iv").toString());

        //
        // using a different pin the encrypted content shall be different
        //
        then(secret1.getJSONArray("secret").toString()).isNotEqualTo(secret2.getJSONArray("secret").toString());
    }

    @Test
    public void save_secret_sanity_check() throws Exception {
        for (final String VALUE: INVALID) {
            exec(String.format("""
                e = "";
                passwd.saveSecret(%s, { label:'key1', data: 'hello world'})
                    .catch((error) => {
                        e = error.toString();
                });
            """, VALUE));
            then(exec("e")).isEqualTo("Error: pin can not be null or empty");
        }

        for (final String VALUE: INVALID) {
            exec(String.format("""
                e = "";
                passwd.saveSecret("1234", { label:%s, data: 'hello world'})
                    .catch((error) => {
                        e = error.toString();
                });
            """, VALUE));
            then(exec("e")).isEqualTo("Error: secret.label can not be null or empty");
        }
        exec("""
            e = "";
            passwd.saveSecret("1234", { data: 'hello world'})
                .catch((error) => {
                    e = error.toString();
            });
        """);
        then(exec("e")).isEqualTo("Error: secret.label can not be null or empty");

        for (final String VALUE: INVALID) {
            exec(String.format("""
                e = "";
                passwd.saveSecret("1234", { label: "key1", data: %s})
                    .catch((error) => {
                        e = error.toString();
                });
            """, VALUE));
            then(exec("e")).isEqualTo("Error: secret.data can not be null or empty");
        }
        exec("""
            e = "";
            passwd.saveSecret("1234", { label: "key1" })
                .catch((error) => {
                    e = error.toString();
            });
        """);
        then(exec("e")).isEqualTo("Error: secret.data can not be null or empty");

        for (final String VALUE: new String[] {"undefined", "null"}) {
            exec(String.format("""
                e = "";
                passwd.saveSecret("1234", %s)
                    .catch((error) => {
                        e = error.toString();
                });
            """, VALUE));
            then(exec("e")).isEqualTo("Error: secret can not be null or empty");
        }
    }

    @Test
    public void load_secret() throws Exception {
        exec("""
            secrets = [];
            Promise.all([
                passwd.saveSecret('1234', { label:'label1', data: 'hello world' }),
                passwd.saveSecret('5678', { label:'label2', data: 'hello universe' })
            ]).then((values) => {
                secrets.push(...values) ;
            });
        """);

        new WaitFor(500, () -> (Boolean)exec("(secrets && secrets.length == 2)"));

        exec("""
            secrets = [];
            Promise.all([
                passwd.loadSecret('1234', 'label1'),
                passwd.loadSecret('1234', 'label2'),
                passwd.loadSecret('5678', 'label2'),
                passwd.loadSecret('1234', 'label1')
            ]).then((values) => {
                secrets.push(...values);
            });
        """);

        new WaitFor(500, () -> (Boolean)exec("(secrets && secrets.length == 4)"));

        then((String)exec("secrets[0]")).isEqualTo("hello world");
        then((String)exec("secrets[1]")).isNotEqualTo("hello world");
        then((String)exec("secrets[2]")).isEqualTo("hello universe");
        then((String)exec("secrets[3]")).isNotEqualTo("hello universe");
    }

    @Test
    public void load_secret_sanity_check() {
        for (final String VALUE: INVALID) {
            exec(String.format("""
                e = "";
                passwd.loadSecret(%s, "label").catch((error) => {
                    e = error.toString();
                });
            """, VALUE));
            then(exec("e")).isEqualTo("Error: pin can not be null or empty");
        }

        for (final String VALUE: INVALID) {
            exec(String.format("""
                e = "";
                passwd.loadSecret("1234", %s).catch((error) => {
                    e = error.toString();
                });
            """, VALUE));
            then(exec("e")).isEqualTo("Error: label can not be null or empty");
        }
    }

    @Test
    public void remove_secret() {
        exec("""
            secrets = [];
            passwd.saveSecret('1234', { label:'label1', data: 'hello world' }).then(() => {
                secrets.push(null) ;
            });
        """);

        new WaitFor(500, () -> (Boolean)exec("(secrets && secrets.length == 1)"));

        exec("""
            secret = {};
            passwd.removeSecret("label1").then(() => {
               secret = localStorage.getItem("toosla.passwd.secret.label1");
            });
        """);

        new WaitFor(500, () -> exec("(secret)") != null);

        exec("""
            done = false;
            passwd.removeSecret("not_exist").then(() => {
               done = true;
            });
        """);

        new WaitFor(500, () -> (Boolean)exec("(done)"));
    }

    @Test
    public void remove_secrete_sanity_check() {
        for (final String VALUE: INVALID) {
            exec(String.format("""
                e = "";
                passwd.removeSecret(%s).catch((error) => {
                    e = error.toString();
                });
            """, VALUE));
            then(exec("e")).isEqualTo("Error: label can not be null or empty");
        }
    }

    @Test
    public void get_existing_pin_from_session_storage() {
        exec("sessionStorage.setItem('toosla.passwd.pin', '1234')");
        then(exec("passwd.pin")).isEqualTo("1234");

        exec("sessionStorage.setItem('toosla.passwd.pin', '5678')");
        then(exec("passwd.pin")).isEqualTo("5678");
    }

    @Test
    public void set_pin_to_session_storage() {
        exec("passwd.pin='1234'");
        then(exec("sessionStorage.getItem('toosla.passwd.pin')")).isEqualTo("1234");

        exec("passwd.pin='5678'");
        then(exec("sessionStorage.getItem('toosla.passwd.pin')")).isEqualTo("5678");
    }

    @Test
    public void check_if_pin_has_been_initialized() {
        then((Boolean)exec("passwd.doesPINExist()")).isFalse();
        exec("localStorage.setItem('toosla.passwd.pin', 'something')");
        then((Boolean)exec("passwd.doesPINExist()")).isTrue();
    }
}
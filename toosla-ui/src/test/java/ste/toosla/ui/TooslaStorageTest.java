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
import ste.xtest.json.api.JSONAssertions;

/**
 *
 * Very basic synchronization for now based on the following assumptions:
 *
 *  - one client at a time
 *  - persist all entries in one storage object
 *
 * At login the storage object is downloaded if more recent than last update
 * of the localStorage. The downloaded object is then saved into the local
 * storage.
 * Each change to the local storage triggers the upload of all localStorage
 * entries.
 *
 **/
public class TooslaStorageTest extends TooslaTestBase {

    public TooslaStorageTest() {
        page = "tooslastorage.html";
    }

    @Before
    public void before() throws Exception {
        super.before();
        exec("localStorage.clear()");
    }

    @Test
    public void login_sets_account_and_tokens() throws Exception {
        //
        // login ok
        //
        exec("""
            const PIN = "123abc";
            const passwd = new PasswordManager();
            passwd.pin = PIN;
            passwd.saveSecret(PIN, {
                label: "storage.credentials",
                data: "username123:password1"
            });
            const tooslaStorage = new TooslaStorage(passwd);
        """);
        async("tooslaStorage.login()");
        then(exec("tooslaStorage.account")).isEqualTo("username123");
        then(exec("tooslaStorage.apiKey")).isEqualTo("key-username123:password1");
        then(exec("tooslaStorage.linkStatus")).isEqualTo("linked");
        then((String)exec("__XTEST__.log")).contains("I TooslaStorage connected with account username123\n");

        exec("""
            passwd.saveSecret(PIN, {
                label: "storage.credentials",
                data: "username456:password2"
            });
        """);
        async("tooslaStorage.login()");
        then(exec("tooslaStorage.account")).isEqualTo("username456");
        then(exec("tooslaStorage.apiKey")).isEqualTo("key-username456:password2");
        then(exec("tooslaStorage.linkStatus")).isEqualTo("linked");
        then((String)exec("__XTEST__.log")).contains("I TooslaStorage connected with account username456\n");

        //
        // special characters
        //
        exec("""
            passwd.saveSecret(PIN, {
                label: "storage.credentials",
                data: 'username%&=?!:@=%56ò&?-\\\"\\''
            });
        """);
        async("tooslaStorage.login()");
        then(exec("tooslaStorage.account")).isEqualTo("username%&=?!");
        then(exec("tooslaStorage.apiKey")).isEqualTo("key-username%&=?!:@=%56ò&?-\"\'");
        then(exec("tooslaStorage.linkStatus")).isEqualTo("linked");

        //
        // invalid credentials - 401
        //
        exec("""
            passwd.saveSecret(PIN, {
                label: "storage.credentials",
                data: "fail-401:nopass"
            });
        """);
        async("tooslaStorage.login()");
        then(exec("tooslaStorage.account")).isNull();
        then(exec("tooslaStorage.apiKey")).isNull();
        then(exec("tooslaStorage.linkStatus")).isEqualTo("unlinked");
        then((String)exec("__XTEST__.log")).contains("TooslaStorage unable to link the remote storage: the provided credentials are not authorized\n");
    }

    @Test
    public void setItem_updates_values_in_local_and_remote_storage() throws Exception {
        login();

        exec("tooslaStorage.lastModified = new Date('2020-12-20T01:01:01Z')");

        //
        // a first value
        //

        exec("tooslaStorage.setItem('key1', 'value1')");
        then(exec("localStorage.getItem('toosla.key1')")).isEqualTo("value1");
        waitUntilTrue("tooslaStorage.changeStatus === 'clean'");
        JSONObject o = new JSONObject(
            (String)exec("localStorage.getItem('/OneMediaHub/Toosla/data.json')")
        );
        JSONAssertions.then(o)
            .hasSize(2)
            .containsEntry("toosla.key1", "value1");
        then(o.getString("lastModified")).matches("[\\dTZ.:+-]+");
        //
        // another value
        //
        exec("tooslaStorage.setItem('key2', 'value2')");
        then(exec("localStorage.getItem('toosla.key2')")).isEqualTo("value2");
        waitUntilTrue("tooslaStorage.changeStatus === 'clean'");
        o = new JSONObject(
            ((String)exec("localStorage.getItem('/OneMediaHub/Toosla/data.json')"))
        );
        JSONAssertions.then(o)
            .hasSize(3)
            .containsEntry("toosla.key1", "value1")
            .containsEntry("toosla.key2", "value2");
        then(o.getString("lastModified")).matches("[\\dTZ.:+-]+");

        //
        // lastModified has been updated
        //
        then(exec("tooslaStorage.lastModified.toISOString()")).isEqualTo(o.getString("lastModified"));
    }

    @Test
    public void getItem_gets_from_local_storage_only() throws Exception {
        login();

        //
        // given a key/value in the local and remote storage
        //
        exec("localStorage.setItem('toosla.key1', 'value1')");
        exec("localStorage.setItem('/OneMediaHub/Toosla/data.json', '{\"toosla.key1\":\"value2\"}')");

        //
        // Read from localStorage (value1) and not remote storage (value2)
        //
        then(exec("tooslaStorage.getItem('key1')")).isEqualTo("value1");
    }

    @Test
    public void length_returns_local_storage_length() throws Exception {
        login();

        //
        // given some items in the local storage
        //
        then((int)exec("tooslaStorage.length")).isZero();
        exec("localStorage.setItem('toosla.key1', 'value1')");
        then((int)exec("tooslaStorage.length")).isEqualTo(1);
        exec("localStorage.setItem('toosla.key2', 'value2')");
        then((int)exec("tooslaStorage.length")).isEqualTo(2);
        exec("localStorage.setItem('another.key', 'anotherValue')"); // Should not be counted

        //
        // Given some items in the remote storage
        //
        exec("localStorage.setItem('/OneMediaHub/Toosla/data.json', '{\"toosla.key3\":\"value3\"}');"); // Remote storage, should not be counted

        //
        // then the length should return only toosla prefixed items
        //
        then((int)exec("tooslaStorage.length")).isEqualTo(2);
    }

    @Test
    public void key_returns_toosla_prefixed_keys() throws Exception {
        login();

        //
        // given some items in the local storage in a specific order
        //
        exec("""
            localStorage.setItem('toosla.key1', 'value1');
            localStorage.setItem('another.key', 'anotherValue'); // Should not be returned by key()
            localStorage.setItem('toosla.key2', 'value2');
            localStorage.setItem('/OneMediaHub/Toosla/data.json', '{"toosla.remoteKey":"remoteValue"}'); // Remote storage, should not be returned by key()
            localStorage.setItem('toosla.key3', 'value3');

            let keys = [];
            for (let i = 0, p = 0; i < localStorage.length; ++i) {
                const key = localStorage.key(i);
                if (key.startsWith("toosla.")) {
                    keys.push(key.substring(7));
                }
            }
            console.debug("keys", keys);
        """);

        //
        // Test for specific keys in the order they were added (after filtering)
        //
        then(exec("tooslaStorage.key(0)")).isEqualTo(exec("keys[0]"));
        then(exec("tooslaStorage.key(1)")).isEqualTo(exec("keys[1]"));
        then(exec("tooslaStorage.key(2)")).isEqualTo(exec("keys[2]"));

        // Test out of bounds
        then(exec("tooslaStorage.key(3)")).isNull();
        then(exec("tooslaStorage.key(99)")).isNull();
        then(exec("tooslaStorage.key(-1)")).isNull();
    }

    @Test
    public void clear_deletes_only_toosla_prefixed_items() throws Exception {
        login();

        //
        // given some items in the local storage
        //
        exec("localStorage.setItem('toosla.key1', 'value1')");
        exec("localStorage.setItem('toosla.key2', 'value2')");
        exec("localStorage.setItem('another.key', 'anotherValue')"); // Should not be deleted
        exec("localStorage.setItem('/OneMediaHub/Toosla/data.json', '{\"toosla.key3\":\"value3\"}');"); // Remote storage, should not be deleted

        then((int)exec("tooslaStorage.length")).isEqualTo(2);
        then(exec("localStorage.getItem('another.key')")).isEqualTo("anotherValue");

        //
        // when I clear the toosla storage
        //
        exec("tooslaStorage.clear()");

        //
        // then only toosla prefixed items are deleted
        //
        then((int)exec("tooslaStorage.length")).isZero();
        then(exec("localStorage.getItem('toosla.key1')")).isNull();
        then(exec("localStorage.getItem('toosla.key2')")).isNull();
        then(exec("localStorage.getItem('another.key')")).isEqualTo("anotherValue");
        waitUntilTrue("tooslaStorage.changeStatus === 'clean'");
        JSONObject o = new JSONObject(
            (String)exec("localStorage.getItem('/OneMediaHub/Toosla/data.json')")
        );
        JSONAssertions.then(o).hasSize(1);
        then(o.getString("lastModified")).matches("[\\dTZ.:+-]+");
    }

    @Test
     public void clear_local_or_both_storage() throws Exception {
        login();
        //
        // given some items in the local storage
        //
        exec("localStorage.setItem('toosla.localKey1', 'localValue1')");

        //
        // and a remote storage with some content updated in the past
        //
        exec("localStorage.setItem('/OneMediaHub/Toosla/data.json', '{\"lastModified\":\"2020-12-20T01:01:01Z\",\"toosla.remoteKey1\":\"remoteValue1\", \"toosla.remoteKey2\":\"remoteValue2\"}');");

        //
        // clear local only
        //
        exec("tooslaStorage.clear(true);");

        then(exec("localStorage.getItem('toosla.localKey1')")).isNull();
        then(exec("localStorage.getItem('/OneMediaHub/Toosla/data.json');")).isNotNull();

        //
        // clear both local and remote storage
        //
        exec("localStorage.setItem('toosla.localKey1', 'localValue1')");
        exec("tooslaStorage.clear(false);");

        then(exec("localStorage.getItem('toosla.localKey1')")).isNull();
        waitUntilTrue("tooslaStorage.changeStatus === 'clean'");
        JSONAssertions.then(new JSONObject((String)exec("localStorage.getItem('/OneMediaHub/Toosla/data.json');")))
            .hasSize(1).contains("lastModified");
     }

    @Test
    public void sync_loads_remote_storage_if_more_recent() throws Exception {
        //
        // given some items in the local storage
        //
        exec("localStorage.setItem('toosla.localKey1', 'localValue1')");
        exec("localStorage.setItem('another.key', 'anotherValue')");

        //
        // and a remote storage with some content
        //
        exec("localStorage.setItem('/OneMediaHub/Toosla/data.json', '{\"toosla.remoteKey1\":\"remoteValue1\", \"toosla.remoteKey2\":\"remoteValue2\"}');");

        //
        // Syncing
        //
        login(); async("tooslaStorage.sync()");
        then(exec("tooslaStorage.changeStatus")).isEqualTo("clean");

        //
        // then toosla storage contains all and only the items from remote
        //
        then((int)exec("tooslaStorage.length")).isEqualTo(2);
        then(exec("tooslaStorage.getItem('remoteKey1')")).isEqualTo("remoteValue1");
        then(exec("tooslaStorage.getItem('remoteKey2')")).isEqualTo("remoteValue2");
        then(exec("tooslaStorage.getItem('localKey1')")).isNull(); // Local item should be overwritten

        //
        // and other items in localStorage are unaffected
        //
        then(exec("localStorage.getItem('another.key')")).isEqualTo("anotherValue");
    }

    @Test
    public void sync_does_not_load_remote_storage_if_less_recent() throws Exception {
        //
        // given some items in the local storage
        //
        exec("localStorage.setItem('toosla.localKey1', 'localValue1')");
        exec("localStorage.setItem('another.key', 'anotherValue')");

        //
        // and a remote storage with some content updated in the past
        //
        exec("localStorage.setItem('/OneMediaHub/Toosla/data.json', '{\"lastModified\":\"2020-12-20T01:01:01Z\",\"toosla.remoteKey1\":\"remoteValue1\", \"toosla.remoteKey2\":\"remoteValue2\"}');");

        //
        // Syncing
        //
        login();
        exec("tooslaStorage.lastModified = new Date('2025-08-01T14:30:00Z')");
        async("tooslaStorage.sync()");
        then(exec("tooslaStorage.changeStatus")).isEqualTo("clean");

        //
        // then toosla storage contains existing values and remote storage
        // the local changes; plus, the last update timestap is the same as
        // the local one.
        //
        then((int)exec("tooslaStorage.length")).isEqualTo(1);
        then(exec("tooslaStorage.getItem('remoteKey1')")).isNull();
        then(exec("tooslaStorage.getItem('localKey1')")).isEqualTo("localValue1");

        //
        // and other items in localStorage are unaffected
        //
        then(exec("localStorage.getItem('another.key')")).isEqualTo("anotherValue");
    }

    @Test
    public void removeItem_deletes_only_toosla_prefixed_items() throws Exception {
        login();

        //
        // given some items in the local storage
        //
        exec("tooslaStorage.setItem('key1', 'value1')");
        exec("tooslaStorage.setItem('key2', 'value2')");
        exec("localStorage.setItem('another.key', 'anotherValue')"); // Should not be deleted

        exec("tooslaStorage.removeItem('key1');");
        then((int)exec("tooslaStorage.length")).isEqualTo(1);
        then(exec("tooslaStorage.getItem('key1');")).isNull();
        waitUntilTrue("tooslaStorage.changeStatus === 'clean'");
        JSONObject o = new JSONObject(
            (String)exec("localStorage.getItem('/OneMediaHub/Toosla/data.json')")
        );
        JSONAssertions.then(o).hasSize(2).containsEntry("toosla.key2", "value2");
        then(o.getString("lastModified")).matches("[\\dTZ.:+-]+");

        exec("tooslaStorage.removeItem('key2');");
        then((int)exec("tooslaStorage.length")).isEqualTo(0);
        then(exec("tooslaStorage.getItem('key2');")).isNull();
        waitUntilTrue("tooslaStorage.changeStatus === 'clean'");
        o = new JSONObject(
            (String)exec("localStorage.getItem('/OneMediaHub/Toosla/data.json')")
        );
        JSONAssertions.then(o).hasSize(1);
        then(o.getString("lastModified")).matches("[\\dTZ.:+-]+");

        then(exec("localStorage.getItem('another.key')")).isEqualTo("anotherValue");

        exec("tooslaStorage.removeItem('key10');"); // it does not exist, just do nothing
        then((int)exec("tooslaStorage.length")).isZero();
    }

    @Test
    public void setItem_sanity_check() throws Exception {
        exec("tooslaStorage = new TooslaStorage('usr:pwd');");
        for (final String VALUE : INVALID) {
            exec(String.format("""
                e = "";
                try {
                    tooslaStorage.setItem(%s, "value");
                } catch(error) {
                    e = error.toString();
                }
            """, VALUE));
            then(exec("e")).isEqualTo("Error: key can not be null or empty");
        }
    }

    @Test
    public void getItem_sanity_check() throws Exception {
        exec("tooslaStorage = new TooslaStorage('usr:pwd');");
        for (final String VALUE : INVALID) {
            exec(String.format("""
                e = "";
                try {
                    tooslaStorage.getItem(%s);
                } catch(error) {
                    e = error.toString();
                }
            """, VALUE));
            then(exec("e")).isEqualTo("Error: key can not be null or empty");
        }
    }

    @Test
    public void removeKey_sanity_check() throws Exception {
        exec("tooslaStorage = new TooslaStorage('usr:pwd');");
        for (final String VALUE : INVALID) {
            exec(String.format("""
                e = "";
                try {
                    tooslaStorage.removeItem(%s);
                } catch(error) {
                    e = error.toString();
                }
            """, VALUE));
            then(exec("e")).isEqualTo("Error: key can not be null or empty");
        }
    }

    // --------------------------------------------------------- private methods

    private void login() throws Exception {
        exec("""
            const PIN = "123abc";
            const passwd = new PasswordManager();
            passwd.pin = PIN;
            passwd.saveSecret(PIN, {
                label: "storage.credentials",
                data: "username123:password1"
            });
            const tooslaStorage = new TooslaStorage(passwd);
        """);
        async("tooslaStorage.login()");
    }
}

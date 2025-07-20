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

/**
 * Get root folder
 *   GET https://zefiro.me/sapi/media/folder/root?action=get&validationkey=543f36292928584c69253c5e5d637d20
 *   {"data":{"folders":[{"name":"OneMediaHub","id":47487,"status":"N","magic":true,"offline":false,"date":1391491519230},{"name":"Download","id":105269,"status":"N","magic":false,"offline":false,"devicename":"fol-9Tbabm+r+K5smt5KB5mtiA==","date":1404913723999},{"name":"Download","id":139809,"status":"N","magic":false,"offline":false,"devicename":"fol-9Tbabm+r+K5smt5KB5mtiA==","date":1406733538488}]},"responsetime":1751126649118}
 *
 * Get OneMediaOne subfolders
 *   GET https://zefiro.me/sapi/media/folder?action=list&parentid=47487&limit=200&validationkey=543f36292928584c69253c5e5d637d20
 *   {"data":{"folders":[{"name":"Mobile and Web Uploads","id":47488,"status":"N","magic":false,"offline":true,"parentid":47487,"date":1391491519690},...]},"responsetime":1751126649266}
 *
 * Create Toosla folder
 *   POST https://zefiro.me/sapi/media/folder?action=save&validationkey=543f36292928584c69253c5e5d637d20
 *     {"data":{"magic":false,"offline":false,"name":"Toosla","parentid":47487}}
 *   {"data":{"folder":{"name":"Toosla","id":867202,"lastupdate":1751127150132},"success":"Folder saved successfully"},"success":"Folder saved successfully","id":867202,"lastupdate":1751127150132,"responsetime":1751127150176}
 *
 * Upload a file
 *   POST https://upload.zefiro.me/sapi/upload?action=save&acceptasynchronous=true&validationkey=543f36292928584c69253c5e5d637d20
 *     multipart
 *
 * Download a file
 *   POST https://zefiro.me/sapi/media?action=get&origin=omh,dropbox&validationkey=543f36292928584c69253c5e5d637d20
 *     {"data":{"ids":[680005393],"fields":["name","creationdate","postingdate","size","thumbnails","viewurl","videometadata","exif","url","shared","exported","origin","folderid","playbackurl","transcodingstatus","etag"]}}
 *   {"data":{"media":[{"id":"680005393","date":1751127510918,"mediatype":"file","status":"U","userid":"ste","url":"https://zefiro.me/sapi/download/file?action=get&k=RlVOI6s9lqoPKYFZt03OSJzbtcNDgU0F6rTjCIyhC-o1c52imEX3p0sCynnCGXzI70OaAn-1Fl547inyHJFJyWKYVV8z7fdYjulI1TxvLYVzOd9rq-T696hQxhiSmDCe8_Mt38k9Uw6tzxYsgBKCQAX6eWU&node=1i164","creationdate":1749970147000,"size":1722,"name":"identity-prod.tgz","etag":"58IEhYENeW04HhTImidzBw==","folder":867202,"shared":false,"origin":{"name":"omh"}}]},"responsetime":1751127803982}
 *
 *   GET https://zefiro.me/sapi/download/file?action=get&k=RlVOI6s9lqoPKYFZt03OSJzbtcNDgU0F6rTjCIyhC-o1c52imEX3p0sCynnCGXzI70OaAn-1Fl547inyHJFJyWKYVV8z7fdYjulI1TxvLYVzOd9rq-T696hQxhiSmDCe8_Mt38k9Uw6tzxYsgBKCQAX6eWU&node=1i164&filename=identity-prod.tgz
 *   ... binary data ...
 *
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
        exec("tooslaStorage = new TooslaStorage('username123:password1');");
        async("tooslaStorage.login()");
        then(exec("tooslaStorage.account")).isEqualTo("username123");
        then(exec("tooslaStorage.validationKey")).isEqualTo("key-username123:password1");
        then(exec("tooslaStorage.linkStatus")).isEqualTo("linked");
        then((String)exec("__XTEST__.log")).contains("I TooslaStorage connected with account username123\n");

        exec("tooslaStorage = new TooslaStorage('username456:password2')");
        async("tooslaStorage.login()");
        then(exec("tooslaStorage.account")).isEqualTo("username456");
        then(exec("tooslaStorage.validationKey")).isEqualTo("key-username456:password2");
        then(exec("tooslaStorage.linkStatus")).isEqualTo("linked");
        then((String)exec("__XTEST__.log")).contains("I TooslaStorage connected with account username456\n");

        //
        // special characters
        //
        exec("tooslaStorage = new TooslaStorage('username%&=?!:@=%56ò&?-\\\"\\'')");
        async("tooslaStorage.login()");
        then(exec("tooslaStorage.account")).isEqualTo("username%&=?!");
        then(exec("tooslaStorage.validationKey")).isEqualTo("key-username%&=?!:@=%56ò&?-\"\'");
        then(exec("tooslaStorage.linkStatus")).isEqualTo("linked");

        //
        // invalid credentials - 401
        //
        exec("tooslaStorage = new TooslaStorage('fail-401:nopass');");
        async("tooslaStorage.login()");
        then(exec("tooslaStorage.account")).isNull();
        then(exec("tooslaStorage.validationKey")).isNull();
        then(exec("tooslaStorage.linkStatus")).isEqualTo("unlinked");
        then((String)exec("__XTEST__.log")).contains("TooslaStorage unable to link the remote storage: the provided credentials are not authorized\n");
    }

    @Test
    public void credentials_with_special_cases() throws Exception {
        //
        // no username
        //
        for (final String VALUE : INVALID) {
            exec(String.format("""
                e = "";
                try {
                    new TooslaStorage(%s);
                } catch (error) {
                    e = error.message;
                }
            """, VALUE));
            then(exec("e")).isEqualTo("credentials can not be null or empty");
        }
    }

    @Test
    public void setItem_in_both_local_and_remote_storage() throws Exception {
        login();

        //
        // a first value
        //
        exec("tooslaStorage.setItem('key1', 'value1')");
        then(exec("localStorage.getItem('toosla.key1')")).isEqualTo("value1");
        waitUntilTrue("tooslaStorage.changeStatus === 'clean'");
        then(exec("localStorage.getItem('/OneMediaHub/Toosla/data.json')"))
            .isEqualTo("{\"toosla.key1\":\"value1\"}");

        //
        // another value
        //
        exec("tooslaStorage.setItem('key2', 'value2')");
        then(exec("localStorage.getItem('toosla.key2')")).isEqualTo("value2");
        waitUntilTrue("tooslaStorage.changeStatus === 'clean'");
        then(exec("localStorage.getItem('/OneMediaHub/Toosla/data.json')"))
            .isEqualTo("{\"toosla.key2\":\"value2\",\"toosla.key1\":\"value1\"}");
    }

    @Test
    public void getItem_gets_from_local_storage_only() throws Exception {
        login();

        //
        // given a key/value in the local storage
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
        then(exec("localStorage.getItem('/OneMediaHub/Toosla/data.json')")).isEqualTo("{}");
    }

    @Test
    public void sync_loads_remote_storage() throws Exception {
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
    public void constructor_stores_credentials() throws Exception {
        // Test successful credential storage
        exec("tooslaStorage = new TooslaStorage('testUser1:testPass1');");
        then(exec("tooslaStorage.credentials")).isEqualTo("testUser1:testPass1");
        exec("tooslaStorage = new TooslaStorage('testUser2:testPass2');");
        then(exec("tooslaStorage.credentials")).isEqualTo("testUser2:testPass2");

        // Test null credentials
        for (final String VALUE : INVALID) {
            exec(String.format("""
                e = "";
                try {
                    tooslaStorage = new TooslaStorage(%s);
                } catch(error) {
                    e = error.toString();
                }
            """, VALUE));
            then(exec("e")).isEqualTo("Error: credentials can not be null or empty");
        }
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
        then(exec("localStorage.getItem('/OneMediaHub/Toosla/data.json')")).isEqualTo("{\"toosla.key2\":\"value2\"}");

        exec("tooslaStorage.removeItem('key2');");
        then((int)exec("tooslaStorage.length")).isEqualTo(0);
        then(exec("tooslaStorage.getItem('key2');")).isNull();
        waitUntilTrue("tooslaStorage.changeStatus === 'clean'");
        then(exec("localStorage.getItem('/OneMediaHub/Toosla/data.json')")).isEqualTo("{}");

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
        exec("tooslaStorage = new TooslaStorage('username123:password1'); tooslaStorage.login();");
    }
}

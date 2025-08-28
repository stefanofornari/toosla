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
import { Utils } from "./Utils.js";

const TOOSLA_KEY_PREFIX = "toosla.";

const LINK_STATUS_UNLINKED = "unlinked"; // before login, invalid tokens
const LINK_STATUS_LINKED = "linked"; // after login, valid tokens
const CHANGE_STATUS_CLEAN = "clean"; // no unsaved changes, in sync
const CHANGE_STATUS_DIRTY = "dirty"; // unsaved changes, not in sync


//
// TODO: make the endpoint dynamic
//
//const TOOSLA_URL = "https://toosla.me";
const TOOSLA_URL = "http://localhost:9090";
const TOOSLA_API = TOOSLA_URL + "/api";
const TOOSLA_API_STORAGE = TOOSLA_API + "/storage";
const TOOSLA_API_STORAGE_LOGIN = TOOSLA_API_STORAGE + "/login";
const TOOSLA_API_STORAGE_READ = TOOSLA_API_STORAGE + "/read";
const TOOSLA_API_STORAGE_WRITE = TOOSLA_API_STORAGE + "/write";

export class TooslaStorage {

    #passwd = null;
    account = null;
    apiKey = null;
    linkStatus = "unlinked";
    changeStatus = "clean";
    lastModified = null;

    constructor(passwd) {
        Utils.checkObject("passwd", passwd);
        this.#passwd = passwd;
    }

    async credentials() {
        return await this.#passwd.loadSecret(this.#passwd.pin, "storage.credentials");
    }

    async login() {
        console.debug("login");

        //
        // authenticate on the remote service
        //
        try {
            let response = await fetch(TOOSLA_API_STORAGE_LOGIN, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    credentials: await this.credentials()
                })
            });
            if (response.ok) {
                const body = await response.json();
                this.account = body.account;
                this.apiKey = body.validationkey;
                this.linkStatus = LINK_STATUS_LINKED;
                console.info(`TooslaStorage connected with account ${this.account}`);
            } else {
                //
                // If the user is not authorized to access the remote storage
                // set status to unlinked. Some operation may fail when unlinked.
                // If there was another error than 401 status does not change
                // as it can be a temporarily error.
                //
                if (response.status === 401) {
                    this.account = null;
                    this.apiKey = null;
                    this.linkStatus = LINK_STATUS_UNLINKED;
                    console.info(`TooslaStorage unable to link the remote storage: the provided credentials are not authorized`);
                }
                return;
            }
        } catch (e) {
            console.info("[TooslaStorage]", "login failed due to a network or unexpected error, working offline");
            console.error(e);
            console.error(e.stack);
            return;
        }
    }

    /**
     * Sync logic is as follows
     *
     * 1. retrieve server version only if more recent
     * 2. if a more recent version is available, the entire local storage is
     *    replaced with the server storage
     *
     * @returns {undefined}
     */
    async sync() {
        console.debug("start sync", this.lastModified);

        try {
            const headers = {
                "Authorization": `token ${this.apiKey}`
            };

            if (this.lastModified) {
                headers["If-Modified-Since"] = this.lastModified.toISOString();
            }

            let response = await fetch(TOOSLA_API_STORAGE_READ, {
                method: "POST",
                headers: headers,
                body: JSON.stringify({
                    "path": "/Toosla/data.json"
                })
            });

            console.debug("downloading done", response.statusText);
            if (response.ok) {
                //
                // Clear existing toosla. prefixed items before loading remote data
                //
                this.clear();

                //
                // Store the values from remote in localStore
                //
                const remoteData = await response.json();
                console.debug("remoteData", JSON.stringify(remoteData));
                for (const key in remoteData) {
                    //
                    // All keys should be toosla keys (starting with toosla.)
                    //
                    console.debug("remote key", key);
                    localStorage.setItem(key, remoteData[key]);
                }
                this.lastModified = Date.parse(response.headers.get("Last-Modified"));
                this.changeStatus = CHANGE_STATUS_CLEAN;
            } else {
                if (response.status === 304) {
                    // Not Modified
                    console.debug("local storage up-to-date");
                    this.changeStatus = CHANGE_STATUS_CLEAN;
                } else if (response.status === 404) {
                    //
                    // 3. Create the Toosla folder and store the storage file if not
                    //    existing already
                    //
                    console.debug("not found, save it");
                    response = await this.saveLocalStorage();
                    this.changeStatus = CHANGE_STATUS_CLEAN;
                }
            }
            console.debug("end sync", this.lastModified);
        } catch (error) {
            console.info("[TooslaStorage]", "unable to read the remote storage due to a network or unexpected error, working offline");
            console.error(error);
            console.error(error.stack);
            throw error;
        }
    }


    /**
     * Save localStorage to the remote storage. Note that the If-Unmodified-Since
     * logic is implemented so that potentially the local changes may be refused
     * by the server. It is up to the server decide when accept changes.
     */
    async saveLocalStorage() {
        try {
            const headers = {
                "Content-Type": "application/json",
                "authorization":  `token ${this.apiKey}`
            };

            if (this.lastModified) {
                headers["If-Unmodified-Since"] = new Date(this.lastModified).toISOString();
            }

            const response = await fetch(TOOSLA_API_STORAGE_WRITE, {
                method: "POST",
                headers: headers,
                body: JSON.stringify({
                    "path": "/Toosla/data.json",
                    "content": this.tooslaLocalStorage()
                })
            });
            if (!response.ok) {
                throw new Error(await response.text());
            }
            this.lastModified = new Date(response.headers.get("Last-Modified"));

            return response;
        } catch (error) {
            console.info("[TooslaStorage]", "unable to save changes to the remote storage due to a network or unexpected error, working offline");
            throw error;
        }
    }

    /**
     * Saves the given key/value pair in both local and remote storage. Notes:
     *
     * 1. when saved in localStorage the key is prepended by TOOSLA_KEY_PREFIX
     * 2. save to remote storage is asynchronous
     *
     * @param {string} key the item's key
     * @param {object} value the items value
     *
     */
    setItem(key, value) {
        console.debug("setItem", key, value);
        Utils.checkValue("key", key);
        localStorage.setItem(TOOSLA_KEY_PREFIX + key, value);
        this.lastUpdated = new Date();
        this.changeStatus = CHANGE_STATUS_DIRTY;
        this.saveLocalStorage()
        .then(() => {
            this.changeStatus = CHANGE_STATUS_CLEAN;
        })
        .catch((error) => {
            //
            // TODO: handle errors
            //
            console.error(error);
            throw error;
        });
    }

    getItem(key) {
        Utils.checkValue("key", key);
        return localStorage.getItem(TOOSLA_KEY_PREFIX + key);
    }

    removeItem(key) {
        Utils.checkValue("key", key);
        localStorage.removeItem(TOOSLA_KEY_PREFIX + key);
        this.changeStatus = CHANGE_STATUS_DIRTY;
        this.saveLocalStorage()
        .then(() => {
            this.changeStatus = CHANGE_STATUS_CLEAN;
        })
        .catch((error) => {
            console.error(error);
            throw error;
        });
    }

    key(n) {
        let tooslaKeys = [];
        for (let i = 0; i < localStorage.length; i++) {
            const key = localStorage.key(i);
            if (key.startsWith(TOOSLA_KEY_PREFIX)) {
                tooslaKeys.push(key.substring(TOOSLA_KEY_PREFIX.length));
            }
        }
        return tooslaKeys[n] || null;
    }

    get length() {
        let count = 0;
        for (let i = 0; i < localStorage.length; i++) {
            const key = localStorage.key(i);
            if (key.startsWith(TOOSLA_KEY_PREFIX)) {
                count++;
            }
        }
        return count;
    }

    clear(localOnly = false) {
        for (let i = localStorage.length - 1; i >= 0; i--) {
            const key = localStorage.key(i);
            if (key.startsWith(TOOSLA_KEY_PREFIX)) {
                localStorage.removeItem(key);
            }
        }
        this.changeStatus = CHANGE_STATUS_DIRTY;
        if (!localOnly) {
            this.saveLocalStorage()
            .then(() => {
                this.changeStatus = CHANGE_STATUS_CLEAN;
            })
            .catch((error) => {
                console.error(error);
                throw error;
            });
        }
    }

    tooslaLocalStorage() {
        const ret = {};

        for (let i = 0; i < localStorage.length; i++)   {
            const key = localStorage.key(i);
            if (key.startsWith(TOOSLA_KEY_PREFIX)) {
                ret[key] = localStorage.getItem(key);
            }
        }

        return ret;
    }
}

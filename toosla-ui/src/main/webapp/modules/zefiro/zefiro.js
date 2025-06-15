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

export class ZefiroController {

    static NAME = "Zefiro";

    constructor($scope, passwd) {
        this.scope = $scope;
        this.passwd = passwd;

        this.scope.credentials = {
            username: "",
            password: "",
            configured: function() {
                return this.username && this.password;
            }
        };
    }

    $onInit() {
        console.debug(`${ZefiroController.NAME} componenent initialized!`);
    }

    $postLink() {
        console.debug(`${ZefiroController.NAME} componenent linked!`);
        this.settings('open');
    }

    $onDestroy() {
        console.debug(`${ZefiroController.NAME} component linked!`);
    }

    async settings(action, status) {
        if (action === 'open') {
            const accounts = this.passwd.labels("zefiro");
            if (accounts.length !== 0) {
                // load settings
                this.scope.credentials.username = accounts[0];
                this.scope.credentials.password = await this.passwd.loadSecret(this.passwd.pin, "zefiro." + accounts[0]);
            } else {
                this.scope.credentials.username = "";
                this.scope.credentials.password = "";
            }
        } else if (action === 'close') {
            if (status === 1) {
                // save settings
                await this.passwd.saveSecret(
                    this.passwd.pin, {
                        label: "zefiro." + this.scope.credentials.username,
                        data: this.scope.credentials.password
                    }
                );
                //
                // remove any additional zefiro credentials
                //
                for (let cred of this.passwd.labels("zefiro")) {
                    if (cred !== this.scope.credentials.username) {
                        await this.passwd.removeSecret("zefiro." + cred);
                    }
                }
            } else {
                // nothing to do
            }
        }
    }

}

ZefiroController.$inject = ['$scope', 'passwd'];
toosla.registerModule(ZefiroController.NAME.toLowerCase(), ZefiroController);
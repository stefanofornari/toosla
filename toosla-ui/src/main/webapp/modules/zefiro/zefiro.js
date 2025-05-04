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
        this.$scope = $scope;
        console.log("passwd", passwd);
    }

    $onInit() {
        console.debug(`${ZefiroController.NAME} componenent initialized!`);
    }

    $postLink() {
        console.debug(`${ZefiroController.NAME} componenent linked!`);
    }

    $onDestroy() {
        console.debug(`${ZefiroController.NAME} component linked!`);
    }

    settings(action, status) {
        console.debug("Zefiro settings action:", action, status);

        if (action === 'open') {
        } else if (action === 'close') {
            if (status === 1) {
                // save settings
                console.debug("saving credentials");
            } else {
                // load settings
            }
        }
    }

}

//ZefiroController.$inject = ['$scope', 'passwd'];
ZefiroController.$inject = ['$scope'];
toosla.registerModule(ZefiroController.NAME.toLowerCase(), ZefiroController);
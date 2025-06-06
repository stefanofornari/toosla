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

export class DemoController {

    name = "World";
    time = "";

    #taskID = null;

    constructor($scope) {
        this.$scope = $scope;
    }

    updateTime() {
        this.time = new Date().toTimeString();
    }

    setup() {
        const self = this;

        self.updateTime();
        if (self.#taskID !== null) {
            clearInterval(self.#taskID);
        }
        self.#taskID = setInterval(() => {
            self.$scope.$apply(() => self.updateTime());
        }, 500);
    }

    settings(action, status) {
        console.log("Demo settings", action, status);
    }

    $onInit() {
        console.log('Demo component initialized!');
    };

    $postLink() {
        console.log('Demo component linked!');
        try {
            this.setup();
        } catch (e) {
            console.error(e);
        }
    }

    $onDestroy() {
        console.log('Demo omponent destroyed!');
    }
}

toosla.registerModule("demo", DemoController);
console.deug("Demo done!");
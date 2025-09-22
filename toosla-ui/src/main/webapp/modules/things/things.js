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

import { Utils } from "../../scripts/Utils.js";

export class ThingsController {

    static NAME = "Things";

    editMode = false;

    things = [];

    constructor($scope) {
        this.$scope = $scope;
    }

    $onInit() {
        console.debug(`${ThingsController.NAME} componenent initialized!`);
        //
        // Read items from local storage
        //
        this.loadThings();

        if (!this.things) {
            //
            // Create the default item
            //
            const NOW = new Date();
            this.things = [{
                text: "Tap to edit your first thing",
                when: `${NOW.getFullYear()}-${String(NOW.getMonth() + 1).padStart(2, '0')}-${String(NOW.getDate()).padStart(2, '0')}`,
                status: "active",
                things: []
            }];
        }

        this.storageListener = (e) => {
            if (e.detail.key === 'toosla.things.things') {
                this.loadThings();
            }
        };

        window.addEventListener('storage', this.storageListener);
    }

    $postLink() {
        console.debug(`${ThingsController.NAME} componenent linked!`);
    }

    $onDestroy() {
        console.debug(`${ThingsController.NAME} component destroyed!`);
        window.removeEventListener('storage', this.storageListener);
    }

    //
    // if item is undefined, add a new thing, otherwise edit the existing one
    //
    onEditThing(item) {
        if (item) {
            this.selectedThing = angular.copy(item);
            this.originalThing = item;
        } else {
            const NOW = new Date();
            this.selectedThing = {
                when: `${NOW.getFullYear()}-${NOW.getMonth()}-${NOW.getDate()}`
            };
            this.originalThing = null;
        }
        this.editMode = true;
    }

    onDeleteThing(event, thing) {
        if (event.target.matches(".btn-bin")) {
            //
            // Show the confirmation dialog and return
            event.stopPropagation();
            this.selectedThing = thing;

            $('#things dialog.delete-confirm')[0].showModal();

            return;
        } else if (event.target.matches(".btn-confirm")) {
            //
            // Delete the item
            //
            this.things.forEach((item, index) => {
                if (item === thing) {
                    this.things.splice(index, 1);
                }
            });

            toosla.storage.setItem("toosla.things.things", JSON.stringify(this.things));
        }

        this.selectedThing = null;
        $('#things dialog.delete-confirm')[0].close();
    }

    onCloseEditThing(save) {
        if (save) {
            this.selectedThing.when = Metro.getPlugin($('#things .edit-view input[name=when]')[0], "datepicker").val();
            if (this.originalThing) {
                //
                // it's an update
                //
                Object.assign(this.originalThing, this.selectedThing);
            } else {
                //
                // it's a new thing
                //
                this.selectedThing.things = [];
                this.things.push(this.selectedThing);
            }

            toosla.storage.setItem("toosla.things.things", JSON.stringify(this.things));
        }
        this.editMode = false;
    }

    loadThings() {
        this.things = JSON.parse(toosla.storage.getItem('toosla.things.things'));
    }

    title(thing) {
        return Utils.abbr(thing.text);
    }
}

ThingsController.$inject = ['$scope'];
toosla.registerModule(ThingsController.NAME.toLowerCase(), ThingsController);
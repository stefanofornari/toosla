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

class ThingsController {

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
        console.debug(localStorage.getItem('toosla.things.things'));
        this.loadThings();

        if (!this.things) {
            //
            // Create the default item
            //
            this.things = [{
                text: "Tap to edit your first thing",
                when: new Date().toISOString().substring(0, 10),
                status: "active",
                things: []
            }];
            this.makeThings(this.things);
        }
    }

    $postLink() {
        console.debug(`${ThingsController.NAME} componenent linked!`);
    }

    $onDestroy() {
        console.debug(`${ThingsController.NAME} component linked!`);
    }

    //
    // if item is undefined, add a new thing, otherwise edit the existing one
    //
    onEditThing(item) {
        console.debug("item clicked", JSON.stringify(item));
        if (item) {
            this.selectedThing = angular.copy(item);
            this.originalThing = item;
        } else {
            this.selectedThing = {};
            this.originalThing = null;
        }
        this.editMode = true;
    }

    onDeleteThing(event, thing) {
        console.debug("delete clicked", JSON.stringify(event), JSON.stringify(thing));
        
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

            localStorage.setItem("toosla.things.things", JSON.stringify(this.things));
        }
        
        this.selectedThing = null;
        $('#things dialog.delete-confirm')[0].close();
    }

    onCloseEditThing(save) {
        console.debug("close clicked", save);

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
                this.makeThings([this.selectedThing]);
                this.things.push(this.selectedThing);
            }

            localStorage.setItem("toosla.things.things", JSON.stringify(this.things));
        }
        this.editMode = false;
    }
    
    loadThings() {
        const things = JSON.parse(localStorage.getItem('toosla.things.things'));
        this.makeThings(things);
        this.things = things;
    }
    
    makeThings(things) {
        if (!things || things === null) {
            return;
        }
        
        for (const t of things) {
            Object.defineProperty(t, "title", {
                get() {
                    if (!this.text) {
                        return "";
                    }
                    let title = this.text;
                    const pos = title.indexOf("\n");
                    if (pos > 0) {
                        title = title.slice(0, pos);
                    }
                    
                    return (title.length > 50) ?
                        title.slice(0, 49) + '…' : title;
                }
            });
        }
    }
}

ThingsController.$inject = ['$scope'];
toosla.registerModule(ThingsController.NAME.toLowerCase(), ThingsController);
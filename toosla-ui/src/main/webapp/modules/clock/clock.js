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

export const CONFIG_CLOCK_TIMEZONE = "toosla.clock.timezone";

export class ClockController {

    currentTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone;  // default timzone for time
    todayDate = ""; // today in the format specified by dateFormat

    dateFormat = new Intl.DateTimeFormat('en-GB', { dateStyle: 'full' });
    timeFormat = new Intl.DateTimeFormat('en-GB', { timeStyle: 'medium', timeZone: 'Europe/Rome' });  // TODO: set a better default

    #taskID = null;

    constructor($scope) {
        this.$scope = $scope;
    }

    clocks() {
        return document.querySelectorAll(".clock");
    };

    currentDates(){
        return document.querySelectorAll(".current-date");
    }

    timeZone(tz) {
        console.info("Setting timezone to", tz);
        this.currentTimezone = tz;
        this.timeFormat = new Intl.DateTimeFormat('en-GB', { timeStyle: 'medium', timeZone: tz });
        this.dateFormat = new Intl.DateTimeFormat('en-GB', { dateStyle: 'full', timeZone: tz  });
    }

    updateTime() {
        const nb = [
            "inset 0 0 0 var(--size) var(--color), 0px var(--size) var(--color), 0px calc(var(--size) * 2) var(--color), 0px calc(var(--size) * 3) var(--color), 0px calc(var(--size) * 4) var(--color), var(--size) 0 var(--color), calc(var(--size) * 2) 0 var(--color), calc(var(--size) * 3) 0 var(--color), calc(var(--size) * 3) var(--size) var(--color), calc(var(--size) * 3) calc(var(--size) * 2) var(--color), calc(var(--size) * 3) calc(var(--size) * 3) var(--color), calc(var(--size) * 3) calc(var(--size) * 4) var(--color), var(--size) calc(var(--size) * 4) var(--color), calc(var(--size) * 2) calc(var(--size) * 4) var(--color)",
            "inset 0 0 0 var(--size) rgb(0 0 0 / 0),calc(var(--size)*2) var(--size) var(--color), calc(var(--size)*2) 0 var(--color), calc(var(--size)*2) calc(var(--size) * 2) var(--color), calc(var(--size)*2) calc(var(--size) * 3) var(--color), calc(var(--size)*2) calc(var(--size) * 4) var(--color)",
            "inset 0 0 0 var(--size) var(--color), var(--size) 0px var(--color), calc(var(--size) * 2) 0px var(--color), calc(var(--size) * 3) 0px var(--color), calc(var(--size) * 3) var(--size) var(--color), 0 calc(var(--size) * 2) var(--color), var(--size) calc(var(--size) * 2) var(--color), calc(var(--size) * 2) calc(var(--size) * 2) var(--color), calc(var(--size) * 3) calc(var(--size) * 2) var(--color), 0 calc(var(--size) * 3) var(--color), 0 calc(var(--size) * 4) var(--color), var(--size) calc(var(--size) * 4) var(--color), calc(var(--size) * 2) calc(var(--size) * 4) var(--color), calc(var(--size) * 3) calc(var(--size) * 4) var(--color)",
            "inset 0 0 0 var(--size) var(--color),0px calc(var(--size) * 4) var(--color), var(--size) 0 var(--color), calc(var(--size) * 2) 0 var(--color), calc(var(--size) * 3) 0 var(--color), calc(var(--size) * 3) var(--size) var(--color), calc(var(--size) * 3) calc(var(--size) * 2) var(--color), calc(var(--size) * 2) calc(var(--size) * 2) var(--color),var(--size) calc(var(--size) * 2) var(--color),0 calc(var(--size) * 2) var(--color), calc(var(--size) * 3) calc(var(--size) * 3) var(--color), calc(var(--size) * 3) calc(var(--size) * 4) var(--color), var(--size) calc(var(--size) * 4) var(--color), calc(var(--size) * 2) calc(var(--size) * 4) var(--color)",
            "inset 0 0 0 var(--size) var(--color),calc(var(--size)* 3) 0 var(--color), calc(var(--size)* 3) var(--size) var(--color),  calc(var(--size)* 3) calc(var(--size) * 2) var(--color), calc(var(--size)* 2) calc(var(--size) * 2) var(--color), var(--size) calc(var(--size) * 2) var(--color), 0 calc(var(--size) * 2) var(--color), 0 var(--size) var(--color), calc(var(--size)* 3) calc(var(--size) * 3) var(--color), calc(var(--size)* 3) calc(var(--size) * 4) var(--color)",
            "inset 0 0 0 var(--size) var(--color), 0px var(--size) var(--color), var(--size) 0 var(--color), calc(var(--size) * 2) 0 var(--color), calc(var(--size) * 3) 0 var(--color), 0 calc(var(--size) * 2) var(--color), var(--size) calc(var(--size) * 2) var(--color), calc(var(--size) * 2) calc(var(--size) * 2) var(--color), calc(var(--size) * 3) calc(var(--size) * 2) var(--color), calc(var(--size) * 3) calc(var(--size) * 3) var(--color), calc(var(--size) * 3) calc(var(--size) * 4) var(--color), calc(var(--size) * 2) calc(var(--size) * 4) var(--color), var(--size) calc(var(--size) * 4) var(--color), 0 calc(var(--size) * 4) var(--color)",
            "inset 0 0 0 var(--size) var(--color),0px var(--size) var(--color), 0px calc(var(--size) * 2) var(--color), 0px calc(var(--size) * 3) var(--color), 0px calc(var(--size) * 4) var(--color), var(--size) calc(var(--size) * 4) var(--color), calc(var(--size)*2) calc(var(--size) * 4) var(--color), calc(var(--size)*3) calc(var(--size) * 3) var(--color), calc(var(--size)*3) calc(var(--size) * 2) var(--color), calc(var(--size)*2) calc(var(--size) * 2) var(--color), var(--size) calc(var(--size) * 2) var(--color), calc(var(--size)*3) calc(var(--size) * 4) var(--color)",
            "inset 0 0 0 var(--size) var(--color),calc(var(--size) * 3) 0 var(--color), calc(var(--size) * 2) 0 var(--color), var(--size) 0 var(--color), calc(var(--size) * 3) var(--size) var(--color), calc(var(--size) * 3) calc(var(--size) * 2) var(--color), calc(var(--size) * 3) calc(var(--size) * 3) var(--color), calc(var(--size) * 3) calc(var(--size) * 4) var(--color)",
            "inset 0 0 0 var(--size) var(--color),0px var(--size) var(--color), 0px calc(var(--size) * 2) var(--color), 0px calc(var(--size) * 3) var(--color), 0px calc(var(--size) * 4) var(--color), var(--size) 0 var(--color), calc(var(--size) * 2) 0 var(--color), calc(var(--size) * 3) 0 var(--color), calc(var(--size) * 3) var(--size) var(--color), calc(var(--size) * 3) calc(var(--size) * 2) var(--color), calc(var(--size) * 2) calc(var(--size) * 2) var(--color),var(--size) calc(var(--size) * 2) var(--color), calc(var(--size) * 3) calc(var(--size) * 3) var(--color), calc(var(--size) * 3) calc(var(--size) * 4) var(--color), var(--size) calc(var(--size) * 4) var(--color), calc(var(--size) * 2) calc(var(--size) * 4) var(--color)",
            "inset 0 0 0 var(--size) var(--color),calc(var(--size) * 3) 0 var(--color),calc(var(--size) * 2) 0 var(--color),var(--size) 0 var(--color),calc(var(--size) * 2) calc(var(--size) * 2) var(--color),var(--size) calc(var(--size) * 2) var(--color),0 calc(var(--size) * 2) var(--color),0 var(--size) var(--color), calc(var(--size) * 3) var(--size) var(--color), calc(var(--size) * 3) calc(var(--size) * 2) var(--color), calc(var(--size) * 3) calc(var(--size) * 3) var(--color), calc(var(--size) * 3) calc(var(--size) * 4) var(--color)"
          ];

        const today = new Date();
        const timeString = this.timeFormat.format(today);
        const time = timeString.split("").filter((v) => v !== ":");

        const todayDate = this.dateFormat.format(today);
        if (this.todayDate !== todayDate) {
            this.todayDate = todayDate;
        }

        this.clocks().forEach((clock) => {
            const divs = clock.querySelectorAll("div");
            time.forEach((t, i) => {
                if (t !== ":") {
                    for (let x = 0; x < divs.length; x++) {
                        const cell = divs[x].querySelector(`span:nth-child(${i + 1})`);
                        cell.style.boxShadow = nb[parseInt(t)];
                    }
                }
            });
        });
    }

    setup() {
        const self = this;

        $("#clock .settings select.timezones").on("itemselect", function() {
            self.timeZone($(this).val());
        });

        self.loadSettings();
        self.updateTime();
        if (self.#taskID !== null) {
            clearInterval(self.#taskID);
        }
        self.#taskID = setInterval(() => {
            self.$scope.$apply(() => self.updateTime());
        }, 500);

        return;
    }

    settings(action, status) {
        console.debug("Clock settings action:", action, status);

        const select = $("#toosla-settings .settings select.timezones");
        if (action === 'open') {
            //
            // initialize available timezones and add them to the select, then
            // select the current timezone
            //
            let options = {};
            for (const tz of Intl.supportedValuesOf('timeZone')) {
                options[tz] = tz;
            }
            const metroSelect = Metro.getPlugin(select, "select");
            metroSelect.data(options);

            metroSelect.val(this.currentTimezone);
        } else if (action === 'close') {
            if (status === 1) {
                this.saveSettings(Metro.getPlugin(select, "select").val());
            } else {
                this.loadSettings();
            }
        }
    }

    loadSettings() {
        try {
            const TZ = toosla.storage.getItem(CONFIG_CLOCK_TIMEZONE);
            if (TZ) {
                this.timeZone(TZ); return;
            }
        } catch (e) {
            console.error(e.stack);
            console.error("Error reading TZ from local storage, using default");
        }

        //
        // if no settings available, use the default
        //
        this.timeZone(Intl.DateTimeFormat().resolvedOptions().timeZone);
    }

    saveSettings(tz) {
        try {
            toosla.storage.setItem(CONFIG_CLOCK_TIMEZONE, tz);
        } catch (e) {
            console.error("Error writing TZ to local storage");
        }
    }

    $onInit() {
        console.debug("Clock componenent initialized!");
    };

    $postLink() {
        console.debug("Clock componenent linked!");
        try {
            this.setup();
        } catch (e) {
            console.error(e);
        }
    }

    $onDestroy() {
        if (self.#taskID !== null) {
            clearInterval(self.#taskID);
        }
    }
}

ClockController.$inject = ['$scope'];
toosla.registerModule('clock', ClockController);
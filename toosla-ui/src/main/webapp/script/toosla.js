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

var toosla = {  // back to const ?
    version: "${project.version}",
    build: "${buildTimestamp}",

    storage: null,

    modules: [],

    setup: function() {
        tooslats = new Date();
        let self = this;

        $("#version").text(toosla.version);
        console.info(
            `%c Toosla %cv${this.version} %c${this.build}`,
            "color: white; font-weight: bold; background: blue",
            "color: white; background: darkgreen",
            "color: white; background: darkgreen|color: white; background: #0080fe;"
        );

        self.darkMode(window.matchMedia('(prefers-color-scheme: dark)').matches);
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', event => {
            self.darkMode(event.matches);
        });

        //
        // wen run into the testing framework the local storage is not supported
        //
        try {
            self.storage = window.localStorage;
        } catch (error) {
            console.info("Local storage not supported");
        }
    },

    darkMode: function(dark) {
        if (dark){
            // switch to dark
            console.info("Switching Toosla to dark mode");
            $("html").addClass("dark-side");
        } else {
            // light
             console.info("Switching Toosla to light mode");
            $("html").removeClass("dark-side");
        }
    },

    checkFeatures: function() {
        console.info("Browser languages:", navigator.languages);
        console.info("Full screen support:", document.fullscreenEnabled);
        console.info("Dark mode: ", window.matchMedia);
        console.info("Local Storage Access: ", window.localStorage.length === 0);
    }
};

//  const { TaskTimer } = tasktimer;

toosla.setup();

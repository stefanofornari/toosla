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

    setup: function() {
        $("#version").text(toosla.version);
        console.info(
            `%c Toosla %cv${this.version} %c${this.build}`,
            "color: white; font-weight: bold; background: blue",
            "color: white; background: darkgreen",
            "color: white; background: darkgreen|color: white; background: #0080fe;"
        );
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', event => {
            if (event.matches){
                // dark
                console.info("Switching Toosla to dark mode");
                $("html").addClass("dark-side");
            } else {
                // light
                 console.info("Switching Toosla to light mode");
                $("html").removeClass("dark-side");
            }
        });
    }
};

toosla.setup();

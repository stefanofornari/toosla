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

const tooslaConsole = (function(srdConsole){
    return {
        out: function(severity, arguments) {
            const NOW = new Date();

            let oldText = document.getElementById("console").getElementsByClassName('content')[0].innerHTML;
            document.getElementById("console").getElementsByClassName('content')[0].innerHTML =
                (NOW.getTime() + " (" + severity + ") " + Array.from(arguments).join(" ") + "<br/> ") + oldText;
        },
        log: function(...text){
            srdConsole.log(...text);
            this.out("L", text)
        },
        info: function (...text) {
            srdConsole.info(...text);
            this.out("I", text)
        },
        warn: function (...text) {
            srdConsole.warn(...text);
            this.out("W", text)
        },
        error: function (...text) {
            srdConsole.error(...text);
            this.out("E", text)
        },
        debug: function (...text) {
            srdConsole.debug(...text);
            this.out("D", text)
        }
    };
}(window.console));


window.console = tooslaConsole;
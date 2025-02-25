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

const CONSOLE = window.console;

class ConsoleController {
    $onInit() {
        window.console = this;
    };

    $postLink() {
    }

    $onDestroy() {
    }

    out(severity, args) {
        const NOW = new Date();

        let oldText = document.getElementById("console").getElementsByClassName('content')[0].innerHTML;
        document.getElementById("console").getElementsByClassName('content')[0].innerHTML =
            (NOW.getTime() + " (" + severity + ") " + Array.from(args).join(" ") + "<br/> ") + oldText;
    }

    log(...text){
        CONSOLE.log(...text);
        this.out("L", text);
    }

    info(...text) {
        CONSOLE.info(...text);
        this.out("I", text);
    }

    warn(...text) {
        CONSOLE.warn(...text);
        this.out("W", text);
    }

    error(...text) {
        CONSOLE.error(...text);
        this.out("E", text);
    }

    debug(...text) {
        CONSOLE.debug(...text);
        this.out("D", text);
    }
}



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

const tooslaConsole = {
    log: function(severity, arguments) {
        var e = $("#console #content");
        e.append("<br/>(" + severity + ") " + Array.from(arguments).join(" "));
    },

    setup: function() {
        const self = this;

        //
        // logs
        //
        console.stdlog = console.log.bind(console);
        console.log = function () {
            self.log("I", arguments);
            console.stdlog.apply(console, arguments);
        };

        //
        // errors
        //
        console.stderr = console.error.bind(console);
        console.error = function () {
            self.log("E", arguments);
            console.stderr.apply(console, arguments);
        };

        //
        // warnings
        //
        console.stdwarn = console.warn.bind(console);
        console.warns = [];
        console.warn = function () {
            self.log("W", arguments);
            console.stdwarn.apply(console, arguments);
        };

        //
        // debug
        //
        console.stddbg = console.debug.bind(console);
        console.debugs = [];
        console.debug = function () {
            self.log("D", arguments);
            console.stddbg.apply(console, arguments);
        };
    }
};

tooslaConsole.setup();
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

export class CalcController {

    expression = "";
    result = "0";

    constructor($scope, $timeout) {
        this.$scope = $scope;
        this.$timeout = $timeout;

        this.$scope.onButtonClick = this.onButtonClick.bind(this);
        this.$scope.onBackspaceClick = this.onBackspaceClick.bind(this);
    }

    onButtonClick(event) {
        let key = event.target.innerHTML;

        //
        // if = calculate and show the result
        //
        if (key === "=") {
            try {
                this.result = parseFloat(math.evaluate(this.expression).toFixed(15));
            } catch (err) {
                console.error(err);
                this.result = "Error";
            }
            this.$scope.$apply();
            return;
        }

        const inputElement = $(".calc-typed input")[0];
        let cursorPosition = inputElement.selectionStart;

        switch(key) {
            case "C":
                this.expression = "";
                this.result = "0";
                this.$scope.$apply();
                return;
            case "∓":
                key = "";
                this.switchSign(cursorPosition);
                ++cursorPosition;
                break;
            case "⊘":
                key = "/";
                break;
            case "⊗":
                key = "*";
                break;
            case "⊖":
                key = "-";
                break;
            case "⊕":
                key = "+";
                break;
        }

        //
        // Handling the content directly as I was not able to get it work
        // dispatching simulated events. Also, updating this.expression
        // only at the end to better handling focus and caret.
        //
        inputElement.focus();

        if (key !== "") {
            this.expression = this.expression.slice(0, cursorPosition) + key + this.expression.slice(cursorPosition);
        }
        this.$scope.$apply();

        inputElement.setSelectionRange(cursorPosition + 1, cursorPosition + 1);
    }

    onBackspaceClick(event) {
        const inputElement = $(".calc-typed input")[0];
        let cursorPosition = inputElement.selectionStart;

        inputElement.focus();
        if (cursorPosition === 0) {
            return;
        }

        this.expression = this.expression.slice(0, cursorPosition-1) + this.expression.slice(cursorPosition);
        this.$scope.$apply();
        inputElement.setSelectionRange(cursorPosition-1, cursorPosition-1);
    }

    $onInit() {
        console.debug("Calc componenent initialized!");
    }

    $postLink() {
        console.debug("Calc componenent linked!");

        const root = document.getElementById("calc");

        //
        // Make the buttons clickable
        //
        for (const b of root.getElementsByClassName("calc-button")) {
            angular.element(b).on('click', this.$scope.onButtonClick);
        }

        //
        // Make backspace clickable (use of $timeout is due to the element
        // being rendered by the metro framework at a later stage)
        //
        this.$timeout(() => {
            const bs = root.getElementsByClassName("mif-arrow-left");
            angular.element(bs[0]).on('click', this.$scope.onBackspaceClick);
        });

        //
        // load math.js
        //
        let script = document.createElement("script");
        script.type = "text/javascript";
        script.async = true;
        script.src = "scripts/math.js";
        document.body.appendChild(script);
    }

    $onDestroy() {
        console.debug('Calc component linked!');
    }

    switchSign(cursorPosition) {
        //
        // search for the insert position, which is the position of the first
        // non-digit character from startPosition
        //
        let insertPosition = cursorPosition-1;
        for (; insertPosition>0; --insertPosition) {
            const c = this.expression.charAt(insertPosition);
            if (c < '0' || c > '9') {
                ++insertPosition;
                break;
            }
        }

        //
        // if at the insert position there is already (-, remove it, otherwise
        // add it
        //
        if (insertPosition >= 2) {
            if (
                (this.expression.charAt(insertPosition-1) === '-') &&
                (this.expression.charAt(insertPosition-2) === '(')
            ) {
                this.expression = this.expression.slice(0, insertPosition-2) + this.expression.slice(insertPosition);
                return;
            }
        }
        this.expression = this.expression.slice(0, insertPosition) + "(-" + this.expression.slice(insertPosition);
    }
}

CalcController.$inject = ['$scope', '$timeout'];
toosla.registerModule('calc', CalcController);

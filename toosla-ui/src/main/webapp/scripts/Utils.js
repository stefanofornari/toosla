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

export class Utils {
    static #MAX_TEXT_LENGTH = 28

    static abbr(text) {
        if (!text) {
            return "";
        }
        const pos = text.indexOf("\n");
        if (pos > 0) {
            text = text.slice(0, pos);
        }

        return (text.length > Utils.#MAX_TEXT_LENGTH) ?
            text.slice(0, Utils.#MAX_TEXT_LENGTH-1) + '…' : text;
    }

    /**
     * Checks if the provided value is not defined or null or empty or just blanks
     *
     * @param name name of the argument/parameter/value; it iwll be used in
     *                 the exception
     * @param value the value to check
     *
     *
     * @throws {type} an exception if the provided value is not define, null
     *                empty or only blanks
     */
    static checkValue(name, value) {
        Utils.checkObject(name, value);
        if (
            !value.length ||
            !value.trim().length
           ) {
           throw new Error(`${name} can not be null or empty`);
        }
    }

    /**
     * Checks if the provided object is not defined or null
     *
     * @param name name of the argument/parameter/value; it iwll be used in
     *                 the exception
     * @param value the value to check
     *
     *
     * @throws {type} an exception if the provided value is not define or null
     */
    static checkObject(name, value) {
        if (
            (value === undefined) ||
            (value === null)
        ) {
           throw new Error(`${name} can not be null or empty`);
        }
    }
}

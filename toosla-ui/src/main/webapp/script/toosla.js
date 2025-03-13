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

//
// TODO: move all methods manipulating the dom into the controller
//
class Toosla {  // back to const ?
    version = "${project.version}";
    build = "${buildTimestamp}";

    #modules = new Map();

    setup() {
        console.debug("Toosla setup start");

        $("#version").text(this.version);
        console.info(
            `%c Toosla %cv${this.version} %c${this.build}`,
            "color: white; font-weight: bold; background: blue",
            "color: white; background: darkgreen",
            "color: white; background: darkgreen|color: white; background: #0080fe;"
        );

        this.darkMode(window.matchMedia('(prefers-color-scheme: dark)').matches);
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', event => {
            self.darkMode(event.matches);
        });

        console.debug("Toosla setup end");
    };

    darkMode(dark) {
        if (dark){
            // switch to dark
            console.info("Switching Toosla to dark mode");
            $("html").addClass("dark-side");
        } else {
            // light
             console.info("Switching Toosla to light mode");
            $("html").removeClass("dark-side");
        }
    };

    checkFeatures() {
        console.info("Browser languages:", navigator.languages);
        console.info("Full screen support:", document.fullscreenEnabled);
        console.info("Dark mode: ", window.matchMedia);
        console.info("Local Storage Access: ", window.localStorage.length === 0);
    };

    registerModule(name, controller) {
        this.#modules.set(name, controller);
    }

    modules() {
        //
        // make sure the toosla component is always the latest
        //
        const moduleList = Array.from(this.#modules.entries());
        moduleList.push(["toosla", TooslaController]);
        return moduleList;
    }
};

class TooslaController {
    constructor($scope, $compile, $timeout) {
        this.$scope = $scope;
        this.$compile = $compile;
        this.$timeout = $timeout;

        this.$scope.moduleSettings = this.moduleSettings.bind(this);
        this.$scope.closeSettings = this.closeSettings.bind(this);
        this.$scope.toogleFullscreen = this.moduleSettings.bind(this);
    }

    $onInit() {
        console.log('Toosla initialized!');
    };

    $postLink() {
        console.log('Toosla linked!');
        this.$timeout(() => { // to make sure it executes after all templates
                              // have been loaded and rendered
            toosla.setup(this.$scope);

            //
            // 1. add application button bar to each module's .application-action-bar element
            // 2. move the settings div into #toosla-settings
            //

            for ([name, controller] of toosla.modules()) {
                const actionBar = $(`#${name} .application-action-bar`);
                const settingsPanel = $(`#${name} .settings`);

                console.log("settings for module", name, actionBar, settingsPanel);

                if (actionBar && actionBar.length>0) {
                    const actionBar = $(`#${name} .application-action-bar`);
                    const buttons = [
                        angular.element(
                            `<button class="button toosla-btn-fullscreen" ng-click="toogleFullscreen('${name}')"><span class="mif-enlarge mif-2x icon"></span></button>`
                        )
                    ];

                    if (settingsPanel && settingsPanel.length>0) {
                        buttons.push(
                            angular.element(
                                `<button class="button toosla-btn-settings" ng-click="moduleSettings('${name}', 'open')"><span class="mif-cog mif-2x icon"></span></button>`
                            )
                        );

                        //
                        // Moving module settings element into toosla-settings
                        //
                        settingsPanel.attr("module", name); settingsPanel.hide();
                        settingsPanel.appendTo('#toosla-settings');
                    }
                    for (const button of buttons) {
                        $(`#${name} .application-action-bar`).prepend(button);
                        this.$compile(button)(this.$scope);
                    }
                }
            }
        });
    }

    $onDestroy() {
        console.log('Toosla destroyed!');
    }

    moduleSettings(module, action, status) {
        console.debug("Toosla settings clicked for", module, action, status);

        const controller = angular.element($(`#${module}`)).controller(module);

        if (action == "close") {
            controller.settings(action, status);
            Metro.charms.close("#toosla-settings");
            $("#toosla-settings").removeAttr("module");
        } else if (action === "open") {
            $("#toosla-settings").attr("module", module);
            //
            // Mark which module settings to edit in an attribute of #toosla-settings
            // and make the module settings visible hiding everything else
            //
            $("#toosla-settings .settings").hide();
            $(`#toosla-settings div.settings[module|='${module}']`).show();

            controller.settings(action);
            Metro.charms.open("#toosla-settings");
        }
    }

    toogleFullscreen(module) {
        console.log("Toosla toogle fullscreen clicked for", module);
    }

    closeSettings(save) {
        this.moduleSettings($("#toosla-settings").attr("module"), "close", save ? 1 : 0);
    }
}

//  const { TaskTimer } = tasktimer;
const toosla = new Toosla();



class DemoController1 {

    name = "World";
    time = "";

    lastSettingsEvent = null;

    #taskID = null;

    constructor($scope) {
        this.$scope = $scope;
    }

    updateTime() {
        this.time = new Date().toTimeString();
    }

    setup() {
        const self = this;

        self.updateTime();
        if (self.#taskID !== null) {
            clearInterval(self.#taskID);
        }
        self.#taskID = setInterval(() => {
            self.$scope.$apply(() => self.updateTime());
        }, 500);
    }

    settings(action, status) {
        this.lastSettingsEvent = action + ((status === undefined) ? "" : `-${status}`);
    }

    $onInit() {
        console.log('Demo1 component initialized!');
    };

    $postLink() {
        console.log('Demo1 component linked!');
        try {
            this.setup();
        } catch (e) {
            console.error(e);
        }
    }

    $onDestroy() {
        console.log('Demo1 omponent destroyed!');
    }
}

toosla.registerModule("demo1", DemoController1);
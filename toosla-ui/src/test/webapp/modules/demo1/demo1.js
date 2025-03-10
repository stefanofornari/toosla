
class DemoController1 {

    name = "World";
    time = "";

    lastSettingsEvent = "pippo";

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

    settings(event) {
        this.lastSettingsEvent = event;
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

class DemoController {

    name = "World";
    time = "";

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
        console.log("Demo settings", action, status);
    }

    $onInit() {
        console.log('Demo component initialized!');
    };

    $postLink() {
        console.log('Demo component linked!');
        try {
            this.setup();
        } catch (e) {
            console.error(e);
        }
    }

    $onDestroy() {
        console.log('Demo omponent destroyed!');
    }
}

toosla.registerModule("demo", DemoController);
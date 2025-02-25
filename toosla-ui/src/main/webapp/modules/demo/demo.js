
class DemoController {

    currentTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone;  // default timzone for time
    todayDate = ""; // today in the format specified by dateFormat

    dateFormat = new Intl.DateTimeFormat('en-GB', { dateStyle: 'full' });
    timeFormat = new Intl.DateTimeFormat('en-GB', { timeStyle: 'medium', timeZone: 'Europe/Rome' });  // TODO: set a better default

    #taskID = null;

    constructor($scope) {
        this.todayDate = "world";
        this.$scope = $scope;
    }

    updateTime() {

        const today = new Date();
        const timeString = this.timeFormat.format(today);

        this.todayDate = timeString;
    }

    setup() {
        const self = this;

        self.updateTime();
        if (self.#taskID !== null) {
            clearInterval(self.#taskID);
        }
        self.#taskID = setInterval(() => {
            //self.$scope.$apply(() => self.updateTime());
            self.updateTime();
        }, 500);
    }

    $onInit() {
        console.log('Component initialized!');
    };

    $postLink() {
        try {
            this.setup();
        } catch (e) {
            console.error(e);
        }
    }

    $onDestroy() {
        console.log('Component destroyed!');
    }
}

DemoController.$inject = ['$scope'];
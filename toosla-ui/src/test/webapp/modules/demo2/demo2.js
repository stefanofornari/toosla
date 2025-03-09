
class DemoController2 {

    constructor($scope) {
        this.$scope = $scope;
    }

    $onInit() {
        console.log('Demo2 component initialized!');
    };

    $postLink() {
        console.log('Demo2 component linked!');
    }

    $onDestroy() {
        console.log('Demo2 omponent destroyed!');
    }
}

toosla.registerModule("demo2", DemoController2);
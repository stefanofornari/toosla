
class DemoController3 {
    constructor($scope) {
        this.$scope = $scope;
    }

    $onInit() {
        console.log('Demo3 component initialized!');
    };

    $postLink() {
        console.log('Demo3 component linked!');
    }

    $onDestroy() {
        console.log('Demo3 omponent destroyed!');
    }
}

toosla.registerModule("demo3", DemoController3);
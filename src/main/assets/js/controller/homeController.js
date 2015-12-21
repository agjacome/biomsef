define(['./main'], function(controller) {
    'use strict';

    var homeController = ['$rootScope', '$scope', function($rootScope, $scope) {
        $rootScope.terms                 = '';
        $rootScope.selectedTagCategories = $rootScope.categoryTags.slice();
        $rootScope.fromYear              = $rootScope.years[0];
        $rootScope.toYear                = $rootScope.years[$rootScope.years.length - 1];

        $scope.terms                 = '';
        $scope.selectedTagCategories = $rootScope.categoryTags.slice();
        $scope.fromYear              = $rootScope.years[0];
        $scope.toYear                = $rootScope.years[$rootScope.years.length - 1];
    }];

    controller.controller('HomeController', homeController);

});

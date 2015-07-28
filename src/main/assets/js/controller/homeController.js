define(['./main'], function(controller) {
    'use strict';

    var homeController = ['$rootScope', function($rootScope) {
        $rootScope.terms = null;
    }];

    controller.controller('HomeController', homeController);

});

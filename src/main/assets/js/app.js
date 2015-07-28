define([
    'angular',
    'angular-cookies',
    'angular-resource',
    'angular-route',
    'ui-bootstrap',
    'ui-bootstrap-tpls',
    './controller/index',
    './directive/index',
    './filter/index',
    './service/index'
], function(angular) {

    var app = angular.module('biomsef', [
        'ngCookies', 'ngRoute', 'ngResource',
        'ui.bootstrap', 'ui.bootstrap.tpls',
        'biomsef.controller',
        'biomsef.directive',
        'biomsef.filter',
        'biomsef.service',
    ]);

    app.run(['$location', '$rootScope', function($location, $rootScope) {

        var keepError = false;

        $rootScope.$on('$routeChangeSuccess', function(event, current, previous) {
            if (keepError) keepError = false; else $rootScope.error = false;
            $rootScope.pageTitle = (current.$$route && current.$$route.pageTitle) || 'BiomSEF';
        });

        $rootScope.$on('$routeChangeError', function(event, current, previous, rejection) {
            keepError        = true;
            $rootScope.error = true;

            if (rejection.status === 401) {
                $rootScope.errorMessage = 'You do not have enough privileges to perform the requested action.';
                $location.path('/login');
            } else {
                $rootScope.errorMessage = 'Cannot perform the requested action.';
                $location.path('/');
            }
        });

        $rootScope.go = function(path) {
            $location.path(path);
            $location.url($location.path());
        }


    }]);

    return app;

});

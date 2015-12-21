define([
    'angular',
    'cytoscape',
    'angular-cookies',
    'angular-resource',
    'angular-route',
    'ng-tags-input',
    'ui-bootstrap',
    'ui-bootstrap-tpls',
    './controller/index',
    './directive/index',
    './filter/index',
    './service/index'
], function(angular) {

    var app = angular.module('biomsef', [
        'ngCookies', 'ngRoute', 'ngResource', 'ngTagsInput',
        'ui.bootstrap', 'ui.bootstrap.tpls',
        'biomsef.controller',
        'biomsef.directive',
        'biomsef.filter',
        'biomsef.service',
    ]);

    app.run(['$location', '$rootScope', function($location, $rootScope) {

        var keepError = false;

        $rootScope.years = [];
        for (var i = 1900; i <= new Date().getFullYear() + 1; ++i) {
            $rootScope.years.push(i);
        }

        $rootScope.categoryTags = [
            { text: 'Compound' },
            { text: 'Gene'     },
            { text: 'Protein'  },
            { text: 'Species'  },
            { text: 'DNA'      },
            { text: 'RNA'      },
            { text: 'CellLine' },
            { text: 'CellType' },
            { text: 'Disease'  },
            { text: 'Drug'     }
        ];

        $rootScope.categoryAutoComplete = function($query) {
            return _.filter($rootScope.categoryTags, function(cat) {
                return cat.text.toLowerCase().indexOf($query.toLowerCase()) != -1;
            });
        };

        $rootScope.$on('$routeChangeSuccess', function(event, current, previous) {
            if (keepError) keepError = false; else $rootScope.error = false;
            $rootScope.pageTitle = (current.$$route && current.$$route.pageTitle) || 'Biomsef';
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

define(['./main'], function(controller) {
    'use strict';

    var searchController = [
        '$scope', '$location', '$rootScope', '$routeParams',
        function($scope, $location, $rootScope, $routeParams) {
            $scope.terms = $routeParams.terms || $rootScope.terms || '';

            $scope.search = function() {
                if ($scope.terms.trim().length > 0)
                    $location.path('/search').search('terms', $scope.terms);
            }
        }
    ];

    controller.controller('SearchController', searchController);

});

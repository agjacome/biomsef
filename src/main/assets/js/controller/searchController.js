define(['./main'], function(controller) {
    'use strict';

    var catParams = function(catsParam, originalCats) {
        var cs = _.filter(originalCats, function(cat) {
            return _.contains(catsParam, cat.text);
        });

        if (_.isEmpty(cs)) return originalCats.slice();
        return cs;
    };

    var searchController = [
        '$scope', '$location', '$rootScope', '$routeParams',
        function($scope, $location, $rootScope, $routeParams) {
            $scope.terms = $routeParams.terms || $rootScope.terms || '';

            $scope.selectedTagCategories = catParams($routeParams.categories, $rootScope.categoryTags);

            $scope.fromYear = $routeParams.fromYear || $rootScope.years[0];
            $scope.toYear   = $routeParams.toYear   || $rootScope.years[$rootScope.years.length - 1];

            $scope.search = function() {
                if ($scope.terms.trim().length > 0) {
                    $location.path('/search')
                        .search('terms'      , $scope.terms)
                        .search('fromYear'   , $scope.fromYear)
                        .search('toYear'     , $scope.toYear)
                        .search('categories' , _.map($scope.selectedTagCategories, function (c) { return c.text; }));
                }
            }
        }
    ];

    controller.controller('SearchController', searchController);

});

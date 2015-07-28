define(['./main'], function(controller) {
    'use strcit';

    var COUNT_PER_PAGE       = 20;
    var MAX_PAGINATION_LINKS = 5;

    var download = function(service, scope, rootScope, location) {
        scope.downloading = true;

        var ids = _.map(scope.ids.split(','), function(id) {
            return parseInt(id.trim(), 10);
        });

        service.download({ ids : ids }).then(
            function(data) {
                rootScope.error = false;
                location.path('/admin/documents');
            },
            function(response) {
                scope.downloading      = false;
                rootScope.error        = true;
                rootScope.errorMessage = response.data.err;
            }
        );
    };

    var search = function(service, scope, rootScope, location) {
        scope.loading = true;
        service.search(
            scope.search.terms,
            scope.search.limit,
            Math.max(0, scope.pageNumber - 1),
            COUNT_PER_PAGE
        ).then(
            function(response) {
                scope.loading   = false;
                rootScope.error = false;
                scope.searchResults = response.data;
            },
            function(response) {
                scope.loading          = false;
                rootScope.error        = true;
                rootScope.errorMessage = response.data.err;
            }
        );
    };

    var pubmedProviderController = [
        '$scope', '$location', '$rootScope', '$window', 'PubMedProviderService',
        function($scope, $location, $rootScope, $window, PubMedProviderService) {

            $scope.ids    = '';
            $scope.search = {
                terms  : '',
                limiit : null,
            };

            $scope.countPerPage = COUNT_PER_PAGE;
            $scope.maxSize      = MAX_PAGINATION_LINKS;
            $scope.pageNumber   = 1;

            $scope.validIds = function( ) {
                return $scope.ids && _.every(
                    $scope.ids.split(","),
                    function(id) {
                        var n = parseInt(id.trim(), 10);
                        return !isNaN(id.trim()) && !_.isNaN(n) && n > 0;
                    }
                );
            };

            $scope.download = function( ) {
                if ($scope.validIds() && $scope.downloadForm.$valid)
                    download(PubMedProviderService, $scope, $rootScope, $location);
            };

            $scope.addToDownloadIds = function(id) {
                if ($scope.ids && $scope.ids.length > 0)
                    $scope.ids += ', ' + id
                else $scope.ids = '' + id;
            };

            $scope.pageChanged = function( ) {
                search(PubMedProviderService, $scope, $rootScope);
                $window.scrollTo(0, 0);
            };

            $scope.searchIDs = function( ) {
                if ($scope.searchForm.$valid)
                    search(PubMedProviderService, $scope, $rootScope);
            };

        }
    ];

    controller.controller('PubMedProviderController', pubmedProviderController);

});

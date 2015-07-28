define(['./main'], function(controller) {
    'use strict';

    var COUNT_PER_PAGE       = 20;
    var MAX_PAGINATION_LINKS = 5;

    var list = function(service, scope, rootScope) {
        scope.loading = true;
        service.query({
            page : Math.max(0, scope.pageNumber - 1),
            count : COUNT_PER_PAGE
        }).$promise.then(
            function(data) {
                scope.loading   = false;
                rootScope.error = false;
                scope.listing   = data;
            },
            function(error) {
                scope.loading   = false;
                rootScope.error = true;
                rootScope.errorMessage = error.data.err;
            }
        );
    };

    var deleteDocument = function(service, document, scope, rootScope) {
        service.delete({ id : document.id }).$promise.then(
            function(data) {
                list(service, scope, rootScope);
            },
            function(error) {
                rootScope.error = true;
                rootScope.errorMessage = error.data.err;
            }
        );
    };

    var annotateDocument = function(service, document, scope, rootScope) {
        service.annotate({ ids : [document.id] }).then(
            function(data) { document.isProcessing = true; },
            function(error) {
                rootScope.error        = true;
                rootScope.errorMessage = error.data.err;
            }
        );
    };

    var modalController = [
        '$scope', '$modalInstance', function($scope, $modalInstance) {

            $scope.message       = "Are you sure you want to permanently delete the selected document?";
            $scope.okMessage     = "Yes, I'm sure";
            $scope.cancelMessage = "Cancel";

            $scope.ok = function( ) {
                $modalInstance.close();
            };

            $scope.cancel = function( ) {
                $modalInstance.dismiss('cancel');
            };

        }
    ];

    var documentListController = [
        '$scope', '$location', '$rootScope', '$window', '$modal', 'AnnotatorService', 'DocumentService',
        function($scope, $location, $rootScope, $window, $modal, AnnotatorService, DocumentService) {

            $scope.ordering     = 'id';
            $scope.countPerPage = COUNT_PER_PAGE;
            $scope.maxSize      = MAX_PAGINATION_LINKS;
            $scope.pageNumber   = 1;

            $scope.pageChanged = function( ) {
                list(DocumentService, $scope, $rootScope);
                $window.scrollTo(0, 0);
            };

            $scope.goToDocument = function(document) {
                $location.path('/document/' + document.id);
            };

            $scope.annotateDocument = function(document) {
                annotateDocument(AnnotatorService, document, $scope, $rootScope);
            };

            $scope.deleteDocument = function(document) {
                var modal = $modal.open({
                    templateUrl : 'assets/html/confirmationDialog.html',
                    controller  : modalController,
                });

                modal.result.then(function( ) {
                    deleteDocument(DocumentService, document, $scope, $rootScope);
                });
            };

            list(DocumentService, $scope, $rootScope);

        }
    ];

    controller.controller('DocumentListController', documentListController);

});

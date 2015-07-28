define(['./main'], function(controller) {

    var createDocument = function(service, scope, rootScope, location) {
        scope.loading = true;
        service.save(scope.document).$promise.then(
            function(data) {
               rootScope.error = false;
               location.path('/document/' + data.id);
            },
            function(response) {
                scope.loading          = false;
                rootScope.error        = true;
                rootScope.errorMessage = response.data.err;
            }
        );
    };

    var documentNewController = [
        '$scope', '$location', '$rootScope', 'DocumentService',
        function($scope, $location, $rootScope, DocumentService) {

            $scope.document = {
                title    : '',
                content  : '',
                pubmedId : null
            };

            $scope.createDocument = function( ) {
                if ($scope.documentForm.$valid)
                    createDocument(DocumentService, $scope, $rootScope, $location);
            };

        }
    ];

    controller.controller('DocumentNewController', documentNewController);

});

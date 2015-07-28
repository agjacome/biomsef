define(['./main'], function(controller) {
    'use strict';

    var adminBarController = [
        '$scope', '$location', 'AuthorizationService', 'AccountService',
        function($scope, $location, AuthorizationService, AccountService) {

            $scope.accountId = AuthorizationService.accountId();
            AccountService.get({ id : $scope.accountId }).$promise.then(
                function(data) { $scope.accountEmail = data.email; }
            );

            $scope.goTo = function(path) {
                $location.path(path);
            };

        }
    ];

    controller.controller('AdminBarController', adminBarController);

});

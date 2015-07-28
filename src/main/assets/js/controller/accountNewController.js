define(['./main'], function(controller) {
    'use strict';

    var createAccount = function(service, scope, rootScope, location) {
        scope.loading = true;
        service.save(scope.account).$promise.then(
            function(data) {
                rootScope.error = false;
                location.path('/admin/accounts');
            },
            function(response) {
                scope.loading          = false;
                rootScope.error        = true;
                rootScope.errorMessage = response.data.err;
            }
        );
    };

    var accountNewController = [
        '$scope', '$location', '$rootScope', 'AccountService',
        function($scope, $location, $rootScope, AccountService) {

            $scope.account = {
                email    : '',
                password : '',
            };

            $scope.createAccount = function() {
                if ($scope.accountForm.$valid)
                    createAccount(AccountService, $scope, $rootScope, $location);
            };

        }
    ];

    controller.controller('AccountNewController', accountNewController);

});

define(['./main'], function(controller) {
    'use strict';

    var getAccount =  function(service, id, scope, rootScope) {
        service.get({ id : id }).$promise.then(
            function(data) {
                scope.account = {
                    id    : data.id,
                    email : data.email,
                    pass  : '',
                };

                rootScope.error     = false;
                rootScope.pageTitle = data.email + rootScope.pageTitle;
            },
            function(response) {
                rootScope.error        = true;
                rootScope.errorMessage = response.data.err;
                rootScope.pageTitle    = 'Error ' + rootScope.pageTitle;
            }
        );
    };

    var editAccount = function(service, scope, rootScope, location, currentId) {
        scope.loading = true;
        service.update(scope.account).$promise.then(
            function(data) {
                scope.loading   = false;
                rootScope.error = false;
                if (scope.account.id === currentId) location.path('/logout');
                else location.path('/admin/accounts');
            },
            function(response) {
                scope.loading          = false;
                rootScope.error        = true;
                rootScope.errorMessage = error.data.err;
            }
        );
    };

    var accountEditController = [
        '$scope', '$location', '$routeParams', '$rootScope', 'AccountService', 'AuthorizationService',
        function($scope, $location, $routeParams, $rootScope, AccountService, AuthorizationService) {

            var accountId = $routeParams.id;
            var currentId = AuthorizationService.accountId();
            if (!(accountId)) $location.path('/');

            $scope.account = {
                id    : accountId,
                email : '',
                pass  : '',
            };

            $scope.editAccount = function( ) {
                if ($scope.accountForm.$valid)
                    editAccount(AccountService, $scope, $rootScope, $location, currentId);
            };

            getAccount(AccountService, accountId, $scope, $rootScope);

        }
    ];

    controller.controller('AccountEditController', accountEditController);

});

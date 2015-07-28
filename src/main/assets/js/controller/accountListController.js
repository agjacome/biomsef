define(['./main'], function(controller) {
    'use strcit';

    var COUNT_PER_PAGE       = 10;
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
                scope.loading          = false;
                rootScope.error        = true;
                rootScope.errorMessage = error.data.err;
            }
        );
    };

    var deleteAccount = function(service, account, currentId, scope, rootScope, location) {
        service.delete({ id : account.id }).$promise.then(
            function(data) {
                if (currentId === account.id) location.path('/logout');
                else list(service, scope, rootScope);
            },
            function(error) {
                rootScope.error        = true;
                rootScope.errorMessage = error.data.err;
            }
        );
    };

    var modalController = [
        '$scope', '$modalInstance', function($scope, $modalInstance) {

            $scope.message       = "Are you sure you want to permanently delete the seleced account?";
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

    var accountListController = [
        '$scope', '$location', '$rootScope', '$window', '$modal', 'AccountService', 'AuthorizationService',
        function($scope, $location, $rootScope, $window, $modal, AccountService, AuthorizationService) {

            $scope.ordering     = 'id';
            $scope.countPerPage = COUNT_PER_PAGE;
            $scope.maxSize      = MAX_PAGINATION_LINKS;
            $scope.pageNumber   = 1;

            $scope.pageChanged = function( ) {
                list(AccountService, $scope, $rootScope);
                $window.scrollTo(0, 0);
            };

            $scope.editAccount = function(account) {
                $location.path('/admin/account/edit/' + account.id);
            };

            $scope.cannotDelete = function(account) {
                return $scope.listing.total === 1;
            };

            $scope.deleteAccount = function(account) {
                if ($scope.cannotDelete(account)) return;

                var currentId = AuthorizationService.accountId();

                var modal = $modal.open({
                    templateUrl : 'assets/html/confirmationDialog.html',
                    controller  : modalController,
                });

                modal.result.then(function( ) {
                    deleteAccount(AccountService, account, currentId, $scope, $rootScope, $location);
                });
            };

            list(AccountService, $scope, $rootScope);

        }
    ];

    controller.controller('AccountListController', accountListController);

});

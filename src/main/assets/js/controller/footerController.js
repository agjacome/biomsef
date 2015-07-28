define(['./main'], function(controller) {
    'use strict';

    var footerController = [
        '$scope', 'AuthorizationService',
        function($scope, AuthorizationService) {

            $scope.loggedIn = !!AuthorizationService.accountId();

            $scope.$watch(AuthorizationService.accountId, function(value) {
                $scope.loggedIn = !!value;
            });

        }
    ];

    controller.controller('FooterController', footerController);

});

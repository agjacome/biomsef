define(['./main'], function(controller) {

    var authorizationController = [
        '$scope', '$location', '$rootScope', '$cookies', 'AuthorizationService',
        function($scope, $location, $rootScope, $cookies, AuthorizationService) {

            var token = $cookies['XSRF-TOKEN'];
            if (token) {
                AuthorizationService.ping().then(
                    function(response) { $location.path('/admin'); },
                    function(response) {
                        token                  = undefined;
                        $cookies['XSRF-TOKEN'] = undefined;
                    }
                );
            }

            $scope.credentials = { email : '', pass : '' };

            $scope.login = function( ) {
                if ($scope.loginForm.$valid) {
                    AuthorizationService.login($scope.credentials).then(
                        function(response) { $location.path('/admin'); },
                        function(response) {
                            $rootScope.error        = true;
                            $rootScope.errorMessage = response.data.err;
                        }
                    );
                }
            };

        }
    ];

    controller.controller('AuthorizationController', authorizationController);

});

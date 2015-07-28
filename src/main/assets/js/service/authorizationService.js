define(['./main'], function(service) {
    'use strict';

    service.factory('AuthorizationService', [
        '$http', '$cookies', function($http, $cookies) {

            var accountId;

            return {
                login : function(credentials) {
                    return $http.post('api/login', credentials);
                },
                logout : function( ) {
                    return $http.post('api/logout');
                },
                ping : function( ) {
                    return $http.get('api/auth_ping');
                },
                accountId : function( ) {
                    return accountId;
                },
                setAccountId : function(id) {
                    accountId = id;
                },
            }

        }
    ]);

});

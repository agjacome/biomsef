define(['./main'], function(service) {
    'use strict';

    service.factory('AccountService', ['$resource', function($resource) {
        return $resource('api/user/:id', { id : '@id' }, {
            query  : { isArray : false },
            update : { method  : 'PUT' },
        });
    }]);

});

define(['./main'], function(service) {
    'use strict';

    service.factory('DocumentService', ['$resource', function($resource) {
        return $resource('api/article/:id', { id : '@id' }, {
            query : { isArray : false },
        });
    }]);

});

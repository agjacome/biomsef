define(['./main'], function(service) {
    'use strict';

    service.factory('SearchService', ['$http', function($http) {
        return {
            search : function(terms, page, count) {
                return $http.get('api/search', {
                    params: {
                        query: terms,
                        page : page,
                        count: count
                    }
                });
            }
        }
    }]);

});

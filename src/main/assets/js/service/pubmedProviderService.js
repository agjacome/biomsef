define(['./main'], function(service) {
    'use strict';

    service.factory('PubMedProviderService', ['$http', function($http) {
        return {
            search : function(terms, limit, page, count) {
                return $http.get('api/provider/pubmed/search', {
                    params : {
                        query: terms,
                        limit: limit,
                        page : page,
                        count: count,
                    }
                });
            },
            download : function(ids) {
                return $http.post('api/provider/pubmed/download', ids);
            },
        }
    }]);

});

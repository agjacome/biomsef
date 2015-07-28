define(['./main'], function(service) {
    'use strict';

    service.factory('AnnotatorService', ['$http', function($http) {
        return {
            annotate : function(ids) {
                return $http.post('api/annotator/annotate', ids);
            },
        }
    }]);

});

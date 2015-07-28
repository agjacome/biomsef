define(['./main'], function(filter) {
    'use strict';

    filter.filter('truncate', function( ) {
        return function(input, chars) {
            if (isNaN(chars)) return input;
            if (chars <= 0)   return '';

            if (input && input.length > chars)
                return input.substring(0, chars) + '...';

            return input;
        };
    });

});

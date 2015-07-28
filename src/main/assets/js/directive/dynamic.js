define(['./main'], function(directive) {
    'use strict';

    var dynamicDirective = ['$compile', '$parse', function($compile, $parse) {
        return {
            link : function(scope, element, attr) {
                var parsed = $parse(attr.ngBindHtml);
                var getStringValue = function( ) {
                    return (parsed(scope) || '').toString();
                }

                scope.$watch(getStringValue, function( ) {
                    $compile(element, null, -9999)(scope);
                });
            }
        }
    }];

    directive.directive('dynamic', dynamicDirective);

});

(function(requirejs) {
    'use strict';

    requirejs.onError = function(err) {
        console.log(err);
    };

    requirejs.config({
        paths: {
            'requirejs'         : ['../lib/requirejs/require'],
            'jquery'            : ['../lib/jquery/jquery'],
            'angular'           : ['../lib/angularjs/angular'],
            'angular-cookies'   : ['../lib/angularjs/angular-cookies'],
            'angular-resource'  : ['../lib/angularjs/angular-resource'],
            'angular-route'     : ['../lib/angularjs/angular-route'],
            'ng-tags-input'     : ['../lib/ng-tags-input/ng-tags-input'],
            'bootstrap'         : ['../lib/bootstrap/js/bootstrap'],
            'underscore'        : ['../lib/underscorejs/underscore'],
            'ui-bootstrap'      : ['../lib/angular-ui-bootstrap/ui-bootstrap'],
            'ui-bootstrap-tpls' : ['../lib/angular-ui-bootstrap/ui-bootstrap-tpls'],
            'cytoscape'         : 'cytoscape/cytoscape.min'
        },
        shim: {
            'angular'           : { deps: ['jquery'], exports: 'angular' },
            'angular-cookies'   : ['angular'],
            'angular-resource'  : ['angular'],
            'angular-route'     : ['angular'],
            'ng-tags-input'     : ['angular'],
            'bootstrap'         : ['jquery'],
            'ui-bootstrap'      : ['bootstrap', 'angular'],
            'ui-bootstrap-tpls' : ['bootstrap', 'angular'],
        }
    });

    require([
        'angular',
        'cytoscape',
        'jquery',
        'underscore',
        'bootstrap',
        './app',
        './routes',
        './util'
    ], function(angular) {

        angular.bootstrap(document, ['biomsef']);

    });

})(requirejs);

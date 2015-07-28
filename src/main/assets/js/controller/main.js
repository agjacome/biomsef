define([
    'angular', 'directive/index', 'filter/index', 'service/index',
], function(angular) {
    'use strict';

    return angular.module('biomsef.controller', [
        'biomsef.directive',
        'biomsef.filter',
        'biomsef.service'
    ]);

});

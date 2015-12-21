define(['./main', 'cytoscape'], function(controller, cytoscape) {
    'use strict';

    var COLORS = {
        'Compound' : '#59ABE3',
        'Gene'     : '#BF55EC',
        'Protein'  : '#F39C12',
        'Species'  : '#2ECC71',
        'DNA'      : '#E74C3C',
        'RNA'      : '#95A5A6',
        'CellLine' : '#96281B',
        'CellType' : '#34495E',
        'Disease'  : '#222222',
        'Drug'     : '#D2527F',
    };

    var COUNT_PER_PAGE       = 10;
    var MAX_PAGINATION_LINKS =  5;

    var catParams = function(catsParam, originalCats) {
        var cs = _.filter(originalCats, function(cat) {
            return _.contains(catsParam, cat.text);
        });

        if (_.isEmpty(cs)) return originalCats.slice();
        return cs;
    };

    var minimizeCompounds = function(results) {
        _.each(results, function(result) {
            _.each(result.keywords, function(keyword) {
                if (keyword.category === 'Compound')
                    keyword.normalized = keyword.normalized.split('/')[1];
            });
        });
    };


    var getGraph = function(results) {
        var elements = [];

        var addedNodes = [];
        var addedEdges = [];

        _.each(results, function(doc) {
            _.each(doc.keywords, function(k1) {
                _.each(doc.keywords, function(k2) {
                    if (!_.contains(addedNodes, k1.id)) {
                        addedNodes.push(k1.id);
                        elements.push({ data: { id: k1.id, name: k1.normalized, color: COLORS[k1.category], category: k1.category }});
                    }
                    if (!_.contains(addedNodes, k2.id)) {
                        addedNodes.push(k2.id);
                        elements.push({ data: { id: k2.id, name: k2.normalized, color: COLORS[k2.category], category: k2.category }});
                    }
                    if (k1.id !== k2.id) {
                        if (!_.contains(addedEdges, k1.id + "->" + k2.id) && !_.contains(addedEdges, k2.id + "->" + k1.id)) {
                            addedEdges.push(k1.id + "->" + k2.id);
                            elements.push({ data: { id: k1.id + "↔" + k2.id, source: k1.id, target: k2.id, counter: 1 }});
                        } else {
                            _.each(elements, function(e) {
                                if (e.data.id === k1.id + "↔" + k2.id || e.data.id === k2.id + "↔" + k1.id)
                                    e.data.counter += 1;
                            });
                        }
                    }
                });
            });
        });

        return elements;
    }

    var search = function(service, scope, rootScope, hasGraph) {
        scope.loading         = true;
        scope.filteredResults = false;

        scope.uncheckFilters();
        if (scope.graph) {
            scope.graph.stop();
            scope.graph = null;
        }

        var cats = _.map(scope.selectedTagCategories, function (c) { return c.text.toLowerCase(); });
        service.advSearch(scope.terms, Math.max(0, scope.pageNumber - 1), COUNT_PER_PAGE, cats, scope.fromYear, scope.toYear).then(
            function(response) {
                scope.loading   = false;
                rootScope.error = false;

                scope.results = response.data;
                minimizeCompounds(response.data.items);
                scope.results.originalItems = scope.results.items.slice();

                scope.createGraph = function() {
                    var cy = cytoscape({
                        container: document.getElementById("graph-container"),

                        elements: getGraph(response.data.originalItems),

                        layout: { name: 'cose' },

                        style: [
                            { selector: 'node', style: {
                                'background-color': 'data(color)',
                                'label': 'data(name)',
                                'color': '#444',
                                'text-outline-width': 1,
                                'text-outline-color': '#888',
                            }},
                            { selector: 'edge', style: {
                                'line-color': '#ccc',
                                'width': 'data(counter)',
                            }},
                            { selector: '.faded', style: {
                                'opacity': 0.30,
                                'text-opacity': 0,
                            }},
                            { selector: '.path', style: {
                                'line-color': '#00f',
                                'width': 5,
                            }},
                            { selector: '.origin', style: {
                                'border-width': 6,
                                'border-color': '#f00',
                            }},
                            { selector: '.destination', style: {
                                'border-width': 5,
                                'border-color': '#0f0',
                            }},
                        ]
                    });

                    cy.on('tap', 'edge', function(e) {
                        var nodes = e.cyTarget.id().split("↔");

                        scope.filteredResults = _.filter(scope.results.originalItems, function(item) {
                            var keywordIds = _.map(item.keywords, function(k) { return "" + k.id });
                            return _.contains(keywordIds, nodes[0]) &&
                                   _.contains(keywordIds, nodes[1]);
                        });

                        scope.$apply();
                    });

                    var origin      = null;
                    var destination = null;

                    var selectNeighbors = function(e) {
                        origin = e.cyTarget;

                        cy.elements().removeClass('origin');
                        origin.addClass('origin');

                        var neighbors = origin.closedNeighborhood();

                        cy.elements().addClass('faded');

                        if (neighbors) {
                            neighbors.removeClass('faded');
                        }
                    };

                    var shortestPath = function(e) {
                        if (origin && e.cyTarget !== origin) {
                            destination = e.cyTarget;

                            cy.elements().removeClass('selected');
                            cy.elements().removeClass('destination');
                            destination.addClass('destination');

                            var path = cy.elements().aStar({ root: origin, goal: destination });
                            cy.elements().addClass('faded');
                            origin.removeClass('faded');
                            destination.removeClass('faded');

                            if (path.found) {
                                path.path.edges().addClass('path');
                                path.path.removeClass('faded');
                            }
                        }
                    };

                    var clear = function() {
                        origin      = null;
                        destination = null;

                        cy.elements().removeClass('origin');
                        cy.elements().removeClass('destination');
                        cy.elements().removeClass('faded');
                        cy.elements().removeClass('path');
                    };

                    cy.on('tap', 'node', function(e) {
                        if (destination) clear();

                        if (origin) shortestPath(e);
                        else        selectNeighbors(e);
                    });

                    cy.on('tap', function(e) {
                        if (e.cyTarget === cy) clear();
                    });

                    return cy;
                };

                if (hasGraph) scope.refreshGraph();
            },
            function(error) {
                scope.loading          = false;
                rootScope.error        = true;
                rootScope.errorMessage = error.data.err;
            }
        );
    };

    var searchResultsController = [
        '$scope', '$location', '$rootScope', '$routeParams', '$window', 'SearchService',
        function($scope, $location, $rootScope, $routeParams, $window, SearchService) {

            $scope.terms = $routeParams.terms || '';
            if (!$scope.terms.trim()) $location.path('/').search('terms', null)

            $scope.selectedTagCategories = catParams($routeParams.categories, $rootScope.categoryTags);
            $scope.fromYear = $routeParams.fromYear || $rootScope.years[0];
            $scope.toYear   = $routeParams.toYear   || $rootScope.years[$rootScope.years.length - 1];

            $scope.countPerPage = COUNT_PER_PAGE;
            $scope.maxSize      = MAX_PAGINATION_LINKS;
            $scope.pageNumber   = 1;
            $scope.categories   = [
                { 'name': 'Compound', 'selected': false },
                { 'name': 'Gene'    , 'selected': false },
                { 'name': 'Protein' , 'selected': false },
                { 'name': 'Species' , 'selected': false },
                { 'name': 'DNA'     , 'selected': false },
                { 'name': 'RNA'     , 'selected': false },
                { 'name': 'CellLine', 'selected': false },
                { 'name': 'CellType', 'selected': false },
                { 'name': 'Disease' , 'selected': false },
                { 'name': 'Drug'    , 'selected': false },
            ];

            $rootScope.pageTitle = $scope.terms + $rootScope.pageTitle;

            $scope.pageChanged = function( ) {
                search(SearchService, $scope, $rootScope, $scope.graph);
            };

            $scope.goToDocument = function(d) {
                $rootScope.terms = $routeParams.terms;
                $location.path('/document/' + d.id).search('terms', null);
            };

            $scope.filterByCategoryList = function(c) {
                var cat = _.find($scope.categories, function (cat) {
                    return cat.name == c.name;
                });

                _.each($scope.categories, function (cat) {
                    if (cat !== c) cat.selected = false;
                });
                cat.selected = !cat.selected;

                if (!$scope.results) return;

                $scope.results.items = _.filter($scope.results.originalItems, function (item) {
                    if (!cat.selected) return true;

                    var cats = _.map(item.keywords, function (k) { return k.category; });
                    return _.contains(cats, cat.name);
                });
            };

            $scope.uncheckFilters = function() {
                _.each($scope.categories, function (cat) {
                    cat.selected = false;
                });
            };

            var removedNodes = null;

            $scope.filterByCategory = function(cat) {
                cat.selected = !cat.selected;

                if (removedNodes) removedNodes.restore();

                if (!_.every($scope.categories, function(c) { return !c.selected; })) {
                    removedNodes = $scope.graph.nodes().filter(function(i, node) {
                        return _.some($scope.categories, function (c) {
                            return c.name === node.data('category') && !c.selected;
                        });
                    }).remove();
                }
            };

            $scope.refreshGraph = function() {
                $scope.graph = $scope.createGraph();
            };

            search(SearchService, $scope, $rootScope, false);
        }
    ];

    controller.controller('SearchResultsController', searchResultsController);

});

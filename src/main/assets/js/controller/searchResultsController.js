define(['./main'], function(controller) {
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

    sigma.classes.graph.addMethod('neighbors', function(nodeId) {
        var k,
        neighbors = {},
        index = this.allNeighborsIndex[nodeId] || {};

        for (k in index)
            neighbors[k] = this.nodesIndex[k];

        return neighbors;
    });

    var minimizeCompounds = function(results) {
        _.each(results, function(result) {
            _.each(result.keywords, function(keyword) {
                if (keyword.category === 'Compound')
                    keyword.normalized = keyword.normalized.split('/')[1];
            });
        });
    };

    var getGraph = function(results) {
        var graph = { nodes: [ ], edges: [ ] };

        var keywords = _.flatten(_.map(results, function(result) {
            return result.keywords;
        }));

        var nodeIds = [ ];
        _.each(keywords, function(keyword) {
            var node = {
                id            : '' + keyword.id,
                x             : Math.random(),
                y             : Math.random(),
                size          : 10,
                label         : keyword.normalized,
                originalLabel : keyword.normalized,
                category      : keyword.category,
                color         : COLORS[keyword.category],
                originalColor : COLORS[keyword.category]
            };

            if (!_.contains(nodeIds, node.id)) {
                graph.nodes.push(node)
                nodeIds.push(node.id);
            } else {
                var found = _.find(graph.nodes, function(n) { return n.id === node.id });
                found.size += 2;
            }
        });

        var edgeIds = [ ];
        _.each(results, function(doc) {
            _.each(doc.keywords, function(k1) {
                _.each(doc.keywords, function(k2) {
                    var edge = {
                        id            : '' + k1.id + '-' + k2.id,
                        source        : '' + k1.id,
                        target        : '' + k2.id,
                        size          : 10,
                        color         : '#444444',
                        originalColor : '#444444',
                    };

                    var inverseId = '' + k2.id + '-' + k1.id;
                    if (!_.contains(edgeIds, edge.id)) {
                        graph.edges.push(edge);
                        edgeIds.push(edge.id);
                        edgeIds.push(inverseId);
                    } else {
                        var found = _.find(graph.edges, function(e) {
                            return e.id === edge.id || e.id === inverseId;
                        });
                        found.size += 10;
                    }
                });
            });
        });


        return graph;
    };

    var bindClickToSigma = function(scope) {
        var s = scope.sigmaGraph;

        s.bind('clickNode', function(e) {
            scope.uncheckFilters();
            scope.$apply();

            var nodeId = e.data.node.id,
            toKeep = s.graph.neighbors(nodeId);
            toKeep[nodeId] = e.data.node;

            s.graph.nodes().forEach(function(n) {
                if (toKeep[n.id]) {
                    n.color = n.originalColor;
                    n.label = n.originalLabel;
                } else {
                    n.color = '#eee';
                    n.label = '';
                }
            });

            s.graph.edges().forEach(function(e) {
                if (toKeep[e.source] && toKeep[e.target])
                    e.color = e.originalColor;
                else
                    e.color = '#eee';
            });

            s.refresh();
        });

        s.bind('clickStage', function(e) {
            scope.uncheckFilters();
            scope.$apply();

            s.graph.nodes().forEach(function(n) {
                n.color = n.originalColor;
                n.label = n.originalLabel;
            });

            s.graph.edges().forEach(function(e) {
                e.color = e.originalColor;
            });

            s.refresh();
        });
    };

    var search = function(service, scope, rootScope) {
        scope.loading = true;
        scope.sigmaGraph.kill();

        service.search(scope.terms, Math.max(0, scope.pageNumber - 1), COUNT_PER_PAGE).then(
            function(response) {
                scope.loading   = false;
                rootScope.error = false;

                scope.results = response.data;
                minimizeCompounds(response.data.items);

                scope.sigmaGraph = new sigma({
                    graph: getGraph(response.data.items),
                    container: 'graph-container',
                });

                // bindClickToSigma(scope.sigmaGraph, scope.categories);
                bindClickToSigma(scope);

                scope.refreshGraph();
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

            $scope.countPerPage = COUNT_PER_PAGE;
            $scope.maxSize      = MAX_PAGINATION_LINKS;
            $scope.pageNumber   = 1;
            $scope.sigmaGraph   = new sigma();
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
                search(SearchService, $scope, $rootScope);
                $window.scrollTo(0, 0);
            };

            $scope.goToDocument = function(d) {
                $rootScope.terms = $routeParams.terms;
                $location.path('/document/' + d.id).search('terms', null);
            };

            $scope.uncheckFilters = function() {
                _.each($scope.categories, function (cat) {
                    cat.selected = false;
                });
            };

            $scope.filterByCategory = function(c) {
                var cat = _.find($scope.categories, function (cat) {
                    return cat.name == c.name;
                });

                _.each($scope.categories, function (cat) {
                    if (cat !== c) cat.selected = false;
                });
                cat.selected = !cat.selected;

                _.each($scope.sigmaGraph.graph.nodes(), function (node) {
                    node.color = node.originalColor;
                    node.label = node.originalLabel;

                    if (cat.selected && node.category !== cat.name) {
                        node.color = '#eee';
                        node.label = '';
                    }
                });

                _.each($scope.sigmaGraph.graph.edges(), function (edge) {
                    edge.color = edge.originalColor;

                    var source = $scope.sigmaGraph.graph.nodes(edge.source);
                    var target = $scope.sigmaGraph.graph.nodes(edge.target);
                    if (cat.selected && (source.category !== cat.name || target.category !== cat.name)) {
                        edge.color = "#eee";
                    }
                });

                $scope.sigmaGraph.refresh();
            };

            $scope.refreshGraph = function() {
                // nasty hack, sleep for 500ms before refreshing, needed
                // because the graph is inside a bootstrap tab, and does not
                // have proper sizes (required by sigma) until it is fully
                // rendered
                window.setTimeout(function() {
                    $scope.sigmaGraph.refresh();
                    window.dispatchEvent(new Event('resize'));
                }, 500);
            };

            search(SearchService, $scope, $rootScope);
        }
    ];

    controller.controller('SearchResultsController', searchResultsController);

});

/* ==========================================
 * JGraphT : a free Java graph-theory library
 * ==========================================
 *
 * Project Info:  http://jgrapht.sourceforge.net/
 * Project Creator:  Barak Naveh (http://sourceforge.net/users/barak_naveh)
 *
 * (C) Copyright 2003-2008, by Barak Naveh and Contributors.
 *
 * This program and the accompanying materials are dual-licensed under
 * either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation, or (at your option) any
 * later version.
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */
/* -----------------
 * ClarksonTwoApproxVCImpl.java
 * -----------------
 * (C) Copyright 2016, by Joris Kinable and Contributors.
 *
 * Original Author:  Joris Kinable
 * Contributor(s):   -
 *
 * $Id$
 *
 * Changes
 * -------
 * 28-Jul-2016 : Initial revision (JK);
 *
 */
package org.jgrapht.alg.vertexcover;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.interfaces.MinimumWeightedVertexCoverAlgorithm;
import org.jgrapht.alg.vertexcover.util.RatioVertex;

import java.util.*;

/**
 * Implementation of the 2-opt algorithm for a minimum weighted vertex cover by
 * Clarkson, Kenneth L. "A modification of the greedy algorithm for vertex cover." Information Processing Letters 16.1 (1983): 23-25.
 * The solution is guaranteed to be within 2 times the optimum solution. Runtime: O(|E|*log|V|)
 *
 * Note: this class supports pseudo-graphs
 *
 * @author Joris Kinable
 *
 */
public class ClarksonTwoApproxVCImpl<V,E> implements MinimumWeightedVertexCoverAlgorithm<V,E> {

    private static int vertexCounter=0;

    @Override
    public VertexCover<V> getVertexCover(UndirectedGraph<V,E> graph, Map<V, Double> vertexWeightMap) {
        //Result
        Set<V> cover=new LinkedHashSet<>();
        double weight=0;

        //Create working graph: for every vertex, create a RatioVertex which maintains its own list of neighbors
        Map<V, RatioVertex<V>> vertexEncapsulationMap=new HashMap<>();
        graph.vertexSet().stream().filter(v -> graph.degreeOf(v) > 0).forEach(v -> vertexEncapsulationMap.put(v, new RatioVertex<V>(vertexCounter++, v, vertexWeightMap.get(v))));

        for(E e : graph.edgeSet()){
            V u=graph.getEdgeSource(e);
            RatioVertex<V> ux=vertexEncapsulationMap.get(u);
            V v=graph.getEdgeTarget(e);
            RatioVertex<V> vx=vertexEncapsulationMap.get(v);
            ux.addNeighbor(vx);
            vx.addNeighbor(ux);

            assert(ux.neighbors.get(vx) == vx.neighbors.get(ux)): " in an undirected graph, if vx is a neighbor of ux, then ux must be a neighbor of vx";
        }

        TreeSet<RatioVertex<V>> workingGraph=new TreeSet<>();
        workingGraph.addAll(vertexEncapsulationMap.values());
        assert(workingGraph.size() == graph.vertexSet().size());

        while(!workingGraph.isEmpty()){ //Continue until all edges are covered

            //Find a vertex vx for which W(vx)/degree(vx) is minimal
            RatioVertex<V> vx=workingGraph.pollFirst();
            assert(workingGraph.parallelStream().allMatch(ux -> vx.getRatio() <= ux.getRatio())) : "vx does not have the smallest ratio among all elements. VX: "+vx+" WorkingGraph: "+workingGraph;

            //Iterate over all the neighbors ux of vx and update ux.W
            double ratio=vx.getRatio();
            for(RatioVertex<V> nx : vx.neighbors.keySet()){

                if(nx ==vx) //Ignore self loops
                    continue;

                workingGraph.remove(nx);
                nx.weight-=ratio*vx.neighbors.get(nx);

                //Delete vx from nx' neighbor list. Delete nx from the graph and place it back, thereby updating the ordering of the graph
                nx.removeNeighbor(vx);

                if (nx.getDegree() > 0)
                    workingGraph.add(nx);

            }

            //Update cover
            cover.add(vx.v);
            weight+=vertexWeightMap.get(vx.v);
            assert(!workingGraph.parallelStream().anyMatch(ux -> ux.ID==vx.ID)) : "vx should no longer exist in the working graph";
        }

        return new VertexCover<>(cover, weight);

    }
}

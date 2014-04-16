/*
 * Copyright 2014 CITIUS <http://citius.usc.es>, University of Santiago de Compostela.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package es.usc.citius.hipster.algorithm;


import com.google.common.base.Stopwatch;
import es.usc.citius.hipster.model.Node;

import java.util.*;

public abstract class Algorithm<A,S,N extends Node<A,S,N>> implements Iterable<N> {

    private S goalState = null;

    /**
     * Holds information about the search process.
     */
    public final class SearchResult {
        Stopwatch stopwatch;
        int iterations;
        Collection<N> goalNodes;


        public SearchResult(N goalNode, int iterations, Stopwatch stopwatch) {
            this.goalNodes = Collections.singletonList(goalNode);
            this.iterations = iterations;
            this.stopwatch = stopwatch;
        }

        public SearchResult(Collection<N> goalNodes, int iterations, Stopwatch stopwatch) {
            this.goalNodes = goalNodes;
            this.iterations = iterations;
            this.stopwatch = stopwatch;
        }

        /**
         * Returns a stopped {@link Stopwatch} with the total search time.
         * Use stopwatch.toString() to print the formatted time.
         * @return stopwatch with the total search time.
         */
        public Stopwatch getStopwatch() {
            return stopwatch;
        }

        /**
         * Number of iterations performed by the search algorithm.
         * @return number of iterations.
         */
        public int getIterations() {
            return iterations;
        }

        /**
         * @return goal node.
         */
        public N getGoalNode() {
            return goalNodes.iterator().next();
        }

        public Collection<N> getGoalNodes() {
            return goalNodes;
        }

        public List<List<S>> getOptimalPaths() {
            List<List<S>> paths = new ArrayList<List<S>>(goalNodes.size());
            for(N goalNode : goalNodes){
                paths.add(recoverStatePath(goalNode));
            }

            return paths;
        }

        @Override
        public String toString() {
            return "SearchResult {" +
                    "SearchTime=" + stopwatch +
                    ", totalIterations=" + iterations +
                    ", solutions=" + getOptimalPaths() +
                    ", goalNodes=" + goalNodes +
                    '}';
        }
    }

    public interface SearchListener<N> {
        void handle(N node);
    }

    private SearchResult search(Iterator<N> it){
        int iteration = 0;
        Stopwatch w = Stopwatch.createStarted();
        N currentNode = null;
        while(it.hasNext()){
            iteration++;
            currentNode = it.next();
            if (goalState != null) {
                if (currentNode.state().equals(this.goalState)) {
                    break;
                }
            }
        }
        w.stop();
        return new SearchResult(currentNode, iteration, w);
    }

    /**
     * Run the algorithm until the goal is found or no more states are
     * available.
     * @return SearchResult with the information of the search
     */
    public SearchResult search(){
        return search(iterator());
    }

    /**
     * Executes the search algorithm and invokes the method
     * {@link SearchListener#handle(Object)} passing the current
     * explored node to the listener.
     *
     * <pre>
     * {@code
     *    TODO;
     * }
     * </pre>
     *
     * @param listener listener used to receive the explored nodes.
     */
    public void search(SearchListener<N> listener){
        Iterator<N> it = iterator();
        while(it.hasNext()){
            listener.handle(it.next());
        }
    }

    public static <S, N extends Node<?,S,N>>  List<S> recoverStatePath(N node){
        List<S> states = new LinkedList<S>();
        for(N n : node.path()){
            states.add(n.state());
        }
        Collections.reverse(states);
        return states;
    }

    public S getGoalState() {
        return goalState;
    }

    public void setGoalState(S goalState) {
        this.goalState = goalState;
    }
}
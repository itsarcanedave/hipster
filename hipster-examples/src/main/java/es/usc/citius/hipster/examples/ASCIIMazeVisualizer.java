package es.usc.citius.hipster.examples;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import es.usc.citius.hipster.algorithm.Hipster;
import es.usc.citius.hipster.model.Node;
import es.usc.citius.hipster.model.Transition;
import es.usc.citius.hipster.model.function.CostFunction;
import es.usc.citius.hipster.model.function.HeuristicFunction;
import es.usc.citius.hipster.model.function.impl.StateTransitionFunction;
import es.usc.citius.hipster.model.impl.WeightedNode;
import es.usc.citius.hipster.model.problem.ProblemBuilder;
import es.usc.citius.hipster.model.problem.SearchProblem;
import es.usc.citius.hipster.util.examples.maze.Maze2D;
import es.usc.citius.hipster.util.examples.maze.Mazes;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * @author Pablo Rodríguez Mier <<a href="mailto:pablo.rodriguez.mier@usc.es">pablo.rodriguez.mier@usc.es</a>>
 */
public class ASCIIMazeVisualizer {
    private JPanel mainPanel;
    private JPanel optionPanel;
    private JComboBox comboMazes;
    private JLabel loadMazeLabel;
    private JPanel algoContainerPanel;
    private JPanel textContainerPanel;
    private JTextArea mazeTextArea;
    private JComboBox comboAlgorithm;
    private JButton resetButton;
    private JButton runButton;
    private JPanel configAlgorithmPanel;
    private JSpinner refreshSpinner;
    private JPanel refreshPanel;
    private JPanel buttonPanel;
    private JLabel refreshLabel;
    private JFrame mainFrame;
    private String lastMaze;

    private enum State {STOPPED, STARTED, PAUSED}

    // Execution state
    private State state = State.STOPPED;
    private AlgorithmListener algorithmListener;

    public static void main(String[] args){

        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ex){}

        JFrame frame = new JFrame("Hipster Maze Shortest Path Visualizer");
        frame.setContentPane(new ASCIIMazeVisualizer(frame).mainPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public ASCIIMazeVisualizer(final JFrame frame) {
        this.mainFrame = frame;
        // Use double buffer for smooth updates
        mazeTextArea.setDoubleBuffered(true);
        refreshSpinner.setValue(50);
        // Listener to process run/pause button
        runButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // Fill with spaces
                switch (state){
                    case STOPPED: start(); break;
                    case STARTED: pause(); break;
                    case PAUSED: start(); break;
                }
            }
        });

        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stop();
                loadSelectedMaze();
            }
        });

        comboMazes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadSelectedMaze();
                frame.pack();
            }
        });

        loadSelectedMaze();
    }

    private void fillTextAreaWithSpaces(){

    }

    private void start(){

        if (state.equals(State.PAUSED)){
            algorithmListener.startTimer();
            runButton.setText("Pause");
            state = State.STARTED;
            return;
        }

        // Create a new maze and run the selected algorithm
        Maze2D maze;
        try {
            maze = new Maze2D(mazeTextArea.getText().split("\\r?\\n"));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainFrame, ex.getMessage() + ". Try to reset the map.", "Maze parse exception", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Iterator<? extends Node<?, Point, ?>> iterator = createAlgorithm(maze);
        algorithmListener = new AlgorithmListener(iterator, maze);
        algorithmListener.startTimer();
        runButton.setText("Pause");
        state = State.STARTED;
    }

    private void stop(){
        if (state.equals(State.STARTED) || state.equals(State.PAUSED)){
            algorithmListener.stopTimer();
        }
        runButton.setText("Run");
        state = State.STOPPED;
    }

    private void pause(){
        if (state.equals(State.STARTED)){
            algorithmListener.stopTimer();
        }
        runButton.setText("Run");
        state = State.PAUSED;
    }

    private void reset(){
        stop();
        loadSelectedMaze();
    }


    private class AlgorithmListener implements ActionListener {

        private Iterator<? extends Node<?, Point, ?>> algorithmIterator;
        private Collection<Point> explored = new HashSet<Point>();
        private Maze2D maze;
        private Timer timer;


        private AlgorithmListener(Iterator<? extends Node<?, Point, ?>> algorithmIterator, Maze2D maze) {
            this.algorithmIterator = algorithmIterator;
            this.maze = maze;
            this.timer = new Timer((Integer)refreshSpinner.getValue(), this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!algorithmIterator.hasNext()) return;
            Node<?,Point,?> currentNode = algorithmIterator.next();
            // Record all visited states to mark as visited in the string
            explored.add(currentNode.state());
            List<Point> statePath = Lists.transform(currentNode.path(), new Function<Node<?, Point, ?>, Point>() {
                @Override
                public Point apply(Node<?, Point, ?> pointNode) {
                    return pointNode.state();
                }
            });

            final String mazeStr = getMazeStringSolution(maze, explored, statePath);
            mazeTextArea.setText(mazeStr);
            if (currentNode.state().equals(maze.getGoalLoc())) stop();
        }

        public void startTimer() {
            timer.start();
        }

        public void stopTimer(){
            timer.stop();
        }
    }

    private Iterator<? extends Node<?, Point, ?>> createAlgorithm(Maze2D maze){
        final Iterator<? extends Node<?, Point, ?>> iterator;
        switch(comboAlgorithm.getSelectedIndex()){

            case 0:
                iterator = Hipster.createBreadthFirstSearch(buildProblem(maze, false)).iterator();
                break;
            case 1:
                iterator = Hipster.createBellmanFord(buildProblem(maze, false)).iterator();
                break;
            case 2:
                iterator = Hipster.createDijkstra(buildProblem(maze, false)).iterator();
                break;
            case 3:
                iterator = Hipster.createAStar(buildProblem(maze, true)).iterator();
                break;
            case 4:
                iterator = Hipster.createIDAStar(buildProblem(maze, true)).iterator();
                break;
            default:
                throw new IllegalStateException("Invalid algorithm");
        }
        return iterator;
    }

    private void loadSelectedMaze(){
        switch(comboMazes.getSelectedIndex()){
            case 0: mazeTextArea.setText(Joiner.on('\n').join(Mazes.exampleMaze1)); break;
            case 1: mazeTextArea.setText(Joiner.on('\n').join(Mazes.testMaze4)); break;
            case 2: mazeTextArea.setText(Joiner.on('\n').join(Mazes.testMaze3)); break;
            case 3: mazeTextArea.setText(Joiner.on('\n').join(Mazes.testMaze2)); break;
            case 4: mazeTextArea.setText(Joiner.on('\n').join(Mazes.testMaze5)); break;
        }
    }

    private String getMazeStringSolution(Maze2D maze, Collection<Point> explored, Collection<Point> path) {
        List<Map<Point, Character>> replacements = new ArrayList<Map<Point, Character>>();
        Map<Point, Character> replacement = new HashMap<Point, Character>();
        for (Point p : explored) {
            replacement.put(p, '.');
        }
        replacements.add(replacement);
        replacement = new HashMap<Point, Character>();
        for (Point p : path) {
            replacement.put(p, '*');
        }
        replacements.add(replacement);
        return maze.getReplacedMazeString(replacements);
    }

    private SearchProblem<Void, Point, WeightedNode<Void, Point, Double>> buildProblem(final Maze2D maze, final boolean heuristic){
        return ProblemBuilder.create()
                .initialState(maze.getInitialLoc())
                .defineProblemWithoutActions()
                .useTransitionFunction(new StateTransitionFunction<Point>() {
                    @Override
                    public Iterable<Point> successorsOf(Point state) {
                        return maze.validLocationsFrom(state);
                    }
                })
                .useCostFunction(new CostFunction<Void, Point, Double>() {
                    @Override
                    public Double evaluate(Transition<Void, Point> transition) {
                        Point source = transition.getFromState();
                        Point destination = transition.getState();
                        double distance = source.distance(destination);
                        double roundedDistance = (double) Math.round(distance*1e5)/1e5;
                        return roundedDistance;
                    }
                })
                .useHeuristicFunction(new HeuristicFunction<Point, Double>() {
                    @Override
                    public Double estimate(Point state) {
                        if (heuristic) {
                            double distance = state.distance(maze.getGoalLoc());
                            double roundedDistance = (double) Math.round(distance*1e5)/1e5;
                            return roundedDistance;
                        }
                        return 0d;
                    }
                })
                .build();
    }
}

package org.uma.jmetal.parallel.asynchronous.algorithm.impl;


import org.uma.jmetal.parallel.asynchronous.jppf.AbstractJPPFBasedNSGAII;
import org.uma.jmetal.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.util.ranking.impl.MergeNonDominatedSortRanking;
import org.uma.jmetal.experimental.componentbasedalgorithm.catalogue.replacement.Replacement;
import org.uma.jmetal.experimental.componentbasedalgorithm.catalogue.replacement.impl.RankingAndDensityEstimatorReplacement;
import org.uma.jmetal.experimental.componentbasedalgorithm.catalogue.termination.Termination;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.parallel.asynchronous.task.impl.SolutionTask;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observable.impl.DefaultObservable;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.util.*;
import java.util.stream.IntStream;


public class AsynchronousJPPFBasedNSGAII<S extends Solution<?>>
        extends AbstractJPPFBasedNSGAII<SolutionTask<S>, S> {
    private CrossoverOperator<S> crossover;
    private MutationOperator<S> mutation;
    private SelectionOperator<List<S>, S> selection;
    private Replacement<S> replacement;
    private Termination termination ;

    private List<S> population = new ArrayList<>();
    private int populationSize;
    private int evaluations = 0;
    private long initTime ;

    private Map<String, Object> attributes;
    private Observable<Map<String, Object>> observable;

    public AsynchronousJPPFBasedNSGAII(
            Problem<S> problem,
            int populationSize,
            CrossoverOperator<S> crossover,
            MutationOperator<S> mutation,
            Termination termination) {
        super(problem);
        this.crossover = crossover;
        this.mutation = mutation;
        this.populationSize = populationSize;
        this.termination = termination ;

        selection =
                new BinaryTournamentSelection<>(
                        new RankingAndCrowdingDistanceComparator<>());

        replacement =
                new RankingAndDensityEstimatorReplacement<S>(
                        new MergeNonDominatedSortRanking<S>(),
                        new CrowdingDistanceDensityEstimator<S>(),
                        RankingAndDensityEstimatorReplacement.RemovalPolicy.oneShot);

        attributes = new HashMap<>() ;
        observable = new DefaultObservable<>("Asynchronous NSGAII observable") ;
    }

    @Override
    public void initProgress() {
        attributes.put("EVALUATIONS", evaluations);
        attributes.put("POPULATION", population);
        attributes.put("COMPUTING_TIME", System.currentTimeMillis() - initTime);

        observable.setChanged();
        observable.notifyObservers(attributes);
    }

    @Override
    public void updateProgress() {
        attributes.put("EVALUATIONS", evaluations);
        attributes.put("POPULATION", population);
        attributes.put("COMPUTING_TIME", System.currentTimeMillis() - initTime);

        observable.setChanged();
        observable.notifyObservers(attributes);
    }

    @Override
    public List<SolutionTask<S>> createInitialTasks() {
        List<S> initialPopulation = new ArrayList<>();
        List<SolutionTask<S>> initialTaskList = new LinkedList<>();
        IntStream.range(0, populationSize)
                .forEach(i -> initialPopulation.add(problem.createSolution()));
        initialPopulation.forEach(
                solution -> {
                    int taskId = JMetalRandom.getInstance().nextInt(0, 1000);
                    initialTaskList.add(new SolutionTask<>(taskId, solution));
                });

        return initialTaskList;
    }

    @Override
    public void processComputedTask(SolutionTask<S> task) {
        evaluations++;
        if (population.size() < populationSize) {
            population.add(task.getContents());
        } else {
            List<S> offspringPopulation = new ArrayList<>(1);
            offspringPopulation.add(task.getContents());

            population = replacement.replace(population, offspringPopulation);
            Check.that(population.size() == populationSize, "The population size is incorrect");
        }
    }

    @Override
    public SolutionTask<S> createNewTask() {
        if (population.size() > 2) {
            List<S> parents = new ArrayList<>(2);
            parents.add(selection.execute(population));
            parents.add(selection.execute(population));

            List<S> offspring = crossover.execute(parents);

            mutation.execute(offspring.get(0));

            return new SolutionTask<>(0, (S) offspring.get(0));
        } else {
            return new SolutionTask<>(0, problem.createSolution());
        }
    }

    @Override
    public boolean stoppingConditionIsNotMet() {
        return !termination.isMet(attributes);
    }

    @Override
    public void run() {
        initTime = System.currentTimeMillis() ;
        super.run();
        jppfClient.close();
    }

    @Override
    public List<S> getResult() {
        return population;
    }

    public Observable<Map<String, Object>> getObservable() {
        return observable ;
    }
}
package org.uma.jmetal.lab.studies.util;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.moead.MOEAD;
import org.uma.jmetal.algorithm.multiobjective.moead.MOEADDE;
import org.uma.jmetal.algorithm.multiobjective.moead.MOEADDEWithArchive;
import org.uma.jmetal.algorithm.multiobjective.moead.MOEADWithArchive;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAII;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIWithArchive;
import org.uma.jmetal.algorithm.multiobjective.smpso.SMPSO;
import org.uma.jmetal.algorithm.multiobjective.smpso.SMPSOWithArchive;
import org.uma.jmetal.algorithm.multiobjective.smsemoa.SMSEMOA;
import org.uma.jmetal.algorithm.multiobjective.smsemoa.SMSEMOAWithArchive;
import org.uma.jmetal.component.evaluation.Evaluation;
import org.uma.jmetal.component.evaluation.impl.SequentialEvaluation;
import org.uma.jmetal.component.ranking.impl.MergeNonDominatedSortRanking;
import org.uma.jmetal.component.termination.Termination;
import org.uma.jmetal.component.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.ProblemUtils;
import org.uma.jmetal.util.aggregativefunction.AggregativeFunction;
import org.uma.jmetal.util.aggregativefunction.impl.PenaltyBoundaryIntersection;
import org.uma.jmetal.util.aggregativefunction.impl.Tschebyscheff;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.archive.BoundedArchive;
import org.uma.jmetal.util.archive.impl.CrowdingDistanceArchive;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;

import java.util.List;

public class AlgorithmBuilder {
  public static Algorithm<List<DoubleSolution>> createNSGAII(
      Problem<DoubleSolution> problem, int populationSize, int maxEvaluations) {
    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;

    double mutationProbability = 1.0 / problem.getNumberOfVariables();
    double mutationDistributionIndex = 20.0;

    int offspringPopulationSize = populationSize;

    Termination termination = new TerminationByEvaluations(maxEvaluations);

    Algorithm<List<DoubleSolution>> algorithm =
        new NSGAII<>(
            problem,
            populationSize,
            offspringPopulationSize,
            new SBXCrossover(crossoverProbability, crossoverDistributionIndex),
            new PolynomialMutation(mutationProbability, mutationDistributionIndex),
            termination, new MergeNonDominatedSortRanking<>());

    return algorithm;
  }

  public static Algorithm<List<DoubleSolution>> createNSGAIIWithArchive(
      Problem<DoubleSolution> problem, Archive<DoubleSolution> archive, int populationSize, int maxEvaluations) {
    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;

    double mutationProbability = 1.0 / problem.getNumberOfVariables();
    double mutationDistributionIndex = 20.0;

    int offspringPopulationSize = populationSize;

    Termination termination = new TerminationByEvaluations(maxEvaluations);

    Algorithm<List<DoubleSolution>> algorithm =
        new NSGAIIWithArchive<>(
            problem,
            populationSize,
            offspringPopulationSize,
            new SBXCrossover(crossoverProbability, crossoverDistributionIndex),
            new PolynomialMutation(mutationProbability, mutationDistributionIndex),
            termination, new MergeNonDominatedSortRanking<>(),
            archive);

    return algorithm;
  }

  public static Algorithm<List<DoubleSolution>> createSMSEMOA(
      Problem<DoubleSolution> problem, int populationSize, int maxEvaluations) {
    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;

    double mutationProbability = 1.0 / problem.getNumberOfVariables();
    double mutationDistributionIndex = 20.0;

    Termination termination = new TerminationByEvaluations(maxEvaluations);

    Algorithm<List<DoubleSolution>> algorithm =
        new SMSEMOA<>(
            problem,
            populationSize,
            new SBXCrossover(crossoverProbability, crossoverDistributionIndex),
            new PolynomialMutation(mutationProbability, mutationDistributionIndex),
            termination);

    return algorithm;
  }

  public static Algorithm<List<DoubleSolution>> createSMSEMOAWithArchive(
      Problem<DoubleSolution> problem, int populationSize, int maxEvaluations) {
    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;

    double mutationProbability = 1.0 / problem.getNumberOfVariables();
    double mutationDistributionIndex = 20.0;

    Termination termination = new TerminationByEvaluations(maxEvaluations);

    Algorithm<List<DoubleSolution>> algorithm =
        new SMSEMOAWithArchive<>(
            problem,
            populationSize,
            new SBXCrossover(crossoverProbability, crossoverDistributionIndex),
            new PolynomialMutation(mutationProbability, mutationDistributionIndex),
            termination,
            new NonDominatedSolutionListArchive<>());

    return algorithm;
  }

  public static Algorithm<List<DoubleSolution>> createSMPSO(
      Problem<DoubleSolution> problem, int populationSize, int maxEvaluations) {
    int swarmSize = populationSize;
    BoundedArchive<DoubleSolution> leadersArchive =
        new CrowdingDistanceArchive<DoubleSolution>(swarmSize);

    double mutationProbability = 1.0 / problem.getNumberOfVariables();
    double mutationDistributionIndex = 20.0;

    Evaluation<DoubleSolution> evaluation = new SequentialEvaluation<>();
    Termination termination = new TerminationByEvaluations(maxEvaluations);

    Algorithm<List<DoubleSolution>> algorithm =
        new SMPSO(
            (DoubleProblem) problem,
            swarmSize,
            leadersArchive,
            new PolynomialMutation(mutationProbability, mutationDistributionIndex),
            evaluation,
            termination);

    return algorithm;
  }

  public static Algorithm<List<DoubleSolution>> createSMPSOWithExternalArchive(
      Problem<DoubleSolution> problem, Archive<DoubleSolution> archive, int populationSize, int maxEvaluations) {
    int swarmSize = maxEvaluations;
    BoundedArchive<DoubleSolution> leadersArchive =
        new CrowdingDistanceArchive<DoubleSolution>(swarmSize);

    double mutationProbability = 1.0 / problem.getNumberOfVariables();
    double mutationDistributionIndex = 20.0;

    Evaluation<DoubleSolution> evaluation = new SequentialEvaluation<>();
    Termination termination = new TerminationByEvaluations(maxEvaluations);

    Algorithm<List<DoubleSolution>> algorithm =
        new SMPSOWithArchive(
            (DoubleProblem) problem,
            swarmSize,
            leadersArchive,
            new PolynomialMutation(mutationProbability, mutationDistributionIndex),
            evaluation,
            termination,
            archive);

    return algorithm;
  }

  public static Algorithm<List<DoubleSolution>> createMOEADDE(
      Problem<DoubleSolution> problem, int populationSize, int maxEvaluations) {
    double cr = 1.0;
    double f = 0.5;

    double neighborhoodSelectionProbability = 0.9;
    int neighborhoodSize = 20;
    int maximumNumberOfReplacedSolutions = 2;

    AggregativeFunction aggregativeFunction = new Tschebyscheff();

    Algorithm<List<DoubleSolution>> algorithm =
        new MOEADDE(
            problem,
            populationSize,
            cr,
            f,
            aggregativeFunction,
            neighborhoodSelectionProbability,
            maximumNumberOfReplacedSolutions,
            neighborhoodSize,
            "resources/weightVectorFiles/moead",
            new TerminationByEvaluations(maxEvaluations));

    return algorithm;
  }

  public static Algorithm<List<DoubleSolution>> createMOEADDEWithArchive(
      Problem<DoubleSolution> problem, Archive<DoubleSolution> archive, int populationSize, int maxEvaluations) {
    double cr = 1.0;
    double f = 0.5;

    double neighborhoodSelectionProbability = 0.9;
    int neighborhoodSize = 20;
    int maximumNumberOfReplacedSolutions = 2;

    AggregativeFunction aggregativeFunction = new Tschebyscheff();

    Algorithm<List<DoubleSolution>> algorithm =
        new MOEADDEWithArchive(
            problem,
            populationSize,
            cr,
            f,
            aggregativeFunction,
            neighborhoodSelectionProbability,
            maximumNumberOfReplacedSolutions,
            neighborhoodSize,
            "resources/weightVectorFiles/moead",
            new TerminationByEvaluations(maxEvaluations),
            archive);
    return algorithm;
  }

  public static Algorithm<List<DoubleSolution>> createMOEAD(
      Problem<DoubleSolution> problem, int populationSize, int maxEvaluations) {

    MutationOperator<DoubleSolution> mutation;
    CrossoverOperator<DoubleSolution> crossover;

    crossover = new SBXCrossover(1.0, 20.0);

    double mutationProbability = 1.0 / problem.getNumberOfVariables();
    double mutationDistributionIndex = 20.0;
    mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    double neighborhoodSelectionProbability = 0.9;
    int neighborhoodSize = 20;

    int maximumNumberOfReplacedSolutions = 2;

    AggregativeFunction aggregativeFunction = new PenaltyBoundaryIntersection();

    MOEAD<DoubleSolution> algorithm =
        new MOEAD<DoubleSolution>(
            problem,
            populationSize,
            mutation,
            crossover,
            aggregativeFunction,
            neighborhoodSelectionProbability,
            maximumNumberOfReplacedSolutions,
            neighborhoodSize,
            "resources/weightVectorFiles/moead",
            new TerminationByEvaluations(maxEvaluations));

    return algorithm;
  }

  public static Algorithm<List<DoubleSolution>> createMOEADWithArchive(
      Problem<DoubleSolution> problem, Archive<DoubleSolution> archive, int populationSize, int maxEvaluations) {

    MutationOperator<DoubleSolution> mutation;
    CrossoverOperator<DoubleSolution> crossover;

    crossover = new SBXCrossover(1.0, 20.0);

    double mutationProbability = 1.0 / problem.getNumberOfVariables();
    double mutationDistributionIndex = 20.0;
    mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    double neighborhoodSelectionProbability = 0.9;
    int neighborhoodSize = 20;

    int maximumNumberOfReplacedSolutions = 2;

    AggregativeFunction aggregativeFunction = new PenaltyBoundaryIntersection();

    MOEADWithArchive<DoubleSolution> algorithm =
        new MOEADWithArchive<>(
            problem,
            populationSize,
            mutation,
            crossover,
            aggregativeFunction,
            neighborhoodSelectionProbability,
            maximumNumberOfReplacedSolutions,
            neighborhoodSize,
            "resources/weightVectorFiles/moead",
            new TerminationByEvaluations(maxEvaluations),
            archive);

    return algorithm;
  }
}

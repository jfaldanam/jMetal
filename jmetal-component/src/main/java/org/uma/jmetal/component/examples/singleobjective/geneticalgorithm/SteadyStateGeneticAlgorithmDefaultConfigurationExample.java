package org.uma.jmetal.component.examples.singleobjective.geneticalgorithm;

import java.io.IOException;
import java.util.List;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.singleobjective.GeneticAlgorithmBuilder;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.singleobjective.Sphere;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.termination.Termination;
import org.uma.jmetal.util.termination.impl.TerminationByEvaluations;

/**
 * Class to configure and run a steady-stat genetic algorithm to solve a {@link DoubleProblem}
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public class SteadyStateGeneticAlgorithmDefaultConfigurationExample {
  public static void main(String[] args) throws JMetalException, IOException {
    Problem<DoubleSolution> problem = new Sphere(20) ;

    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    var crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / problem.getNumberOfVariables();
    double mutationDistributionIndex = 20.0;
    var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    int populationSize = 100;
    int offspringPopulationSize = 1;

    Termination termination = new TerminationByEvaluations(125000);

    EvolutionaryAlgorithm<DoubleSolution> geneticAlgorithm = new GeneticAlgorithmBuilder<>(
        "ssGA",
                    problem,
                    populationSize,
                    offspringPopulationSize,
                    crossover,
                    mutation)
        .setTermination(termination)
        .build();

    geneticAlgorithm.run();

    List<DoubleSolution> population = geneticAlgorithm.getResult();
    JMetalLogger.logger.info("Total execution time : " + geneticAlgorithm.getTotalComputingTime() + "ms");
    JMetalLogger.logger.info("Number of evaluations: " + geneticAlgorithm.getNumberOfEvaluations());
    JMetalLogger.logger.info("Best fitness: " + geneticAlgorithm.getResult().get(0).objectives()[0]);

    new SolutionListOutput(population)
            .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
            .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
            .print();

    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());
    JMetalLogger.logger.info("Objectives values have been written to file FUN.csv");
    JMetalLogger.logger.info("Variables values have been written to file VAR.csv");
  }
}

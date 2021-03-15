package org.uma.jmetal.parallel.example;

import org.uma.jmetal.experimental.componentbasedalgorithm.catalogue.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousJPPFBasedNSGAII;
import org.uma.jmetal.parallel.synchronous.SparkEvaluation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

import java.util.List;
/**
 * Class to configure and run the NSGA-II algorithm using a JPPF grid.
 *
 * @author Jose Francisco Aldana Martin <jfaldanam@gmail.com>
 */
public class AsynchronousJPPFBasedNSGAIIExample {
    public static void main(String[] args) {
        CrossoverOperator<DoubleSolution> crossover;
        MutationOperator<DoubleSolution> mutation;
        SelectionOperator<List<DoubleSolution>, DoubleSolution> selection;
        int populationSize = 100;
        int maxEvaluations = 25000;

        Problem<DoubleSolution> problem = new ZDT1();

        double crossoverProbability = 0.9;
        double crossoverDistributionIndex = 20.0;
        crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

        double mutationProbability = 1.0 / problem.getNumberOfVariables();
        double mutationDistributionIndex = 20.0;
        mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

        long initTime = System.currentTimeMillis();

        AsynchronousJPPFBasedNSGAII<DoubleSolution> asynchronousNSGAII =
                new AsynchronousJPPFBasedNSGAII<>(
                        problem, populationSize, crossover, mutation, new TerminationByEvaluations(maxEvaluations));

//    EvaluationObserver evaluationObserver = new EvaluationObserver(1000);
//    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
//           new RunTimeChartObserver<>("NSGA-II", 80, 1000, "resources/referenceFrontsCSV/ZDT1.pf");

//    asynchronousNSGAII.getObservable().register(runTimeChartObserver);
//    asynchronousNSGAII.getObservable().register(evaluationObserver);

        asynchronousNSGAII.run();

        long endTime = System.currentTimeMillis();

        List<DoubleSolution> resultList = asynchronousNSGAII.getResult();

        JMetalLogger.logger.info("Computing time: " + (endTime - initTime));
        new SolutionListOutput(resultList)
                .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
                .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
                .print();
    }
}
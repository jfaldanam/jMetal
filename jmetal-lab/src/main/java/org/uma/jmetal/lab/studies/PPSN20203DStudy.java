package org.uma.jmetal.lab.studies;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.moead.MOEADDE;
import org.uma.jmetal.algorithm.multiobjective.moead.MOEADDEWithArchive;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAII;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIWithArchive;
import org.uma.jmetal.algorithm.multiobjective.smpso.SMPSO;
import org.uma.jmetal.algorithm.multiobjective.smpso.SMPSOWithArchive;
import org.uma.jmetal.algorithm.multiobjective.smsemoa.SMSEMOA;
import org.uma.jmetal.algorithm.multiobjective.smsemoa.SMSEMOAWithArchive;
import org.uma.jmetal.component.evaluation.Evaluation;
import org.uma.jmetal.component.evaluation.impl.SequentialEvaluation;
import org.uma.jmetal.component.termination.Termination;
import org.uma.jmetal.component.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.*;
import org.uma.jmetal.lab.experiment.util.AlgorithmReturningASubSetOfSolutions;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.*;
import org.uma.jmetal.problem.multiobjective.wfg.*;
import org.uma.jmetal.qualityindicator.impl.*;
import org.uma.jmetal.qualityindicator.impl.hypervolume.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.aggregativefunction.AggregativeFunction;
import org.uma.jmetal.util.aggregativefunction.impl.Tschebyscheff;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.archive.BoundedArchive;
import org.uma.jmetal.util.archive.impl.CosineSimilarityArchive;
import org.uma.jmetal.util.archive.impl.CrowdingDistanceArchive;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.point.impl.IdealPoint;
import org.uma.jmetal.util.point.impl.NadirPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.uma.jmetal.lab.studies.util.AlgorithmBuilder.*;

/** @author Antonio J. Nebro <antonio@lcc.uma.es> */
public class PPSN20203DStudy {
  private static final int INDEPENDENT_RUNS = 25;
  private static final int MAX_EVALUATIONS = 50000;
  private static final int POPULATION_SIZE = 91;

  public static void main(String[] args) throws IOException {

    if (args.length != 1) {
      throw new JMetalException("Missing argument: experimentBaseDirectory");
    }
    String experimentBaseDirectory = args[0];

    List<ExperimentProblem<DoubleSolution>> problemList = new ArrayList<>();
    problemList.add(new ExperimentProblem<>(new DTLZ1()).setReferenceFront("DTLZ1.3D.pf"));
    problemList.add(new ExperimentProblem<>(new DTLZ2()).setReferenceFront("DTLZ2.3D.pf"));
    problemList.add(new ExperimentProblem<>(new DTLZ3()).setReferenceFront("DTLZ3.3D.pf"));
    problemList.add(new ExperimentProblem<>(new DTLZ4()).setReferenceFront("DTLZ4.3D.pf"));
    problemList.add(new ExperimentProblem<>(new DTLZ5()).setReferenceFront("DTLZ5.3D.pf"));
    problemList.add(new ExperimentProblem<>(new DTLZ6()).setReferenceFront("DTLZ6.3D.pf"));
    problemList.add(new ExperimentProblem<>(new DTLZ7()).setReferenceFront("DTLZ7.3D.pf"));
    problemList.add(new ExperimentProblem<>(new WFG1(2, 4, 3)).setReferenceFront("WFG1.3D.pf"));
    problemList.add(new ExperimentProblem<>(new WFG2(2, 4, 3)).setReferenceFront("WFG2.3D.pf"));
    problemList.add(new ExperimentProblem<>(new WFG3(2, 4, 3)).setReferenceFront("WFG3.3D.pf"));
    problemList.add(new ExperimentProblem<>(new WFG4(2, 4, 3)).setReferenceFront("WFG4.3D.pf"));
    problemList.add(new ExperimentProblem<>(new WFG5(2, 4, 3)).setReferenceFront("WFG5.3D.pf"));
    problemList.add(new ExperimentProblem<>(new WFG6(2, 4, 3)).setReferenceFront("WFG6.3D.pf"));
    problemList.add(new ExperimentProblem<>(new WFG7(2, 4, 3)).setReferenceFront("WFG7.3D.pf"));
    problemList.add(new ExperimentProblem<>(new WFG8(2, 4, 3)).setReferenceFront("WFG8.3D.pf"));
    problemList.add(new ExperimentProblem<>(new WFG9(2, 4, 3)).setReferenceFront("WFG9.3D.pf"));

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
        configureAlgorithmList(problemList);

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("PPSN20203DStudy")
            .setAlgorithmList(algorithmList)
            .setProblemList(problemList)
            .setExperimentBaseDirectory(experimentBaseDirectory)
            .setOutputParetoFrontFileName("FUN")
            .setOutputParetoSetFileName("VAR")
            .setReferenceFrontDirectory("resources/referenceFrontsCSV")
            .setIndicatorList(
                Arrays.asList(
                    new Epsilon<DoubleSolution>(),
                    new PISAHypervolume<DoubleSolution>(),
                    new InvertedGenerationalDistance<DoubleSolution>(),
                    new InvertedGenerationalDistancePlus<DoubleSolution>()))
            .setIndependentRuns(INDEPENDENT_RUNS)
            .setNumberOfCores(8)
            .build();

    new ExecuteAlgorithms<>(experiment).run();

    new ComputeQualityIndicators<>(experiment).run();
    new GenerateLatexTablesWithStatistics(experiment).run();
    new GenerateWilcoxonTestTablesWithR<>(experiment).run();
    new GenerateFriedmanTestTables<>(experiment).run();
    new GenerateBoxplotsWithR<>(experiment).setRows(5).setColumns(4).run();
  }

  public static Algorithm<List<DoubleSolution>> createAlgorithmToSelectPartOfTheResultSolutionList(
      Algorithm<List<DoubleSolution>> algorithm, int numberOfReturnedSolutions) {

    return new AlgorithmReturningASubSetOfSolutions<>(algorithm, numberOfReturnedSolutions);
  }

  /**
   * The algorithm list is composed of pairs {@link Algorithm} + {@link Problem} which form part of
   * a {@link ExperimentAlgorithm}, which is a decorator for class {@link Algorithm}. The {@link
   * ExperimentAlgorithm} has an optional tag component, that can be set as it is shown in this
   * example, where four variants of a same algorithm are defined.
   */
  static List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> configureAlgorithmList(
      List<ExperimentProblem<DoubleSolution>> problemList) {
    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();

    for (int run = 0; run < INDEPENDENT_RUNS; run++) {
      for (int i = 0; i < problemList.size(); i++) {
        algorithms.add(
            new ExperimentAlgorithm<>(
                createNSGAII(problemList.get(i).getProblem(), POPULATION_SIZE, MAX_EVALUATIONS),
                "NSGAII",
                problemList.get(i),
                run));
        algorithms.add(
            new ExperimentAlgorithm<>(
                createAlgorithmToSelectPartOfTheResultSolutionList(
                    createNSGAIIWithArchive(
                        problemList.get(i).getProblem(),
                        new NonDominatedSolutionListArchive<>(),
                        POPULATION_SIZE,
                        MAX_EVALUATIONS),
                    POPULATION_SIZE),
                "NSGAIIUA",
                problemList.get(i),
                run));
        algorithms.add(
            new ExperimentAlgorithm<>(
                createNSGAIIWithArchive(
                    problemList.get(i).getProblem(),
                    new CosineSimilarityArchive<>(POPULATION_SIZE, new NadirPoint(problemList.get(i).getProblem().getNumberOfObjectives()), true),
                    POPULATION_SIZE,
                    MAX_EVALUATIONS),
                "NSGAIICN",
                problemList.get(i),
                run));
        algorithms.add(
            new ExperimentAlgorithm<>(
                createNSGAIIWithArchive(
                    problemList.get(i).getProblem(),
                    new CosineSimilarityArchive<>(POPULATION_SIZE, new IdealPoint(problemList.get(i).getProblem().getNumberOfObjectives()), true),
                    POPULATION_SIZE,
                    MAX_EVALUATIONS),
                "NSGAIICI",
                problemList.get(i),
                run));
        algorithms.add(
            new ExperimentAlgorithm<>(
                createMOEAD(problemList.get(i).getProblem(), POPULATION_SIZE, MAX_EVALUATIONS),
                "MOEAD",
                problemList.get(i),
                run));
        algorithms.add(
            new ExperimentAlgorithm<>(
                createAlgorithmToSelectPartOfTheResultSolutionList(
                    createMOEADDEWithArchive(
                        problemList.get(i).getProblem(),
                        new NonDominatedSolutionListArchive<>(),
                        POPULATION_SIZE,
                        MAX_EVALUATIONS),
                    POPULATION_SIZE),
                "MOEADUA",
                problemList.get(i),
                run));

        /*
        algorithms.add(
                new ExperimentAlgorithm<>(
                        createSMSEMOA(problemList.get(i).getProblem()), "SMSEMOA", problemList.get(i), run));
        algorithms.add(
                new ExperimentAlgorithm<>(
                        createSMSEMOAWithArchive(problemList.get(i).getProblem()),
                        "SMSEMOAA",
                        problemList.get(i),
                        run));

         */
      }
    }
    return algorithms;
  }
}

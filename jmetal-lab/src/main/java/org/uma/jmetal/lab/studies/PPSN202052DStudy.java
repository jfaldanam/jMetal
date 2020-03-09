package org.uma.jmetal.lab.studies;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.*;
import org.uma.jmetal.lab.experiment.util.AlgorithmReturningASubSetOfSolutions;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dtlz.*;
import org.uma.jmetal.problem.multiobjective.maf.*;
import org.uma.jmetal.problem.multiobjective.wfg.*;
import org.uma.jmetal.problem.multiobjective.zdt.*;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.hypervolume.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalException;
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
public class PPSN202052DStudy {
  private static final int INDEPENDENT_RUNS = 5;
  private static final int MAX_EVALUATIONS = 25000;
  private static final int POPULATION_SIZE = 100 ;

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new JMetalException("Missing argument: experimentBaseDirectory");
    }
    String experimentBaseDirectory = args[0];

    List<ExperimentProblem<DoubleSolution>> problemList = new ArrayList<>();
    problemList.add(new ExperimentProblem<>(new ZDT1()));
    problemList.add(new ExperimentProblem<>(new ZDT2()));
    problemList.add(new ExperimentProblem<>(new ZDT3()));
    problemList.add(new ExperimentProblem<>(new ZDT4()));
    problemList.add(new ExperimentProblem<>(new ZDT6()));
    problemList.add(new ExperimentProblem<>(new DTLZ1_2D()).setReferenceFront("DTLZ1.2D.pf"));
    problemList.add(new ExperimentProblem<>(new DTLZ2_2D()).setReferenceFront("DTLZ2.2D.pf"));
    problemList.add(new ExperimentProblem<>(new DTLZ3_2D()).setReferenceFront("DTLZ3.2D.pf"));
    problemList.add(new ExperimentProblem<>(new DTLZ4_2D()).setReferenceFront("DTLZ4.2D.pf"));
    problemList.add(new ExperimentProblem<>(new DTLZ5_2D()).setReferenceFront("DTLZ5.2D.pf"));
    problemList.add(new ExperimentProblem<>(new DTLZ6_2D()).setReferenceFront("DTLZ6.2D.pf"));
    problemList.add(new ExperimentProblem<>(new DTLZ7_2D()).setReferenceFront("DTLZ7.2D.pf"));
    problemList.add(new ExperimentProblem<>(new WFG1()).setReferenceFront("WFG1.2D.pf"));
    problemList.add(new ExperimentProblem<>(new WFG2()).setReferenceFront("WFG2.2D.pf"));
    problemList.add(new ExperimentProblem<>(new WFG3()).setReferenceFront("WFG3.2D.pf"));
    problemList.add(new ExperimentProblem<>(new WFG4()).setReferenceFront("WFG4.2D.pf"));
    problemList.add(new ExperimentProblem<>(new WFG5()).setReferenceFront("WFG5.2D.pf"));
    problemList.add(new ExperimentProblem<>(new WFG6()).setReferenceFront("WFG6.2D.pf"));
    problemList.add(new ExperimentProblem<>(new WFG7()).setReferenceFront("WFG7.2D.pf"));
    problemList.add(new ExperimentProblem<>(new WFG8()).setReferenceFront("WFG8.2D.pf"));
    problemList.add(new ExperimentProblem<>(new WFG9()).setReferenceFront("WFG9.2D.pf"));

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
        configureAlgorithmList(problemList);

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("PPSN2020S2Dtudy")
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

    //new ExecuteAlgorithms<>(experiment).run();
    new ComputeQualityIndicators<>(experiment).run();
    new GenerateLatexTablesWithStatistics(experiment).run();
    new GenerateWilcoxonTestTablesWithR<>(experiment).run();
    new GenerateFriedmanTestTables<>(experiment).run();
    new GenerateBoxplotsWithR<>(experiment).setRows(4).setColumns(4).run();
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
                    createNSGAIIWithArchive(
                        problemList.get(i).getProblem(),
                        new CrowdingDistanceArchive<>(POPULATION_SIZE),
                            POPULATION_SIZE,
                        MAX_EVALUATIONS),
                "NSGAIICD",
                problemList.get(i),
                run));

        algorithms.add(
            new ExperimentAlgorithm<>(
                    createNSGAIIWithArchive(
                        problemList.get(i).getProblem(),
                        new CosineSimilarityArchive<>(
                                POPULATION_SIZE,
                            new NadirPoint(problemList.get(i).getProblem().getNumberOfObjectives()),
                            true),
                            POPULATION_SIZE,
                        MAX_EVALUATIONS),
                "NSGAIICN",
                problemList.get(i),
                run));

        algorithms.add(
            new ExperimentAlgorithm<>(
                    createNSGAIIWithArchive(
                        problemList.get(i).getProblem(),
                        new CosineSimilarityArchive<>(
                                POPULATION_SIZE,
                            new IdealPoint(problemList.get(i).getProblem().getNumberOfObjectives()),
                            true),
                            POPULATION_SIZE,
                        MAX_EVALUATIONS),
                "NSGAIICI",
                problemList.get(i),
                run));

        algorithms.add(
            new ExperimentAlgorithm<>(
                createSMPSO(problemList.get(i).getProblem(), POPULATION_SIZE, MAX_EVALUATIONS),
                "SMPSO",
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

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
import org.uma.jmetal.component.ranking.impl.MergeNonDominatedSortRanking;
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
import org.uma.jmetal.problem.multiobjective.maf.*;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
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
import java.util.Locale;

import static org.uma.jmetal.lab.studies.util.AlgorithmBuilder.*;

/** @author Antonio J. Nebro <antonio@lcc.uma.es> */
public class PPSN20205DStudy {
  private static final int INDEPENDENT_RUNS = 5;
  private static final int MAX_EVALUATIONS = 100000;

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new JMetalException("Missing argument: experimentBaseDirectory");
    }
    String experimentBaseDirectory = args[0];

    List<ExperimentProblem<DoubleSolution>> problemList = new ArrayList<>();
    problemList.add(new ExperimentProblem<>(new MaF01(12, 5)).setReferenceFront("MaF01.5D.pf"));
    problemList.add(new ExperimentProblem<>(new MaF02(12, 5)).setReferenceFront("MaF02.5D.pf"));
    problemList.add(new ExperimentProblem<>(new MaF03(12, 5)).setReferenceFront("MaF03.5D.pf"));
    problemList.add(new ExperimentProblem<>(new MaF04(12, 5)).setReferenceFront("MaF04.5D.pf"));
    problemList.add(new ExperimentProblem<>(new MaF05(12, 5)).setReferenceFront("MaF05.5D.pf"));
    problemList.add(new ExperimentProblem<>(new MaF06(12, 5)).setReferenceFront("MaF06.5D.pf"));
    problemList.add(new ExperimentProblem<>(new MaF07(12, 5)).setReferenceFront("MaF07.5D.pf"));
    problemList.add(new ExperimentProblem<>(new MaF08(2, 5)).setReferenceFront("MaF08.5D.pf"));
    problemList.add(new ExperimentProblem<>(new MaF09(2, 5)).setReferenceFront("MaF09.5D.pf"));
    problemList.add(new ExperimentProblem<>(new MaF10(12, 5)).setReferenceFront("MaF10.5D.pf"));
    problemList.add(new ExperimentProblem<>(new MaF11(12, 5)).setReferenceFront("MaF11.5D.pf"));
    problemList.add(new ExperimentProblem<>(new MaF12(12, 5)).setReferenceFront("MaF12.5D.pf"));
    problemList.add(new ExperimentProblem<>(new MaF13(5, 5)).setReferenceFront("MaF13.5D.pf"));
    problemList.add(new ExperimentProblem<>(new MaF14(60, 5)).setReferenceFront("MaF14.5D.pf"));
    problemList.add(new ExperimentProblem<>(new MaF15(60, 5)).setReferenceFront("MaF15.5D.pf"));

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
        configureAlgorithmList(problemList);

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("PPSN2020S5Dtudy")
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
                        createNSGAII(problemList.get(i).getProblem(), 240, MAX_EVALUATIONS),
                        "NSGAII",
                        problemList.get(i),
                        run));
        algorithms.add(
                new ExperimentAlgorithm<>(
                        createAlgorithmToSelectPartOfTheResultSolutionList(
                                createNSGAIIWithArchive(
                                        problemList.get(i).getProblem(),
                                        new NonDominatedSolutionListArchive<>(),
                                        240,
                                        MAX_EVALUATIONS),
                                240),
                        "NSGAIIA",
                        problemList.get(i),
                        run));
        algorithms.add(
            new ExperimentAlgorithm<>(
                    createNSGAIIWithArchive(
                        problemList.get(i).getProblem(),
                        new CosineSimilarityArchive<>(
                            240,
                            new NadirPoint(problemList.get(i).getProblem().getNumberOfObjectives()),
                            true),
                        240,
                        MAX_EVALUATIONS),
                "NSGAIICN",
                problemList.get(i),
                run));

        algorithms.add(
            new ExperimentAlgorithm<>(
                    createNSGAIIWithArchive(
                        problemList.get(i).getProblem(),
                        new CosineSimilarityArchive<>(
                            240,
                            new IdealPoint(problemList.get(i).getProblem().getNumberOfObjectives()),
                            true),
                        240,
                        MAX_EVALUATIONS),
                "NSGAIICI",
                problemList.get(i),
                run));
        /*
        algorithms.add(
            new ExperimentAlgorithm<>(
                createAlgorithmToSelectPartOfTheResultSolutionList(
                    createMOEADDE(problemList.get(i).getProblem(), 495, MAX_EVALUATIONS), 240),
                "MOEAD",
                problemList.get(i),
                run));
        algorithms.add(
            new ExperimentAlgorithm<>(
                createAlgorithmToSelectPartOfTheResultSolutionList(
                    createMOEADDEWithArchive(
                        problemList.get(i).getProblem(),
                        new NonDominatedSolutionListArchive<>(),
                        495,
                        MAX_EVALUATIONS),
                    240),
                "MOEADA",
                problemList.get(i),
                run));
        algorithms.add(
            new ExperimentAlgorithm<>(
                createSMPSO(problemList.get(i).getProblem(), 240, MAX_EVALUATIONS),
                "SMPSO",
                problemList.get(i),
                run));
        algorithms.add(
            new ExperimentAlgorithm<>(
                createAlgorithmToSelectPartOfTheResultSolutionList(
                    createSMPSOWithExternalArchive(
                        problemList.get(i).getProblem(),
                        new NonDominatedSolutionListArchive<>(),
                        240,
                        MAX_EVALUATIONS),
                    240),
                "SMPSOA",
                problemList.get(i),
                run));
*/
        algorithms.add(
            new ExperimentAlgorithm<>(
                createAlgorithmToSelectPartOfTheResultSolutionList(
                    createSMPSOWithExternalArchive(
                        problemList.get(i).getProblem(),
                        new NonDominatedSolutionListArchive<>(),
                        240,
                        MAX_EVALUATIONS),
                    240),
                "CVEA3",
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

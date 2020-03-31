package org.uma.jmetal.component.replacement.impl;

import org.uma.jmetal.component.densityestimator.DensityEstimator;
import org.uma.jmetal.component.ranking.Ranking;
import org.uma.jmetal.component.ranking.impl.MergeNonDominatedSortRanking;
import org.uma.jmetal.component.ranking.impl.SteadyStateMergeNonDominatedSortRanking;
import org.uma.jmetal.component.replacement.Replacement;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.JMetalException;

import java.util.ArrayList;
import java.util.List;

public class SteadyStateRankingAndDensityEstimatorReplacement<S extends Solution<?>>
    implements Replacement<S> {
  private SteadyStateMergeNonDominatedSortRanking<S> ranking;
  private DensityEstimator<S> densityEstimator;
  private RemovalPolicy removalPolicy ;

  int firstTime = 0 ;

  public SteadyStateRankingAndDensityEstimatorReplacement(
      SteadyStateMergeNonDominatedSortRanking<S> ranking, DensityEstimator<S> densityEstimator, RemovalPolicy removalPolicy) {
    this.densityEstimator = densityEstimator;
    this.removalPolicy = removalPolicy ;

    this.ranking = ranking ;
  }

  public List<S> replace(List<S> solutionList, List<S> offspringList) {
    if (firstTime == 0) {
      firstTime ++ ;
      List<S> jointPopulation = new ArrayList<>();
      jointPopulation.addAll(solutionList);
      jointPopulation.addAll(offspringList);

      List<S> resultList;
      ranking.computeRanking(jointPopulation);

      if (removalPolicy == RemovalPolicy.oneShot) {
        resultList = oneShotTruncation(0, solutionList.size());
      } else {
        resultList = sequentialTruncation(0, solutionList.size());
      }
      ranking.resizePopulation(resultList);    // Reorganizar estructuras internas del MNDS
      return resultList;
    } else {
      ranking.addSolution(offspringList.get(0));
      ranking.steadyStateRanking() ;

      List<S> resultList;
      if (removalPolicy == RemovalPolicy.oneShot) {
        resultList = oneShotTruncation(0, solutionList.size());
      } else {
        resultList = sequentialTruncation(0, solutionList.size());
      }

      List<S> jointPopulation = new ArrayList<>();
      jointPopulation.addAll(solutionList);
      jointPopulation.addAll(offspringList);

      boolean found = false ;
      for (S solution: jointPopulation) {
        if (!resultList.contains(solution)) {
          ranking.removeSolution(solution);
          found = true ;
          break ;
        }
      }

      if (found == false) {
        throw new JMetalException("Solution to remove not found") ;
      }

      return resultList ;
    }
  }

  private List<S> oneShotTruncation(int sizeOfTheResultingSolutionList) {
    int currentRank = 0 ;

    List<S> resultList = new ArrayList<>();
    while (resultList.size() < sizeOfTheResultingSolutionList) {
      if (ranking.getSubFront(currentRank).size() < (sizeOfTheResultingSolutionList - resultList.size())) {
        resultList.addAll(ranking.getSubFront(currentRank)) ;
        currentRank ++ ;
      } else {
        densityEstimator.computeDensityEstimator(ranking.getSubFront(currentRank));
        densityEstimator.sort(ranking.getSubFront(currentRank)) ;
        int i = 0 ;
        while (resultList.size() < sizeOfTheResultingSolutionList) {
          resultList.add(ranking.getSubFront(currentRank).get(i)) ;
          i++ ;
        }
      }
    }

    return resultList ;
  }


  private List<S> oneShotTruncation(int rankingId, int sizeOfTheResultingSolutionList) {
    List<S> currentRankSolutions = ranking.getSubFront(rankingId);
    densityEstimator.computeDensityEstimator(currentRankSolutions);

    List<S> resultList = new ArrayList<>();

    if (currentRankSolutions.size() < sizeOfTheResultingSolutionList) {
      resultList.addAll(ranking.getSubFront(rankingId));
      resultList.addAll(
          oneShotTruncation(
              rankingId + 1,
              sizeOfTheResultingSolutionList - currentRankSolutions.size()));
    } else {
      densityEstimator.sort(currentRankSolutions);
      int i = 0;
      while (resultList.size() < sizeOfTheResultingSolutionList) {
        resultList.add(currentRankSolutions.get(i));
        i++;
      }
    }

    return resultList;
  }


  private List<S> sequentialTruncation(int rankingId, int sizeOfTheResultingSolutionList) {
    List<S> currentRankSolutions = ranking.getSubFront(rankingId);
    densityEstimator.computeDensityEstimator(currentRankSolutions);

    List<S> resultList = new ArrayList<>();

    if (currentRankSolutions.size() < sizeOfTheResultingSolutionList) {
      resultList.addAll(ranking.getSubFront(rankingId));
      resultList.addAll(
          sequentialTruncation(
              rankingId + 1,
              sizeOfTheResultingSolutionList - currentRankSolutions.size()));
    } else {
      for (S solution : currentRankSolutions)
        resultList.add(solution) ;
      while (resultList.size() > sizeOfTheResultingSolutionList) {
        densityEstimator.sort(resultList);

        resultList.remove(resultList.size()-1) ;
        densityEstimator.computeDensityEstimator(resultList);
      }
    }

    return resultList;
  }
}

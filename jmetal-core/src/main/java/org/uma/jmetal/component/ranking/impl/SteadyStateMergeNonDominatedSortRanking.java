package org.uma.jmetal.component.ranking.impl;

import org.uma.jmetal.component.ranking.Ranking;
import org.uma.jmetal.component.ranking.impl.util.MNDSBitsetManager;
import org.uma.jmetal.component.ranking.impl.util.MNDSBitsetManager_SS;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.util.attribute.util.attributecomparator.AttributeComparator;
import org.uma.jmetal.solution.util.attribute.util.attributecomparator.impl.IntegerValueAttributeComparator;
import org.uma.jmetal.util.JMetalException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * This class implements a solution list ranking based on dominance ranking. Given a collection of
 * solutions, they are ranked according to scheme similar to the one proposed in NSGA-II. As an
 * output, a set of subsets are obtained. The subsets are numbered starting from 0 (in NSGA-II, the
 * numbering starts from 1); thus, subset 0 contains the non-dominated solutions, subset 1 contains
 * the non-dominated population after removing those belonging to subset 0, and so on.
 *
 * @author Javier Moreno <javier.morenom@edu.uah.es>
 */
@SuppressWarnings("serial")
public class SteadyStateMergeNonDominatedSortRanking<S extends Solution<?>> implements Ranking<S> {
  private String attributeId = getClass().getName();
  private Comparator<S> solutionComparator;

  private static final int SOLUTION_INDEX = 0xFFFFFA;
  private static final int INSERTIONSORT = 7;
  //	private static int SolIDix; //field to store the identifier of the jMetal solution
  //	private static int ORDINALix; //field to store the solution ordinal after ordering by the first objective

  private static int SolIDix; //field to store the identifier of the jMetal solution
  private static int ORDINALix; //field to store the solution index after ordering by the first objective
  private static int RANKINGix; // filed to store the solution ranking
  private static int NumberOfix; // total number of fields in the population matrix

  private int m; // Number of Objectives
  private long _comparisonCounter = 0;
  private long _nonDomEarlyDetection = 0;
  private int n; // Population Size
  //TODO SOLO PARA PRUEBAS, QUITAR! private boolean debug;
  private int[] ranking;
  private double[][] population, initialPopulation;
  private double[][] work; // Work array
  private MNDSBitsetManager_SS bsManager;
  private List<ArrayList<S>> rankedSubPopulations;

  public SteadyStateMergeNonDominatedSortRanking() {
    this.solutionComparator =
            new IntegerValueAttributeComparator<>(
                    attributeId, AttributeComparator.Ordering.ASCENDING);
  }

  @Override
  public Ranking<S> computeRanking(List<S> solutionSet) {
    n = solutionSet.size();
    m = solutionSet.get(0).getNumberOfObjectives();
    bsManager = new MNDSBitsetManager_SS(n, m);
    SolIDix = m;
    ORDINALix = SolIDix + 1;
    RANKINGix = ORDINALix + 1;
    NumberOfix = RANKINGix + 1;
    _nonDomEarlyDetection = 0;

    work = new double[n][];

    initialPopulation = new double[n][];

    for (int i = 0; i < n; i++) {
      initialPopulation[i] = new double[ORDINALix + 1];
      System.arraycopy(solutionSet.get(i).getObjectives(), 0, initialPopulation[i], 0, m);
      initialPopulation[i][SolIDix] = i; // asignamos id a la solucion
    }
    int ranking[] = sort(initialPopulation);
    rankedSubPopulations = new ArrayList<ArrayList<S>>();
    for (int i = 0; i < n; i++) {
      for (int r = rankedSubPopulations.size(); r <= ranking[i]; r++) {
        rankedSubPopulations.add(new ArrayList<S>());
      }
      solutionSet.get(i).setAttribute(attributeId, ranking[i]);
      solutionSet.get(i).setAttribute(SOLUTION_INDEX, initialPopulation[i][ORDINALix]);
      rankedSubPopulations.get(ranking[i]).add(solutionSet.get(i));
    }
    return this;
  }
  final public long getNumberOfEarlyDetections() {
    return _nonDomEarlyDetection;
  }

  final public long getComparisonCounter() {
    return _comparisonCounter;
  }

  final private int compare_lex(double[] s1, double[] s2, int fromObj, int toObj) {
    for (; fromObj < toObj; fromObj++) {
      _comparisonCounter++;
      if (s1[fromObj] < s2[fromObj])
        return -1;
      _comparisonCounter++;
      if (s1[fromObj] > s2[fromObj])
        return 1;
    }
    return 0;
  }

  private boolean merge_sort(double src[][], double dest[][], int low, int high, int obj, int toObj) {
    int i, j, s;
    double temp[] = null;
    int destLow = low;
    int length = high - low;

    if (length < INSERTIONSORT) {
      for (i = low; i < high; i++) {
        for (j = i; j > low && compare_lex(dest[j - 1], dest[j], obj, toObj) > 0; j--) {
          temp = dest[j];
          dest[j] = dest[j - 1];
          dest[j - 1] = temp;
        }
      }
      return temp == null; //if temp==null, src is already sorted
    }
    int mid = (low + high) >>> 1;
    boolean isSorted = merge_sort(dest, src, low, mid, obj, toObj);
    isSorted &= merge_sort(dest, src, mid, high, obj, toObj);

    // If list is already sorted, just copy from src to dest.
    _comparisonCounter++;
    if (src[mid - 1][obj] <= src[mid][obj]) {
      System.arraycopy(src, low, dest, destLow, length);
      return isSorted;
    }

    for (s = low, i = low, j = mid; s < high; s++) {
      if (j >= high) {
        dest[s] = src[i++];
      } else if (i < mid && compare_lex(src[i], src[j], obj, toObj) <= 0) {
        dest[s] = src[i++];
      } else {
        dest[s] = src[j++];
      }
    }
    return false;
  }

  private void sortFirstObjective() {
    System.arraycopy(population, 0, work, 0, n);
    merge_sort(population, work, 0, n, 0, m);
    System.arraycopy(work, 0, population, 0, n);
    for (int p = 1; p < n; p++) {
      work[p][ORDINALix] = p;
      population[p] = work[p];
    }
    //TODO SOLO PARA PRUEBAS, QUITAR!  printer.updateObjective(0);
  }

  private boolean sortSecondObjective() {
    int p, solutionId, rank;
    boolean dominance = false;
    merge_sort(population, work, 0, n, 1, 2);
    System.arraycopy(work, 0, population, 0, n);
    for (p = 0; p < n; p++) {
      solutionId = ((int) population[p][ORDINALix]);
      dominance |= bsManager.initializeSolutionBitset(solutionId);
      bsManager.updateIncrementalBitset(solutionId);
      if (2 == m) {
        rank = bsManager.computeSolutionRanking(solutionId);
        population[p][RANKINGix] = rank;
        ranking[(int) population[p][SolIDix]] = rank;
      }
    }
    if (!dominance)
      _nonDomEarlyDetection++;
    //TODO SOLO PARA PRUEBAS, QUITAR!  printer.updateObjective(1);
    return dominance;
  }

  private void sortRestOfObjectives() {
    int p, rank, solutionId, lastObjective = m - 1;
    boolean dominance;
    System.arraycopy(population, 0, work, 0, n);
    for (int obj = 2; obj < m; obj++) {
      if (merge_sort(population, work, 0, n, obj, obj + 1)) {//Population has the same order as in previous objective
        if (obj == lastObjective) {
          for (p = 0; p < n; p++) {
            rank = bsManager.computeSolutionRanking((int) population[p][ORDINALix]);
            population[p][RANKINGix] = rank;
            ranking[(int) population[p][SolIDix]] = rank;
          }
        }
        //TODO SOLO PARA PRUEBAS, QUITAR!  printer.updateObjective(obj);
        continue;
      }
      System.arraycopy(work, 0, population, 0, n);
      bsManager.clearIncrementalBitset();
      dominance = false;
      for (p = 0; p < n; p++) {
        solutionId = ((int) population[p][ORDINALix]);
        if (obj < lastObjective) {
          dominance |= bsManager.updateSolutionDominance(solutionId);
        } else {
          rank = bsManager.computeSolutionRanking(solutionId);
          population[p][RANKINGix] = rank;
          ranking[(int) population[p][SolIDix]] = rank;
        }
        bsManager.updateIncrementalBitset(solutionId);
      }
      //TODO SOLO PARA PRUEBAS, QUITAR!  printer.updateObjective(obj);
      if (!dominance) {
        _nonDomEarlyDetection++;
        break;
      }
    }
    //TODO SOLO PARA PRUEBAS, QUITAR!  if (debug)  System.out.print(printer.print("\nMNDS Steady State - Primera ordenacion--"));
  }

  //TODO: PASAR INFORMACION DE DUPLICADOS AL BS MANAGER!!!!
  // main
  final public int[] sort(double[][] populationData) {

    //INITIALIZATION
    _comparisonCounter = 0;
    population = populationData;
    //SORTING
    sortFirstObjective();
    if (sortSecondObjective()) {
      sortRestOfObjectives();
    }

    //TODO: UPDATING DUPLICATED SOLUTIONS: ranking y bitsets!!!

    //Ordenamos la poblacion por el indice original de cada solucion
    System.arraycopy(population, 0, initialPopulation, 0, n);
    merge_sort(population, initialPopulation, 0, n, SolIDix, SolIDix + 1);
    return ranking;
  }

  public int[] steadyStateSort(int solutionIndex, double[] newSolution) {
    //reemplazamos con la nueva solucion
    System.arraycopy(newSolution, 0, initialPopulation[solutionIndex], 0, m);
    System.arraycopy(initialPopulation, 0, population, 0, n);
    bsManager.setNewSolution(initialPopulation[solutionIndex][ORDINALix]);
    //SORTING
    reorderPopulation(initialPopulation[solutionIndex][ORDINALix]);
    return ranking;
  }

  private void reorderPopulation(double ordNewSol) {
    int dominanceModifications[] = new int[n];
    System.arraycopy(population, 0, work, 0, n);
    for (int p, obj = 0; obj < m; obj++) {
      merge_sort(population, work, 0, n, obj, obj + 1);
      System.arraycopy(work, 0, population, 0, n);
      for (p = 0; p < n && work[p][ORDINALix] != ordNewSol; p++) {
        dominanceModifications[(int) work[p][ORDINALix]]--;
      }
      for (p++; p < n; p++) {
        dominanceModifications[(int) work[p][ORDINALix]]++;
      }
      //TODO SOLO PARA PRUEBAS, QUITAR!  printer.updateObjective(obj);
    }
    bsManager.updatePopulation(dominanceModifications);
    for (int p = 0; p < n; p++) {
      int rank = bsManager.computeSteadyStateRank((int) population[p][ORDINALix]);
      ranking[(int) population[p][SolIDix]] = rank;
      population[p][RANKINGix] = rank;
    }
    //TODO SOLO PARA PRUEBAS, QUITAR! if (debug) System.out.print(printer.print(String.format("\nMNDS Steady State +%d", iteration)));
  }

  //TODO SOLO PARA PRUEBAS, QUITAR! public void setDebug(boolean dbg) { debug = dbg;}

  //////////////////////////////////////////////
  //////////////////////////////////////////////
  //////////////////////////////////////////////
  public int getNumberOfPopulationFields() {
    return NumberOfix;
  }

  final public void freeMem() {
    population = null;
    work = null;
    ranking = null;
    bsManager.freeMem();
    rankedSubPopulations = null;
  }

  public Ranking<S> replaceSolutionAndGetRanking(int solutionRank, int solutionIndex, S newSolution) {
    ArrayList<S> front = rankedSubPopulations.get(solutionRank);
    S solution = front.remove(solutionIndex);
    int internalSolutionIndex = (int) solution.getAttribute(SOLUTION_INDEX);
    newSolution.setAttribute(SOLUTION_INDEX, internalSolutionIndex);
    newSolution.setAttribute(attributeId, -1);
    front.add(newSolution); //el rank correcto se le asigna despues de ordenar
    // ordenamos
    steadyStateSort(internalSolutionIndex, newSolution.getObjectives());
    // actualizamos ranking
    for (int n = rankedSubPopulations.size() - 1; n >= 0; n--) {
      front = rankedSubPopulations.get(n);
      for (int p = front.size() - 1; p >= 0; p--) {
        solution = front.get(p);
        internalSolutionIndex = (int) solution.getAttribute(SOLUTION_INDEX);
        if (initialPopulation[internalSolutionIndex][ORDINALix] != internalSolutionIndex)
          throw new JMetalException("error en indices");

        if (ranking[internalSolutionIndex] != (int) solution.getAttribute(attributeId)) {
          front.remove(p);
          solution.setAttribute(attributeId, ranking[internalSolutionIndex]);
          while (rankedSubPopulations.size() < ranking[internalSolutionIndex])
            rankedSubPopulations.add(new ArrayList<S>());
          rankedSubPopulations.get(ranking[internalSolutionIndex]).add(solution);
        }
      }
    }
    return this;
  }

  @Override
  public List<S> getSubFront(int rank) {
    if (rank >= rankedSubPopulations.size()) {
      throw new JMetalException(
              "Invalid rank: " + rank + ". Max rank = " + (rankedSubPopulations.size() - 1));
    }
    return rankedSubPopulations.get(rank);
  }
  @Override
  public int getNumberOfSubFronts() {
    return rankedSubPopulations.size();
  }

  @Override
  public Comparator<S> getSolutionComparator() {
    return solutionComparator;
  }

  @Override
  public String getAttributeId() {
    return attributeId;
  }
}

package org.uma.jmetal.component.ranking.impl;

import org.uma.jmetal.component.ranking.Ranking;
import org.uma.jmetal.component.ranking.impl.util.MNDSBitsetManager_SS;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.util.attribute.util.attributecomparator.AttributeComparator;
import org.uma.jmetal.solution.util.attribute.util.attributecomparator.impl.IntegerValueAttributeComparator;
import org.uma.jmetal.util.JMetalException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

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
  private Vector<S> solutionsList;
  private int updatedSolutionIndex;
  private List<ArrayList<S>> rankedSubPopulations;
  //TODO SOLO PARA PRUEBAS, QUITAR!  private PopulationPrinter printer;

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

  public SteadyStateMergeNonDominatedSortRanking(int populationSize, int nObjectives) {
    this.solutionComparator =
            new IntegerValueAttributeComparator<>(
                    attributeId, AttributeComparator.Ordering.ASCENDING);

    n = populationSize;
    m = nObjectives;
    SolIDix = m;
    ORDINALix = SolIDix + 1;
    RANKINGix = ORDINALix + 1;
    NumberOfix = RANKINGix + 1;
    _nonDomEarlyDetection = 0;
    population = new double[n + 1][];
    work = new double[n + 1][];
    initialPopulation = new double[n + 1][];
    initialPopulation[n] = new double[NumberOfix];
    updatedSolutionIndex = n;
    ranking = new int[n + 1];
  }

  private void initializeObjects(double[][] populationData) {
    population = new double[n + 1][];
    System.arraycopy(populationData, 0, population, 0, n + 1);
    System.arraycopy(populationData, 0, initialPopulation, 0, n + 1);
    updatedSolutionIndex = n;
    bsManager = new MNDSBitsetManager_SS(n + 1, m);
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
      if (!dominance) {
        _nonDomEarlyDetection++;
        break;
      }
    }
  }

  //TODO: PASAR INFORMACION DE DUPLICADOS AL BS MANAGER!!!!
  // main
  final public int[] sort(double[][] populationData) {

    //INITIALIZATION
    _comparisonCounter = 0;
    initializeObjects(populationData);
    //SORTING
    sortFirstObjective();
    if (sortSecondObjective()) {
      sortRestOfObjectives();
    }
    //Ordenamos la poblacion por el indice original de cada solucion
    System.arraycopy(population, 0, initialPopulation, 0, n + 1);
    merge_sort(population, initialPopulation, 0, n, SolIDix, SolIDix + 1);
    n++; //a partir de ahora la poblacion tiene un elemento mas
    return ranking;
  }

  private void initializeSolutionsList(List<S> solutionSet, int[] ranking) {
    solutionsList = new Vector<S>(n);
    solutionsList.setSize(n);
    int populationSize = solutionSet.size();
    for (int i = 0; i < populationSize; i++) {
      S solution = solutionSet.get(i);
      solution.setAttribute(attributeId, ranking[i]);
      int index = (int) initialPopulation[i][ORDINALix];
      solution.setAttribute(SOLUTION_INDEX, index);
      solutionsList.set(index, solution);
    }
  }

  @Override
  public Ranking<S> computeRanking(List<S> solutionSet) {
    int populationSize = solutionSet.size();
    for (int i = 0; i < populationSize; i++) {
      initialPopulation[i] = new double[NumberOfix];
      System.arraycopy(solutionSet.get(i).getObjectives(), 0, initialPopulation[i], 0, m);
      initialPopulation[i][SolIDix] = i; // asignamos id a la solucion
    }
    int ranking[] = sort(initialPopulation);
    initializeSolutionsList(solutionSet, ranking);
    createRankedSubPopulations(bsManager.getLastRank(), populationSize);

    return this;
  }

  private void createRankedSubPopulations(int lastRank, int populationSize) {
    lastRank++;
    rankedSubPopulations = new ArrayList<ArrayList<S>>(lastRank);
    for (int r = rankedSubPopulations.size(); r <= lastRank; r++) {
      rankedSubPopulations.add(new ArrayList<S>());
    }
    Object rankAttr = attributeId;
    for (int i = 0; i < populationSize; i++) {
      S solution = solutionsList.get(i);
      int rank = (int) solution.getAttribute(rankAttr);
      rankedSubPopulations.get(rank).add(solution);
    }
  }

  public Ranking<S> steadyStateRanking() {
    steadyStateSort(updatedSolutionIndex);
    solutionsList.get(updatedSolutionIndex).setAttribute(attributeId, ranking[updatedSolutionIndex]);
    createRankedSubPopulations(bsManager.getLastRank(), n);
    return this;
  }

  public void removeSolution(S solution) {
    updatedSolutionIndex = (int) solution.getAttribute(SOLUTION_INDEX);
  }

  public void addSolution(S solution) {
    System.arraycopy(population, 0, initialPopulation, 0, n);
    merge_sort(population, initialPopulation, 0, n, ORDINALix, ORDINALix + 1);

    System.arraycopy(solution.getObjectives(), 0, initialPopulation[updatedSolutionIndex], 0, m);
    initialPopulation[updatedSolutionIndex][ORDINALix] = updatedSolutionIndex;
    System.arraycopy(initialPopulation, 0, population, 0, n);
    bsManager.setNewSolution(updatedSolutionIndex);
    solution.setAttribute(SOLUTION_INDEX, updatedSolutionIndex);
    solutionsList.set(updatedSolutionIndex, solution);
  }

  public void addSolution(double[] solution, int index) {
    System.arraycopy(solution, 0, initialPopulation[index], 0, m);
    initialPopulation[index][ORDINALix] = initialPopulation[index][SolIDix];
    System.arraycopy(initialPopulation, 0, population, 0, n);
    bsManager.setNewSolution(initialPopulation[index][ORDINALix]);
  }

  public int[] steadyStateSort(double ordNewSol) {
    int dominanceModifications[] = new int[n];
    ranking = new int[n];
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
    }
    if (bsManager.updatePopulation(dominanceModifications)) {
      for (int p = 0; p < n; p++) {
        int rank = bsManager.computeSteadyStateRank((int) population[p][ORDINALix]);
        ranking[(int) population[p][SolIDix]] = rank;
        population[p][RANKINGix] = rank;
      }
    }
    return ranking;
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

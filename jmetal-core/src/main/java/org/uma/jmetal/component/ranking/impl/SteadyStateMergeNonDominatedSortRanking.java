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
  private ArrayList<Dup> duplicatedSolutions;
  private MNDSBitsetManager_SS bsManager;
  private Vector<S> solutionsList;
  private int updatedSolutionIndex, initialPopulationSize;
  private List<ArrayList<S>> rankedSubPopulations;
  //TODO SOLO PARA PRUEBAS, QUITAR!  private PopulationPrinter printer;

  private class Dup {
    public Dup(double[] srcSol, double sIDix, int dupOrdix) {
      solSrc = srcSol;
      solDup = new double[NumberOfix];
      System.arraycopy(solSrc, 0, solDup, 0, m);
      solDup[SolIDix] = sIDix;
      solDup[ORDINALix] = dupOrdix;

    }

    public int updateRanking() {
      solDup[RANKINGix] = solSrc[RANKINGix];
      return (int) solDup[RANKINGix];
    }

    double[] solSrc;
    double[] solDup;
  }

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
    initialPopulationSize = n;
    SolIDix = m;
    ORDINALix = SolIDix + 1;
    RANKINGix = ORDINALix + 1;
    NumberOfix = RANKINGix + 1;
    _nonDomEarlyDetection = 0;
    ranking = new int[n];
    population = new double[n][];
    work = new double[n][];
    duplicatedSolutions = new ArrayList<Dup>(n);

    bsManager = new MNDSBitsetManager_SS(n, m);
    solutionsList = new Vector<S>(n);
    solutionsList.setSize(n);
    initialPopulation = new double[n][];
  }

  /*
  private void initializeSteadySteateObjects(int popSize) {
    n = popSize;
    initialPopulationSize = n;
    ranking = new int[n];
    population = new double[n][];
    duplicatedSolutions = new ArrayList<Dup>(n);
    bsManager = new MNDSBitsetManager_SS(n, m);

    population = new double[n][];
    work = new double[n][];
    initialPopulation = new double[n][];

    solutionsList = new Vector<S>(n);
    solutionsList.setSize(n);
  }
  */
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

  private boolean sortFirstObjective() {
    int p = 0, iDup = n;
    System.arraycopy(population, 0, work, 0, n);
    merge_sort(population, work, 0, n, 0, m);
    population[0] = work[0];
    population[0][ORDINALix] = 0;
    for (int q = 1; q < n; q++) {
      if (0 != compare_lex(population[p], work[q], 0, m)) {
        p++;
        population[p] = work[q];
        population[p][ORDINALix] = p;
      } else {
        duplicatedSolutions.add(new Dup(population[p], work[q][SolIDix], --iDup));
      }
    }
    for (Dup duplicated : duplicatedSolutions) {
      iDup = (int) duplicated.solDup[ORDINALix];
      population[iDup] = duplicated.solDup;
    }

    n = p + 1;
    return n > 1;
  }

  private boolean sortSecondObjective() {
    int p, solutionId, rank;
    boolean dominance = false;
    System.arraycopy(population, 0, work, 0, n); //necesario cuando hay duplicadas
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

  //
  private void updateDuplicated() {
    for (Dup duplicated : duplicatedSolutions) {
      int rank = duplicated.updateRanking();
      int src = (int) duplicated.solSrc[ORDINALix];
      int dup = (int) duplicated.solDup[ORDINALix];
      int dupSid = (int) duplicated.solDup[SolIDix];
      ranking[dupSid] = rank;
      bsManager.copyBitset(bsManager, src, dup);
    }
    n = initialPopulationSize;
  }

  // main
  final public int[] sort() {
    //INITIALIZATION
    _comparisonCounter = 0;
    //SORTING
    if (sortFirstObjective()) {
      if (sortSecondObjective()) {
        sortRestOfObjectives();
      }
    }
    updateDuplicated();
    return ranking;
  }

  private void initializeSolutionsList(List<S> solutionSet) {
    solutionsList = new Vector<S>(n);
    solutionsList.setSize(n);
    int sssize = solutionSet.size();
    for (int i = 0; i < sssize; i++) {
      S solution = solutionSet.get(i);
      int index = (int) population[i][SolIDix];
      solution.setAttribute(SOLUTION_INDEX, index);
      solution.setAttribute(rankAttr, (int) population[i][RANKINGix]);
      solutionsList.set(index, solution);
    }
  }

  @Override
  public Ranking<S> computeRanking(List<S> solutionSet) {
    for (int i = 0; i < n; i++) {
      population[i] = new double[NumberOfix];
      System.arraycopy(solutionSet.get(i).getObjectives(), 0, population[i], 0, m);
      population[i][SolIDix] = i; // asignamos id a la solucion
    }
    initializeSolutionsList(solutionSet);

    int ranking[] = sort();
    createRankedSubPopulations(bsManager.getLastRank(), solutionSet.size(), ranking);
    prepareToAdd1();
    return this;
  }

  private void prepareToAdd1() {
    Vector<S> solutionsList2 = new Vector<S>(n);
    solutionsList2.setSize(n);
    for (int i = 0; i < n; i++) {
      int solix = (int) population[i][SolIDix];
      int ordix = (int) population[i][ORDINALix];
      S sol = solutionsList.get(solix);
      sol.setAttribute(SOLUTION_INDEX, ordix);
      solutionsList2.set(ordix, sol);
      population[i][SolIDix] = ordix;
      initialPopulation[ordix] = population[i];
    }
    solutionsList = solutionsList2;
  }

  Object rankAttr = attributeId;

  private void createRankedSubPopulations(int lastRank, int populationSize, int[] ranking) {
    lastRank++;
    rankedSubPopulations = new ArrayList<ArrayList<S>>(lastRank);
    for (int r = rankedSubPopulations.size(); r < lastRank; r++) {
      rankedSubPopulations.add(new ArrayList<S>());
    }

    for (int i = 0; i < populationSize; i++) {
      S solution = solutionsList.get(i);
      solution.setAttribute(rankAttr, ranking[i]);
      rankedSubPopulations.get(ranking[i]).add(solution);
    }
  }

  public Ranking<S> steadyStateRanking() {
    int[] ranking = steadyStateSort(updatedSolutionIndex);
    createRankedSubPopulations(bsManager.getLastRank(), n, ranking);
    return this;
  }

  public void removeSolution(S solution) {
    updatedSolutionIndex = (int) solution.getAttribute(SOLUTION_INDEX);
  }

  public void addSolution(S solution) {
    System.arraycopy(solution.getObjectives(), 0, initialPopulation[updatedSolutionIndex], 0, m);
    initialPopulation[updatedSolutionIndex][RANKINGix] = 0;//reset ranking
    System.arraycopy(initialPopulation, 0, population, 0, n);
    bsManager.setNewSolution(updatedSolutionIndex);
    solution.setAttribute(SOLUTION_INDEX, updatedSolutionIndex);
    solutionsList.set(updatedSolutionIndex, solution);
  }

  public void addSolution(double[] solution, int index) {
    System.arraycopy(solution, 0, initialPopulation[index], 0, m);
    System.arraycopy(initialPopulation, 0, population, 0, n);
    bsManager.setNewSolution(initialPopulation[index][SolIDix]);
  }

  private boolean[] getDuplicatedSols(double ordNewSol) {
    boolean rankAssigned = false;
    boolean[] dups = new boolean[n];
    double[] newSol = initialPopulation[updatedSolutionIndex];
    int solix = (int) newSol[SolIDix];
    for (int p = 0; p < n; p++) {
      int pix = (int) population[p][SolIDix];
      if (pix != solix && 0 == compare_lex(population[p], newSol, 0, m)) {
        dups[pix] = true;
        if (!rankAssigned) {
          rankAssigned = true;
          bsManager.copyBitset(bsManager, (int) population[p][SolIDix], solix);
          ranking[solix] = (int) population[p][RANKINGix];
          newSol[RANKINGix] = population[p][RANKINGix];
        }
      }
    }
    if (!rankAssigned)
      dups = null;
    return dups;
  }

  public int[] steadyStateSort(double ordNewSol) {
    int dominanceModifications[] = new int[n];
    ranking = new int[n];
    boolean[] dupSols = getDuplicatedSols(ordNewSol);

    System.arraycopy(population, 0, work, 0, n);
    for (

            int p, obj = 0; obj < m; obj++) {
      merge_sort(population, work, 0, n, obj, 0 == obj ? m : obj + 1);
      System.arraycopy(work, 0, population, 0, n);
      for (p = 0; p < n && work[p][SolIDix] != ordNewSol; p++) {
        if (dupSols == null || !dupSols[(int) work[p][SolIDix]]) {
          dominanceModifications[(int) work[p][SolIDix]]--;
        }
      }

      for (p++; p < n; p++) {
        if (dupSols == null || !dupSols[(int) work[p][SolIDix]]) {
          dominanceModifications[(int) work[p][SolIDix]]++;
        }
      }
    }

    if (bsManager.updatePopulation(dominanceModifications)) {
      for (int p = 0; p < n; p++) {
        int rank = bsManager.computeSteadyStateRank((int) population[p][SolIDix]);
        int six = (int) population[p][SolIDix];
        ranking[six] = rank;
        population[p][RANKINGix] = rank;
        solutionsList.get(six).setAttribute(attributeId, rank);
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

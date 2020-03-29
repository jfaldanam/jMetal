package org.uma.jmetal.component.ranking.impl.util;

/**
 *
 * This class implements a simple bitset adapted to the Merge Non-dominated
 * Sorting (MNDS) algorithm
 * Please, note that in MNDS the size of a bitset can only be reduced or remain
 * the same
 * 
 * @author Javier Moreno <javier.morenom@edu.uah.es>
 */

public class MNDSBitsetManager_SS {
	private final static int FIRST_WORD_RANGE = 0;
	private final static int LAST_WORD_RANGE = 1;
	private final static int N_BIT_ADDR = 6;
	private final static int WORD_SIZE = 1 << N_BIT_ADDR;
	private static final long WORD_MASK = 0xffffffffffffffffL;
	private long[][] bitsets;
	private int[][] bsRanges;
	private int[] wordRanking;
	private int[] ranking;
	private int nSols, nObjs, maxRank, newSol, newSolWordIndex;
	private long newSolSet1, newSolClean;
	private long[] incrementalBitset;
	private int incBsFstWord, incBsLstWord, nWords;

	/////////////////////////////////////////////////
	///////// steady-state //////////////////////////
	/////////////////////////////////////////////////

	public void setNewSolution(double solutionId) {
		newSol = (int) solutionId;
		maxRank = 0;
		newSolWordIndex = newSol >> N_BIT_ADDR;
		newSolSet1 = 1L << newSol;
		newSolClean = ~(1L << newSol);
		wordRanking = new int[nWords];
		cleanBitset(newSol, 1, 0);
	}

	private int cleanBitset(int solutionId, int firstWord, int lastWord) {
		if (firstWord > lastWord) {
			ranking[solutionId] = 0;
			bitsets[solutionId] = new long[nWords];
			bsRanges[solutionId][FIRST_WORD_RANGE] = Integer.MAX_VALUE;
			bsRanges[solutionId][LAST_WORD_RANGE] = 0;
			return 0;
		}
		int size = lastWord - firstWord + 1;
		if (bsRanges[solutionId][FIRST_WORD_RANGE] != firstWord || bsRanges[solutionId][LAST_WORD_RANGE] != lastWord) {
			long[] bs = new long[nWords];
			System.arraycopy(bitsets[solutionId], firstWord, bs, firstWord, size);
			bitsets[solutionId] = new long[nWords];
			System.arraycopy(bs, firstWord, bitsets[solutionId], firstWord, size);
			bs = null;
			bsRanges[solutionId][FIRST_WORD_RANGE] = firstWord;
			bsRanges[solutionId][LAST_WORD_RANGE] = lastWord;
		}
		return size;
	}

	public boolean updatePopulation(int[] solutionList) {
		boolean existDominance = false;
		for (int solId = 0; solId < nSols; solId++) {
			if (solutionList[solId] == nObjs) { // newSol dominates solutionList[solId]
				if (null == bitsets[solId]) {
					bitsets[solId] = new long[nWords];
					bsRanges[solId][FIRST_WORD_RANGE] = newSolWordIndex;
					bsRanges[solId][LAST_WORD_RANGE] = newSolWordIndex;
				} else {
					if (bsRanges[solId][FIRST_WORD_RANGE] > newSolWordIndex)
						bsRanges[solId][FIRST_WORD_RANGE] = newSolWordIndex;
					if (bsRanges[solId][LAST_WORD_RANGE] < newSolWordIndex)
						bsRanges[solId][LAST_WORD_RANGE] = newSolWordIndex;
				}
				bitsets[solId][newSolWordIndex] |= newSolSet1;
				existDominance = true;
				continue;
			}
			if (solutionList[solId] == -nObjs) { // solutionList[solId] dominates newSol
				int wordIndex = solId >> N_BIT_ADDR;
				bitsets[newSol][wordIndex] |= (1L << solId);
				if (bsRanges[newSol][LAST_WORD_RANGE] < wordIndex)
					bsRanges[newSol][LAST_WORD_RANGE] = wordIndex;
				if (bsRanges[newSol][FIRST_WORD_RANGE] > wordIndex)
					bsRanges[newSol][FIRST_WORD_RANGE] = wordIndex;
			}
			if (bitsets[solId] != null && (bitsets[solId][newSolWordIndex] & newSolSet1) != 0) {
				bitsets[solId][newSolWordIndex] &= newSolClean; //borrado bit asociado a la solucion eliminada
				if (bitsets[solId][newSolWordIndex] == 0) { // compactacion
					int fw = bsRanges[solId][FIRST_WORD_RANGE];
					int lw = bsRanges[solId][LAST_WORD_RANGE];
					while (fw <= lw && 0 == bitsets[solId][fw])
						fw++;
					while (fw <= lw && 0 == bitsets[solId][lw])
						lw--;
					cleanBitset(solId, fw, lw);
				}
			}
			existDominance |= bsRanges[solId][LAST_WORD_RANGE] >= bsRanges[solId][FIRST_WORD_RANGE];
		}
		return existDominance;
	}

	public int computeSteadyStateRank(int solutionId) {
		int fw = bsRanges[solutionId][FIRST_WORD_RANGE];
		int lw = bsRanges[solutionId][LAST_WORD_RANGE];
		return getRank(solutionId, fw, lw);
	}

	/////////////////////////////////////////////////
	/////////////////////////////////////////////////
	/////////////////////////////////////////////////
	public void freeMem() {
		incrementalBitset = null;
		bitsets = null;
		bsRanges = null;
		wordRanking = null;
		ranking = null;
	}

	public boolean updateSolutionDominance(int solutionId) {
		int fw = bsRanges[solutionId][FIRST_WORD_RANGE];
		int lw = bsRanges[solutionId][LAST_WORD_RANGE];

		if (lw > incBsLstWord)
			lw = incBsLstWord;
		if (fw < incBsFstWord)
			fw = incBsFstWord;

		while (fw <= lw && 0 == (bitsets[solutionId][fw] & incrementalBitset[fw]))
			fw++;
		while (fw <= lw && 0 == (bitsets[solutionId][lw] & incrementalBitset[lw]))
			lw--;

		if (0 == cleanBitset(solutionId, fw, lw)) {
			return false;
		}
		for (; fw <= lw; fw++)
			bitsets[solutionId][fw] &= incrementalBitset[fw];
		return true;
	}

	private int getRank(int solutionId, int fw, int lw) {
		long word;
		int i = 0, rank = 0, offset;
		for (; fw <= lw; fw++) {
			word = bitsets[solutionId][fw];
			if (word != 0) {
				i = Long.numberOfTrailingZeros(word);
				offset = fw * WORD_SIZE;
				do {
					if (ranking[offset + i] >= rank)
						rank = ranking[offset + i] + 1;
					i++;
					i += Long.numberOfTrailingZeros(word >> i);
				} while (i < WORD_SIZE && rank <= wordRanking[fw]);
				if (rank > maxRank) {
					maxRank = rank;
					break;
				}
			}
		}
		ranking[solutionId] = rank;
		i = solutionId >> N_BIT_ADDR;
		if (rank > wordRanking[i])
			wordRanking[i] = rank;

		return rank;
	}

	public int getLastRank() {
		return maxRank;
	}

	public int computeSolutionRanking(int solutionId) {
		int fw = bsRanges[solutionId][FIRST_WORD_RANGE];
		int lw = bsRanges[solutionId][LAST_WORD_RANGE];
		if (lw > incBsLstWord)
			lw = incBsLstWord;
		if (fw < incBsFstWord)
			fw = incBsFstWord;

		while (fw <= lw && 0 == (bitsets[solutionId][fw] & incrementalBitset[fw]))
			fw++;
		while (fw <= lw && 0 == (bitsets[solutionId][lw] & incrementalBitset[lw]))
			lw--;

		if (0 == cleanBitset(solutionId, fw, lw)) {
			return 0;
		}
		for (int i = fw; i <= lw; i++) {
			bitsets[solutionId][i] &= incrementalBitset[i];
		}
		return getRank(solutionId, fw, lw);
	}

	public void updateIncrementalBitset(int solutionId) {
		int wordIndex = solutionId >> N_BIT_ADDR;
		incrementalBitset[wordIndex] |= (1L << solutionId);
		if (incBsLstWord < wordIndex)
			incBsLstWord = wordIndex;
		if (incBsFstWord > wordIndex)
			incBsFstWord = wordIndex;
	}

	public boolean initializeSolutionBitset(int solutionId) {
		int wordIndex = solutionId >> N_BIT_ADDR;
		if (wordIndex < incBsFstWord || 0 == solutionId) {
			bsRanges[solutionId][FIRST_WORD_RANGE] = Integer.MAX_VALUE;
			return false;
		} else if (wordIndex == incBsFstWord) { //only 1 word in common
			bitsets[solutionId] = new long[nWords];// ahora puece crecer y menguar --> tamaño maximo new long[wordIndex + 1];
			long intersection = incrementalBitset[incBsFstWord] & ~(WORD_MASK << solutionId);
			if (intersection != 0) {
				bsRanges[solutionId][FIRST_WORD_RANGE] = wordIndex;
				bsRanges[solutionId][LAST_WORD_RANGE] = wordIndex;
				bitsets[solutionId][wordIndex] = intersection;
			}
			return intersection != 0;
		}
		//more than one word in common
		int lw = incBsLstWord < wordIndex ? incBsLstWord : wordIndex;
		bsRanges[solutionId][FIRST_WORD_RANGE] = incBsFstWord;
		bsRanges[solutionId][LAST_WORD_RANGE] = lw;
		bitsets[solutionId] = new long[nWords];// maximo tamaño posible new long[lw + 1];
		System.arraycopy(incrementalBitset, incBsFstWord, bitsets[solutionId], incBsFstWord, lw - incBsFstWord + 1);
		if (incBsLstWord >= wordIndex) { // update (compute intersection) the last word
			bitsets[solutionId][lw] = incrementalBitset[lw] & ~(WORD_MASK << solutionId);
			if (bitsets[solutionId][lw] == 0) {
				bsRanges[solutionId][LAST_WORD_RANGE]--;
			}
		}
		return true;
	}

	public void clearIncrementalBitset() {
		incrementalBitset = new long[incrementalBitset.length];
		incBsLstWord = 0;
		incBsFstWord = Integer.MAX_VALUE;
	}

	public MNDSBitsetManager_SS(int nSolutions, int nObjectives) {
		nSols = nSolutions;
		nWords = nSols >> N_BIT_ADDR;
		nWords++;

		nObjs = nObjectives;
		ranking = new int[nSolutions];
		wordRanking = new int[nWords];
		bitsets = new long[nSolutions][];
		bsRanges = new int[nSolutions][2];
		incrementalBitset = new long[nWords];
		incBsLstWord = 0;
		incBsFstWord = Integer.MAX_VALUE;
	}
}


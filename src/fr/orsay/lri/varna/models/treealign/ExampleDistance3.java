package fr.orsay.lri.varna.models.treealign;
import java.util.ArrayList;



/**
 * 
 * 
 * @author Raphael Champeimont
 */
public class ExampleDistance3 implements TreeAlignLabelDistanceSymmetric<RNANodeValue2> {
	public double f(RNANodeValue2 v1, RNANodeValue2 v2) {
		if (v1 == null) {
			if (v2 == null) {
				return 0;
			} else if (!v2.isSingleNode()) { // v2 is a list of bases
				// We insert all bases, with a cost of 1 for each base.
				return v2.getNodes().size();
			} else { // v2 is a single node
				return 2;
			}
		} else if (!v1.isSingleNode()) { // v1 is a list of bases
			if (v2 == null) {
				return v1.getNodes().size();
			} else if (!v2.isSingleNode()) { // v2 is a list of bases
				// We compute the sequence distance
				return alignSequenceNodes(v1, v2).getDistance();
			} else { // v2 is a single node
				return 2 + v1.getNodes().size();
			}
		} else { // v1 is a single node
			// all the same as when v1 == null
			if (v2 == null) {
				return 2;
			} else if (!v2.isSingleNode()) { // v2 is a list of bases
				return 2 + v2.getNodes().size();
			} else { // v2 is a single node
				String l1 = v1.getNode().getLeftNucleotide();
				String r1 = v1.getNode().getRightNucleotide();
				String l2 = v2.getNode().getLeftNucleotide();
				String r2 = v2.getNode().getRightNucleotide();
				// We have cost(subst((x,y) to (x',y'))) = 1
				// when x != x' and y != y'.
				// It means it is less than substituting 2 non-paired bases 
				return (!l1.equals(l2) ? 0.5 : 0)
				     + (!r1.equals(r2) ? 0.5 : 0);
			}
		}
	}
	

	public class SequenceAlignResult {
		private double distance;
		private int[][] alignment;
		
		public double getDistance() {
			return distance;
		}
		public void setDistance(double distance) {
			this.distance = distance;
		}
		
		/** The result array is a matrix of height 2
		 * and width at most length(sequence A) + length(sequence B).
		 * with result[0] is the alignment for A
		 * and result[1] the alignment for B.
		 * The alignment consists int the indexes of the original
		 * bases positions, with -1 when there is no match.
		 */
		public int[][] getAlignment() {
			return alignment;
		}
		public void setAlignment(int[][] alignment) {
			this.alignment = alignment;
		}
		
	}
	
	/**
	 * Align two sequences contained in nodes.
	 * Both nodes have to be non-single nodes, otherwise an
	 * RNANodeValue2WrongTypeException exception will be thrown.
	 */
	public SequenceAlignResult alignSequenceNodes(RNANodeValue2 v1, RNANodeValue2 v2) {
		char[] A = v1.computeSequence();
		char[] B = v2.computeSequence();
		return alignSequences(A, B);
	}
	
	/**
	 * Align sequences using the Needleman-Wunsch algorithm.
	 * Time: O(A.length * B.length)
	 * Space: O(A.length * B.length)
	 * Space used by the returned object: O(A.length + B.length)
	 */
	public SequenceAlignResult alignSequences(char[] A, char[] B) {
		SequenceAlignResult result = new SequenceAlignResult();
		
		final int la = A.length;
		final int lb = B.length;
		double[][] F = new double[la+1][lb+1];
		int[][] decisions = new int[la+1][lb+1];
		final double d = 1; // insertion/deletion cost
		final double substCost = 1; // substitution cost
		for (int i=0; i<=la; i++)
			F[i][0] = d*i;
		for (int j=0; j<=lb; j++)
			F[0][j] = d*j;
		for (int i=1; i<=la; i++)
			for (int j=1; j<=lb; j++)
			{
				double min;
				int decision;
				double match = F[i-1][j-1] + (A[i-1] == B[j-1] ? 0 : substCost);
				double delete = F[i-1][j] + d;
				if (match < delete) {
					decision = 1;
					min = match;
				} else {
					decision = 2;
					min = delete;
				}
				double insert = F[i][j-1] + d;
				if (insert < min) {
					decision = 3;
					min = insert;
				}
				F[i][j] = min;
				decisions[i][j] = decision;
			}
		
		result.setDistance(F[la][lb]);

		int[][] alignment = computeAlignment(F, decisions, A, B);
		result.setAlignment(alignment);
		
		return result;
	}
	
	private int[][] computeAlignment(double[][] F, int[][] decisions, char[] A, char[] B) {
		// At worst the alignment will be of length (A.length + B.length)
		ArrayList<Integer> AlignmentA = new ArrayList<Integer>(A.length + B.length);
		ArrayList<Integer> AlignmentB = new ArrayList<Integer>(A.length + B.length);
		int i = A.length;
		int j = B.length;
		while (i > 0 && j > 0)
		{
			int decision = decisions[i][j];
			switch (decision) {
			case 1:
				AlignmentA.add(i-1);
				AlignmentB.add(j-1);
				i = i - 1;
				j = j - 1;
				break;
			case 2:
				AlignmentA.add(i-1);
				AlignmentB.add(-1);
				i = i - 1;
				break;
			case 3:
				AlignmentA.add(-1);
				AlignmentB.add(j-1);
				j = j - 1;
				break;
			default:
				throw (new Error("Bug in ExampleDistance3: decision = " + decision));
			}
		}
		while (i > 0)
		{
			AlignmentA.add(i-1);
			AlignmentB.add(-1);
			i = i - 1;
		}
		while (j > 0)
		{
			AlignmentA.add(-1);
			AlignmentB.add(j-1);
			j = j - 1;
		}

		// Convert the ArrayLists to the right format:
		// We need to reverse the list and change the numbering of
		int l = AlignmentA.size();
		int[][] result = new int[2][l];
		for (i=0; i<l; i++) {
			result[0][i] = AlignmentA.get(l-1-i);
			result[1][i] = AlignmentB.get(l-1-i);
		}
		return result;
		
	}
}

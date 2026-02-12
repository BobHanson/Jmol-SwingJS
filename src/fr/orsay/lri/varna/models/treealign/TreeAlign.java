package fr.orsay.lri.varna.models.treealign;

import java.util.*;


/**
 * Tree alignment algorithm.
 * This class implements the tree alignment algorithm
 * for ordered trees explained in article:
 *   T. Jiang, L. Wang, K. Zhang,
 *   Alignment of trees - an alternative to tree edit,
 *   Theoret. Comput. Sci. 143 (1995).
 * Other references:
 * - Claire Herrbach, Alain Denise and Serge Dulucq.
 *   Average complexity of the Jiang-Wang-Zhang pairwise tree alignment
 *   algorithm and of a RNA secondary structure alignment algorithm.
 *   Theoretical Computer Science 411 (2010) 2423-2432.
 *   
 * Our implementation supposes that the trees will never have more
 * than 32000 nodes and that the total distance will never require more
 * significant digits that a float (single precision) has. 
 * 
 * @author Raphael Champeimont
 * @param <ValueType1> The type of values on nodes in the first tree.
 * @param <ValueType2> The type of values on nodes in the second tree.
 */
public class TreeAlign<ValueType1, ValueType2> {
	
	private class TreeData<ValueType> {
		/**
		 * The tree.
		 */
		public Tree<ValueType> tree;
		
		/**
		 * The tree size (number of nodes).
		 */
		public int size = -1;
		
		/**
		 * The number of children of a node is called the node degree.
		 * This variable is the maximum node degree in the tree.
		 */
		public int degree = -1;
		
		/**
		 * The number of children of a node is called the node degree.
		 * degree[i] is the degree of node i, with i being an index in nodes.
		 */
		public int[] degrees;
		
		/**
		 * The trees as an array of its nodes (subtrees rooted at each node
		 * in fact), in postorder. 
		 */
		public Tree<ValueType>[] nodes;
		
		/**
		 * children[i] is the array of children (as indexes in nodes)
		 * of i (an index in nodes)
		 */
		public int[][] children;
		
		/**
		 * Values of nodes.
		 */
		public ValueType[] values;
	}


	/**
	 * The distance function between labels.
	 */
	private TreeAlignLabelDistanceAsymmetric<ValueType1,ValueType2> labelDist;

	
	/**
	 * Create a TreeAlignSymmetric object, which can align trees.
	 * The distance function will be called only once on every pair
	 * of nodes. The result is then kept in a matrix, so you need not manage
	 * yourself a cache of f(value1, value2).
	 * Note that it is permitted to have null values on nodes,
	 * so comparing a node with a non-null value with a node with a null
	 * value will give the same cost as to insert the first node.
	 * This can be useful if you tree has "fake" nodes.
	 * @param labelDist The label distance.
	 */
	public TreeAlign(TreeAlignLabelDistanceAsymmetric<ValueType1,ValueType2> labelDist) {
		this.labelDist = labelDist;
	}
	

	
	private class ConvertTreeToArray<ValueType> {
		private int nextNodeIndex = 0;
		private TreeData<ValueType> treeData;
		
		public ConvertTreeToArray(TreeData<ValueType> treeData) {
			this.treeData = treeData;
		}
		
		private void convertTreeToArrayAux(
				Tree<ValueType> subtree,
				int[] siblingIndexes,
				int siblingNumber) throws TreeAlignException {
			// We want it in postorder, so first we put the children
			List<Tree<ValueType>> children = subtree.getChildren();
			int numberOfChildren = children.size();
			int[] childrenIndexes = new int[numberOfChildren];
			int myIndex = -1;
			{
				int i = 0;
				for (Tree<ValueType> child: children) {
					convertTreeToArrayAux(child, childrenIndexes, i);
					i++;
				}
			}
			// Compute the maximum degree
			if (numberOfChildren > treeData.degree) {
				treeData.degree = numberOfChildren;
			}
			// Now we add the node (root of the given subtree).
			myIndex = nextNodeIndex;
			nextNodeIndex++;
			treeData.nodes[myIndex] = subtree;
			// Record how many children I have
			treeData.degrees[myIndex] = numberOfChildren;
			// Store my value in an array
			ValueType v = subtree.getValue();
			treeData.values[myIndex] = v;
			// Tell the caller my index
			siblingIndexes[siblingNumber] = myIndex;
			// Record my children indexes
			treeData.children[myIndex] = childrenIndexes;
		}
		
		/**
		 * Reads: treeData.tree
		 * Computes: treeData.nodes, treeData.degree, treeData.degrees
		 *           treeData.fathers, treeData.children, treeData.size,
		 *           treeData.values
		 * Converts a tree to an array of nodes, in postorder.
		 * We also compute the maximum node degree in the tree.
		 * @throws TreeAlignException 
		 */
		@SuppressWarnings("unchecked")
		public void convert() throws TreeAlignException {
			treeData.degree = 0;
			treeData.size = treeData.tree.countNodes();
			// we didn't write new Tree<ValueType>[treeData.size] because
			// java does not support generics with arrays
			treeData.nodes = new Tree[treeData.size];
			treeData.children = new int[treeData.size][];
			treeData.degrees = new int[treeData.size];
			treeData.values = (ValueType[]) new Object[treeData.size];
			int rootIndex[] = new int[1];
			convertTreeToArrayAux(treeData.tree, rootIndex, 0);
		}
	}
	

	/**
	 * For arrays that take at least O(|T1|*|T2|) we take care
	 * not to use too big data types.
	 */
	private class Aligner {
		/**
		 * The first tree.
		 */
		private TreeData<ValueType1> treeData1;
		
		/**
		 * The second tree. 
		 */
		private TreeData<ValueType2> treeData2;
		
		/**
		 * DF1[i][j_t] is DFL for (i,j,s,t) with s=0.
		 * See description of DFL in Aligner.computeAlignmentP1().
		 * DF1 and DF2 are the "big" arrays, ie. those that may the space
		 * complexity what it is.
		 */
		private float[][][][] DF1;
		
		/**
		 * DF2[j][i_s] is DFL for (i,j,s,t) with t=0.
		 * See description of DFL in Aligner.computeAlignmentP1().
		 */
		private float[][][][] DF2;
		
		/**
		 * This arrays have the same shape as respectively DF1.
		 * They are used to remember which term in the minimum won, so that
		 * we can compute the alignment.
		 * Decision1 is a case number (< 10)
		 * and Decision2 is a child index, hence the types.
		 */
		private byte[][][][] DF1Decisions1;
		private short[][][][] DF1Decisions2;
		
		/**
		 * This arrays have the same shape as respectively DF2.
		 * They are used to remember which term in the minimum won, so that
		 * we can compute the alignment.
		 */
		private byte[][][][] DF2Decisions1;
		private short[][][][] DF2Decisions2;
		
		/**
		 * Distances between subtrees.
		 * DT[i][j] is the distance between the subtree rooted at i in the first tree
		 * and the subtree rooted at j in the second tree.
		 */
		private float[][] DT;
		
		/**
		 * This array has the same shape as DT, but is used to remember which
		 * case gave the minimum, so that we can later compute the alignment.
		 */
		private byte[][] DTDecisions1;
		private short[][] DTDecisions2;
		
		/**
		 * Distances between labels.
		 * DL[i][j] is the distance labelDist.f(value(T1[i]), value(T2[i])).
		 * By convention, we say that value(T1[|T1|]) = null
		 * and value(T2[|T2|]) = null
		 */
		private float[][] DL;
		
		/**
		 * DET1[i] is the distance between the empty tree and T1[i]
		 * (the subtree rooted at node i in the first tree).
		 */
		private float[] DET1;
		
		/**
		 * Same as DET1, but for second tree.
		 */
		private float[] DET2;
		
		/**
		 * DEF1[i] is the distance between the empty forest and F1[i]
		 * (the forest of children of node i in the first tree).
		 */
		private float[] DEF1;
		
		/**
		 * Same as DEF1, but for second tree.
		 */
		private float[] DEF2;
		
		
		/**
		 * @param i node in T1
		 * @param s number of first child of i to consider
		 * @param m_i degree of i
		 * @param j node in T2
		 * @param t number of first child of j to consider
		 * @param n_j degree of j
		 * @param DFx which array to fill (DF1 or DF2)
		 */
		private void computeAlignmentP1(int i, int s, int m_i, int j, int t, int n_j, int DFx) {
			/**
			 * DFL[pr][qr] is D(F1[i_s, i_p], F2[j_t, j_q])
			 * where p=s+pr-1 and q=t+qr-1 (ie. pr=p-s+1 and qr=q-t+1)
			 * By convention, F1[i_s, i_{s-1}] and F2[j_t, j_{t-1}] are the
			 * empty forests.
			 * Said differently, DFL[pr][qr] is the distance between the forest
			 * of the pr first children of i, starting with child s
			 * (first child is s = 0), and the forest of the qr first children
			 * of j, starting with child t (first child is t = 0).
			 * This array is allocated for a fixed value of (i,j,s,t).
			 */
			float[][] DFL;
			
			/**
			 * Same shape as DFL, but to remember which term gave the min,
			 * so that we can later compute the alignment.
			 */
			byte[][] DFLDecisions1;
			short[][] DFLDecisions2;
			
			DFL = new float[m_i-s+2][n_j-t+2];
			DFL[0][0] = 0; // D(empty forest, empty forest) = 0
			
			DFLDecisions1 = new byte[m_i-s+2][n_j-t+2];
			DFLDecisions2 = new short[m_i-s+2][n_j-t+2];
			
			// Compute indexes of i_s and j_t because we will need them
			int i_s = m_i != 0 ? treeData1.children[i][s] : -1;
			int j_t = n_j != 0 ? treeData2.children[j][t] : -1;
			
			for (int p=s; p<m_i; p++) {
				DFL[p-s+1][0] = DFL[p-s][0] + DET1[treeData1.children[i][p]];
			}
			
			for (int q=t; q<n_j; q++) {
				DFL[0][q-t+1] = DFL[0][q-t] + DET2[treeData2.children[j][q]];
			}
			
			for (int p=s; p<m_i; p++) {
				int i_p = treeData1.children[i][p];
				for (int q=t; q<n_j; q++) {
					int j_q = treeData2.children[j][q];
					
					float min = Float.POSITIVE_INFINITY;
					int decision1 = -1;
					int decision2 = -1;
					
					// Lemma 3 - Case: We delete the rightmost tree of T1
					{
						float minCandidate = DFL[p-s][q-t+1] + DET1[i_p];
						if (minCandidate < min) {
							min = minCandidate;
							decision1 = 1;
						}
					}
					
					// Lemma 3 - Case: We insert the rightmost tree of T2 (symmetric of previous case)
					{
						float minCandidate = DFL[p-s+1][q-t] + DET2[j_q];
						if (minCandidate < min) {
							min = minCandidate;
							decision1 = 2;
						}
					}
					
					// Lemma 3 - Case: Align rightmost trees with each other
					{
						float minCandidate = 
							DFL[p-s][q-t] + DT [i_p] [j_q];
						if (minCandidate < min) {
							min = minCandidate;
							decision1 = 3;
						}
					}
					
					// Lemma 3 - Case: We cut the T1 forest and match the first part
					// with the T2 forest except the rightmost tree, and we match the second
					// part with the T2 rightmost tree's forest of children
					{
						float minCandidate = Float.POSITIVE_INFINITY;
						int best_k = -1;
						for (int k=s; k<p; k++) {
							float d = DFL[k-s][q-t]
							         + DF2 [j_q] [treeData1.children[i][k]] [p-k+1] [treeData2.degrees[j_q]];
							if (d < minCandidate) {
								minCandidate = d;
								best_k = k;
							}
						}
						minCandidate += DL[treeData1.size][j_q];
						if (minCandidate < min) {
							min = minCandidate;
							decision1 = 4;
							decision2 = best_k;
						}
					}
					
					// Lemma 3 - Case: Syemmetric of preivous case
					{
						float minCandidate = Float.POSITIVE_INFINITY;
						int best_k = -1;
						for (int k=t; k<q; k++) {
							float d = DFL[p-s][k-t]
							         + DF1 [i_p] [treeData2.children[j][k]] [treeData1.degrees[i_p]] [q-k+1];
							if (d < minCandidate) {
								minCandidate = d;
								best_k = k;
							}
						}
						minCandidate += DL[i_p][treeData2.size];
						if (minCandidate < min) {
							min = minCandidate;
							decision1 = 5;
							decision2 = best_k;
						}
					}
					
					DFL[p-s+1][q-t+1] = min;
					DFLDecisions1[p-s+1][q-t+1] = (byte) decision1;
					DFLDecisions2[p-s+1][q-t+1] = (short) decision2;
				}
			}
			
			// Copy references to DFL to persistent arrays
			if (DFx == 2) {
				DF2[j][i_s] = DFL;
				DF2Decisions1[j][i_s] = DFLDecisions1;
				DF2Decisions2[j][i_s] = DFLDecisions2;
			} else {
				DF1[i][j_t] = DFL;
				DF1Decisions1[i][j_t] = DFLDecisions1;
				DF1Decisions2[i][j_t] = DFLDecisions2;
			}
			
		}
		
		public float align() throws TreeAlignException {
			(new ConvertTreeToArray<ValueType1>(treeData1)).convert();
			(new ConvertTreeToArray<ValueType2>(treeData2)).convert();
			
			// Allocate necessary arrays
			DT = new float[treeData1.size][treeData2.size];
			DTDecisions1 = new byte[treeData1.size][treeData2.size];
			DTDecisions2 = new short[treeData1.size][treeData2.size];
			DL = new float[treeData1.size+1][treeData2.size+1];
			DET1 = new float[treeData1.size];
			DET2 = new float[treeData2.size];
			DEF1 = new float[treeData1.size];
			DEF2 = new float[treeData2.size];
			DF1 = new float[treeData1.size][treeData2.size][][];
			DF1Decisions1 = new byte[treeData1.size][treeData2.size][][];
			DF1Decisions2 = new short[treeData1.size][treeData2.size][][];
			DF2 = new float[treeData2.size][treeData1.size][][];
			DF2Decisions1 = new byte[treeData2.size][treeData1.size][][];
			DF2Decisions2 = new short[treeData2.size][treeData1.size][][];
			
			DL[treeData1.size][treeData2.size] = (float) labelDist.f(null, null);

			for (int i=0; i<treeData1.size; i++) {
				int m_i = treeData1.degrees[i];
				DEF1[i] = 0;
				for (int k=0; k<m_i; k++) {
					DEF1[i] += DET1[treeData1.children[i][k]];
				}
				DL[i][treeData2.size] = (float) labelDist.f((ValueType1) treeData1.values[i], null);
				DET1[i] = DEF1[i] + DL[i][treeData2.size];
			}
			
			for (int j=0; j<treeData2.size; j++) {
				int n_j = treeData2.degrees[j];
				DEF2[j] = 0;
				for (int k=0; k<n_j; k++) {
					DEF2[j] += DET2[treeData2.children[j][k]];
				}
				DL[treeData1.size][j] = (float) labelDist.f(null, (ValueType2) treeData2.values[j]);
				DET2[j] = DEF2[j] + DL[treeData1.size][j];
			}
			

			for (int i=0; i<treeData1.size; i++) {
				int m_i = treeData1.degrees[i];
				for (int j=0; j<treeData2.size; j++) {
					int n_j = treeData2.degrees[j];
					
					// Precompute f(value(i), value(j)) and keep the result
					// to avoid calling f on the same values several times.
					// This is important in case the computation of f takes
					// long.
					DL[i][j] = (float) labelDist.f((ValueType1) treeData1.values[i], (ValueType2) treeData2.values[j]);
					
					for (int s=0; s<m_i; s++) {
						computeAlignmentP1(i, s, m_i, j, 0, n_j, 2);
					}
					
					for (int t=0; t<n_j; t++) {
						computeAlignmentP1(i, 0, m_i, j, t, n_j, 1);
					}
					
					DT[i][j] = Float.POSITIVE_INFINITY;
					// Lemma 2 - Case: Root is (blank, j)
					{
						float minCandidate = Float.POSITIVE_INFINITY;
						int best_r = -1;
						for (int r=0; r<n_j; r++) {
							float d = DT[i][treeData2.children[j][r]] - DET2[treeData2.children[j][r]];
							if (d < minCandidate) {
								minCandidate = d;
								best_r = r;
							}
						}
						minCandidate += DET2[j];
						if (minCandidate < DT[i][j]) {
							DT[i][j] = minCandidate;
							DTDecisions1[i][j] = 1;
							DTDecisions2[i][j] = (short) best_r;
						}
					}
					// Lemma 2 - Case: Root is (i, blank)
					{
						float minCandidate = Float.POSITIVE_INFINITY;
						int best_r = -1;
						for (int r=0; r<m_i; r++) {
							float d = DT[treeData1.children[i][r]][j] - DET1[treeData1.children[i][r]];
							if (d < minCandidate) {
								minCandidate = d;
								best_r = r;
							}
						}
						minCandidate += DET1[i];
						if (minCandidate < DT[i][j]) {
							DT[i][j] = minCandidate;
							DTDecisions1[i][j] = 2;
							DTDecisions2[i][j] = (short) best_r;
						}
					}
					// Lemma 2 - Case: Root is (i,j)
					{
						float minCandidate;
						if (n_j != 0) {
							minCandidate = DF1 [i] [treeData2.children[j][0]] [m_i] [n_j];
						} else {
							if (m_i != 0) {
								minCandidate = DF2 [j] [treeData1.children[i][0]] [m_i] [n_j];
							} else {
								minCandidate = 0; // D(empty forest, empty forest) = 0
							}
						}
						minCandidate += DL[i][j];
						if (minCandidate < DT[i][j]) {
							DT[i][j] = minCandidate;
							DTDecisions1[i][j] = 3;
						}
					}
					
					
				}
			}

			
			// We return the distance beetween T1[root] and T2[root].
			return DT[treeData1.size-1][treeData2.size-1];
		}
		
		public Aligner(Tree<ValueType1> T1, Tree<ValueType2> T2) {
			treeData1 = new TreeData<ValueType1>();
			treeData1.tree = T1;
			treeData2 = new TreeData<ValueType2>();
			treeData2.tree = T2;
		}
		
		/** Align F1[i_s,i_p] with F2[j_t,j_q].
		 * If p = s-1, by convention it means F1[i_s,i_p] = empty forest.
		 * Idem for q=t-1.
		 */
		private List<Tree<AlignedNode<ValueType1,ValueType2>>> computeForestAlignment(int i, int s, int p, int j, int t, int q) {
			if (p == s-1) { // left forest is the empty forest
				List<Tree<AlignedNode<ValueType1,ValueType2>>> result = new ArrayList<Tree<AlignedNode<ValueType1,ValueType2>>>();
				for (int k=t; k<=q; k++) {
					result.add(treeInserted(treeData2.children[j][k]));
				}
				return result;
			} else {
				if (q == t-1) { // right forest is the empty forest
					List<Tree<AlignedNode<ValueType1,ValueType2>>> result = new ArrayList<Tree<AlignedNode<ValueType1,ValueType2>>>();
					for (int k=s; k<=p; k++) {
						result.add(treeDeleted(treeData1.children[i][k]));
					}
					return result;
				} else { // both forests are non-empty
					int decision1, k;
					if (s == 0) {
						decision1 =
							DF1Decisions1 [i] [treeData2.children[j][t]] [p-s+1] [q-t+1];
						k = 
							DF1Decisions2 [i] [treeData2.children[j][t]] [p-s+1] [q-t+1];
					} else if (t == 0) {
						decision1 =
							DF2Decisions1 [j] [treeData1.children[i][s]] [p-s+1] [q-t+1];
						k = 
							DF2Decisions2 [j] [treeData1.children[i][s]] [p-s+1] [q-t+1];
					} else {
						throw (new Error("TreeAlignSymmetric bug: both s and t are non-zero"));
					}
					switch (decision1) {
					case 1:
					{
						List<Tree<AlignedNode<ValueType1,ValueType2>>> result;
						result = computeForestAlignment(i, s, p-1, j, t, q);
						result.add(treeDeleted(treeData1.children[i][p]));
						return result;
					}
					case 2:
					{
						List<Tree<AlignedNode<ValueType1,ValueType2>>> result;
						result = computeForestAlignment(i, s, p, j, t, q-1);
						result.add(treeInserted(treeData2.children[j][q]));
						return result;
					}
					case 3:
					{
						List<Tree<AlignedNode<ValueType1,ValueType2>>> result;
						result = computeForestAlignment(i, s, p-1, j, t, q-1);
						result.add(computeTreeAlignment(treeData1.children[i][p], treeData2.children[j][q]));
						return result;
					}
					case 4:
					{
						List<Tree<AlignedNode<ValueType1,ValueType2>>> result;
						result = computeForestAlignment(i, s, k-1, j, t, q-1);
						
						int j_q = treeData2.children[j][q];
						Tree<AlignedNode<ValueType1,ValueType2>> insertedNode = new Tree<AlignedNode<ValueType1,ValueType2>>();
						AlignedNode<ValueType1,ValueType2> insertedNodeValue = new AlignedNode<ValueType1,ValueType2>();
						insertedNodeValue.setLeftNode(null);
						insertedNodeValue.setRightNode((Tree<ValueType2>) treeData2.nodes[j_q]);
						insertedNode.setValue(insertedNodeValue);
						
						insertedNode.replaceChildrenListBy(computeForestAlignment(i, k, p, j_q, 0, treeData2.degrees[j_q]-1));
						
						result.add(insertedNode);
						
						return result;
					}
					case 5:
					{
						List<Tree<AlignedNode<ValueType1,ValueType2>>> result;
						result = computeForestAlignment(i, s, p-1, j, t, k-1);
						
						int i_p = treeData1.children[i][p];
						Tree<AlignedNode<ValueType1,ValueType2>> deletedNode = new Tree<AlignedNode<ValueType1,ValueType2>>();
						AlignedNode<ValueType1,ValueType2> deletedNodeValue = new AlignedNode<ValueType1,ValueType2>();
						deletedNodeValue.setLeftNode((Tree<ValueType1>) treeData1.nodes[i_p]);
						deletedNodeValue.setRightNode(null);
						deletedNode.setValue(deletedNodeValue);
						
						deletedNode.replaceChildrenListBy(computeForestAlignment(i_p, 0, treeData1.degrees[i_p]-1, j, k, q));
						
						result.add(deletedNode);
						
						return result;
					}
					default:
						throw (new Error("TreeAlign: decision1 = " + decision1));
					}
				}
			}
		}
		
		/**
		 * Align T1[i] with the empty tree.
		 * @return the alignment
		 */
		private Tree<AlignedNode<ValueType1,ValueType2>> treeDeleted(int i) {
			Tree<AlignedNode<ValueType1,ValueType2>> root = new Tree<AlignedNode<ValueType1,ValueType2>>();
			AlignedNode<ValueType1,ValueType2> alignedNode = new AlignedNode<ValueType1,ValueType2>();
			alignedNode.setLeftNode(treeData1.nodes[i]);
			alignedNode.setRightNode(null);
			root.setValue(alignedNode);
			for (int r = 0; r<treeData1.degrees[i]; r++) {
				root.getChildren().add(treeDeleted(treeData1.children[i][r]));
			}
			return root;
		}
		
		/**
		 * Align the empty tree with T2[j].
		 * @return the alignment
		 */
		private Tree<AlignedNode<ValueType1,ValueType2>> treeInserted(int j) {
			Tree<AlignedNode<ValueType1,ValueType2>> root = new Tree<AlignedNode<ValueType1,ValueType2>>();
			AlignedNode<ValueType1,ValueType2> alignedNode = new AlignedNode<ValueType1,ValueType2>();
			alignedNode.setLeftNode(null);
			alignedNode.setRightNode(treeData2.nodes[j]);
			root.setValue(alignedNode);
			for (int r = 0; r<treeData2.degrees[j]; r++) {
				root.getChildren().add(treeInserted(treeData2.children[j][r]));
			}
			return root;
		}
		
		private Tree<AlignedNode<ValueType1,ValueType2>> computeTreeAlignment(int i, int j) {
			switch (DTDecisions1[i][j]) {
			case 1:
			{
				Tree<AlignedNode<ValueType1,ValueType2>> root = new Tree<AlignedNode<ValueType1,ValueType2>>();
				
				// Compute the value of the node
				AlignedNode<ValueType1,ValueType2> alignedNode = new AlignedNode<ValueType1,ValueType2>();
				alignedNode.setLeftNode(null);
				alignedNode.setRightNode(treeData2.nodes[j]);
				root.setValue(alignedNode);
				
				// Compute the children
				for (int r = 0; r<treeData2.degrees[j]; r++) {
					if (r == DTDecisions2[i][j]) {
						root.getChildren().add(computeTreeAlignment(i, treeData2.children[j][r]));
					} else {
						root.getChildren().add(treeInserted(treeData2.children[j][r]));
					}
				}
				return root;
			}
			case 2:
			{
				Tree<AlignedNode<ValueType1,ValueType2>> root = new Tree<AlignedNode<ValueType1,ValueType2>>();
				
				// Compute the value of the node
				AlignedNode<ValueType1,ValueType2> alignedNode = new AlignedNode<ValueType1,ValueType2>();
				alignedNode.setLeftNode(treeData1.nodes[i]);
				alignedNode.setRightNode(null);
				root.setValue(alignedNode);
				
				// Compute the children
				for (int r = 0; r<treeData1.degrees[i]; r++) {
					if (r == DTDecisions2[i][j]) {
						root.getChildren().add(computeTreeAlignment(treeData1.children[i][r], j));
					} else {
						root.getChildren().add(treeDeleted(treeData1.children[i][r]));
					}
				}
				return root;
			}
			case 3:
			{
				Tree<AlignedNode<ValueType1,ValueType2>> root = new Tree<AlignedNode<ValueType1,ValueType2>>();
				
				// Compute the value of the node
				AlignedNode<ValueType1,ValueType2> alignedNode = new AlignedNode<ValueType1,ValueType2>();
				alignedNode.setLeftNode(treeData1.nodes[i]);
				alignedNode.setRightNode(treeData2.nodes[j]);
				root.setValue(alignedNode);
				
				// Compute the children
				List<Tree<AlignedNode<ValueType1,ValueType2>>> children =
					computeForestAlignment(i, 0, treeData1.degrees[i]-1, j, 0, treeData2.degrees[j]-1);
				root.replaceChildrenListBy(children);
				
				return root;
			}
			default:
				throw (new Error("TreeAlign: DTDecisions1[i][j] = " + DTDecisions1[i][j]));
			}
		}
		
		public Tree<AlignedNode<ValueType1,ValueType2>> computeAlignment() {
			return computeTreeAlignment(treeData1.size-1, treeData2.size-1);
		}
		
	}

	
	/**
	 * Align T1 with T2, computing both the distance and the alignment.
	 * Time:  O(|T1|*|T2|*(deg(T1)+deg(T2))^2)
	 * Space: O(|T1|*|T2|*(deg(T1)+deg(T2)))
	 * Average (over possible trees) time: O(|T1|*|T2|)
	 * @param T1 The first tree.
	 * @param T2 The second tree.
	 * @return The distance and the alignment.
	 * @throws TreeAlignException 
	 */
	public TreeAlignResult<ValueType1, ValueType2> align(Tree<ValueType1> T1, Tree<ValueType2> T2) throws TreeAlignException {
		TreeAlignResult<ValueType1, ValueType2> result = new TreeAlignResult<ValueType1, ValueType2>();
		Aligner aligner = new Aligner(T1, T2);
		result.setDistance(aligner.align());
		result.setAlignment(aligner.computeAlignment());
		return result;
	}
	
	
	/**
	 * Takes a alignment, and compute the distance between the two 
	 * original trees. If you have called align(), the result object already
	 * contains the distance D and the alignment A. If you call
	 * distanceFromAlignment on the alignment A it will compute the distance D.
	 */
	public float distanceFromAlignment(Tree<AlignedNode<ValueType1,ValueType2>> alignment) {
		Tree<ValueType1> originalT1Node;
		Tree<ValueType2> originalT2Node;
		originalT1Node = alignment.getValue().getLeftNode();
		originalT2Node = alignment.getValue().getRightNode();
		float d = (float) labelDist.f(
				originalT1Node != null ? originalT1Node.getValue() : null,
				originalT2Node != null ? originalT2Node.getValue() : null);
		for (Tree<AlignedNode<ValueType1,ValueType2>> child: alignment.getChildren()) {
			d += distanceFromAlignment(child);
		}
		return d;
	}

	
}

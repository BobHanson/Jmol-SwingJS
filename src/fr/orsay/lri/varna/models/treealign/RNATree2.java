package fr.orsay.lri.varna.models.treealign;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import fr.orsay.lri.varna.exceptions.MappingException;
import fr.orsay.lri.varna.models.rna.Mapping;
import fr.orsay.lri.varna.models.rna.RNA;


/**
 * This class contains all functions that are specific to trees
 * (class Tree) of RNA, with RNANodeValue2.
 * 
 * @author Raphael Champeimont
 *
 */
public class RNATree2 {
	/**
	 * Convert an RNA object into a RNA tree with RNANodeValue2.
	 * @throws RNATree2Exception 
	 */
	public static Tree<RNANodeValue2> RNATree2FromRNA(RNA rna) throws RNATree2Exception {
		Tree<RNANodeValue> fullTree = RNATree.RNATreeFromRNA(rna);
		return RNATree2FromRNATree(fullTree);
	}

	/**
	 * Convert from RNANodeValue model to RNANodeValue2 model,
	 * ie. compact consecutive non-paired bases.
	 */
	public static Tree<RNANodeValue2> RNATree2FromRNATree(Tree<RNANodeValue> originalTree) throws RNATree2Exception {
		Tree<RNANodeValue2> newTree = new Tree<RNANodeValue2>();
		// Root in original tree is fake, so make a fake root
		newTree.setValue(null);
		newTree.replaceChildrenListBy(RNAForest2FromRNAForest(originalTree.getChildren()));
		return newTree;
	}
	
	private static void RNAForest2FromRNAForestCommitNonPaired(List<Tree<RNANodeValue2>> forest, List<RNANodeValue> consecutiveNonPairedBases) {
		// add the group of non-paired bases if there is one
		if (consecutiveNonPairedBases.size() > 0) {
			RNANodeValue2 groupOfConsecutiveBases = new RNANodeValue2(false);
			groupOfConsecutiveBases.getNodes().addAll(consecutiveNonPairedBases);
			Tree<RNANodeValue2> groupOfConsecutiveBasesNode = new Tree<RNANodeValue2>();
			groupOfConsecutiveBasesNode.setValue(groupOfConsecutiveBases);
			forest.add(groupOfConsecutiveBasesNode);
			consecutiveNonPairedBases.clear();
		}
	}
	
	private static List<Tree<RNANodeValue2>> RNAForest2FromRNAForest(List<Tree<RNANodeValue>> originalForest) throws RNATree2Exception {
		List<Tree<RNANodeValue2>> forest = new ArrayList<Tree<RNANodeValue2>>();
		List<RNANodeValue> consecutiveNonPairedBases = new LinkedList<RNANodeValue>();
		for (Tree<RNANodeValue> originalTree: originalForest) {
			if (originalTree.getValue().getRightBasePosition() == -1) {
				// non-paired base
				if (originalTree.getChildren().size() > 0) {
					throw (new RNATree2Exception("Non-paired base cannot have children."));
				}
				
				switch (originalTree.getValue().getOrigin()) {
				case BASE_FROM_HELIX_STRAND5:
				case BASE_FROM_HELIX_STRAND3:
					// This base is part of a broken base pair
					
					// if we have gathered some non-paired bases, add a node with
					// the group of them 
					RNAForest2FromRNAForestCommitNonPaired(forest, consecutiveNonPairedBases);
					
					// now add the node
					RNANodeValue2 pairedBase = new RNANodeValue2(true);
					pairedBase.setNode(originalTree.getValue());
					Tree<RNANodeValue2> pairedBaseNode = new Tree<RNANodeValue2>();
					pairedBaseNode.setValue(pairedBase);
					forest.add(pairedBaseNode);
					break;
				case BASE_FROM_UNPAIRED_REGION:
					consecutiveNonPairedBases.add(originalTree.getValue());
					break;
				case BASE_PAIR_FROM_HELIX:
					throw (new RNATree2Exception("Origin is BASE_PAIR_FROM_HELIX but this is not a pair."));
				}
			} else {
				// paired bases
				
				// if we have gathered some non-paired bases, add a node with
				// the group of them 
				RNAForest2FromRNAForestCommitNonPaired(forest, consecutiveNonPairedBases);
				
				// now add the node
				RNANodeValue2 pairedBase = new RNANodeValue2(true);
				pairedBase.setNode(originalTree.getValue());
				Tree<RNANodeValue2> pairedBaseNode = new Tree<RNANodeValue2>();
				pairedBaseNode.setValue(pairedBase);
				pairedBaseNode.replaceChildrenListBy(RNAForest2FromRNAForest(originalTree.getChildren()));
				forest.add(pairedBaseNode);
			}
		}
		
		// if there we have some non-paired bases, add them grouped
		RNAForest2FromRNAForestCommitNonPaired(forest, consecutiveNonPairedBases);
		
		return forest;
	}
	
	
	/**
	 * Convert an RNA tree (with RNANodeValue2) alignment into a Mapping.
	 */
	public static Mapping mappingFromAlignment(Tree<AlignedNode<RNANodeValue2,RNANodeValue2>> alignment) throws MappingException {
		ConvertToMapping converter = new ConvertToMapping();
		return converter.convert(alignment);
	}
	
	private static class ConvertToMapping {
		private Mapping m;
		ExampleDistance3 sequenceAligner = new ExampleDistance3();
		
		public Mapping convert(Tree<AlignedNode<RNANodeValue2,RNANodeValue2>> tree) throws MappingException {
			m = new Mapping();
			convertSubTree(tree);
			return m;
		}
		
		private void convertSubTree(Tree<AlignedNode<RNANodeValue2,RNANodeValue2>> tree) throws MappingException {
			AlignedNode<RNANodeValue2,RNANodeValue2> alignedNode = tree.getValue();
			Tree<RNANodeValue2> leftNode  = alignedNode.getLeftNode();
			Tree<RNANodeValue2> rightNode = alignedNode.getRightNode();
			if (leftNode != null && rightNode != null) {
				RNANodeValue2 v1 = leftNode.getValue();
				RNANodeValue2 v2 = rightNode.getValue();
				if (v1.isSingleNode() && v2.isSingleNode()) {
					// we have aligned (x,y) with (x',y')
					// so we map x with x' and y with y'
					RNANodeValue vsn1 = v1.getNode();
					RNANodeValue vsn2 = v2.getNode();
					int l1 = vsn1.getLeftBasePosition();
					int r1 = vsn1.getRightBasePosition();
					int l2 = vsn2.getLeftBasePosition();
					int r2 = vsn2.getRightBasePosition();
					if (l1 >= 0 && l2 >= 0) {
						m.addCouple(l1, l2);
					}
					if (r1 >= 0 && r2 >= 0) {
						m.addCouple(r1, r2);
					}
				} else if (!v1.isSingleNode() && !v2.isSingleNode()) {
					// We have aligned x1 x2 ... xn with y1 y2 ... ym.
					// So we will now (re-)compute this sequence alignment.
					int[][] sequenceAlignment = sequenceAligner.alignSequenceNodes(v1, v2).getAlignment();
					int l = sequenceAlignment[0].length;
					for (int i=0; i<l; i++) {
						// b1 and b2 are indexes in the aligned sequences
						int b1 = sequenceAlignment[0][i];
						int b2 = sequenceAlignment[1][i];
						if (b1 != -1 && b2 != -1) {
							int l1 = v1.getNodes().get(b1).getLeftBasePosition();
							int l2 = v2.getNodes().get(b2).getLeftBasePosition();
							m.addCouple(l1, l2);
						}
					}

				}
			}
			
			for (Tree<AlignedNode<RNANodeValue2,RNANodeValue2>> child: tree.getChildren()) {
				convertSubTree(child);
			}
		}
	}
}

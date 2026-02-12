package fr.orsay.lri.varna.models.treealign;

import java.util.ArrayList;
import java.util.List;

import fr.orsay.lri.varna.exceptions.MappingException;
import fr.orsay.lri.varna.models.rna.Mapping;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.ModeleBaseNucleotide;
import fr.orsay.lri.varna.models.rna.ModeleBP;
import fr.orsay.lri.varna.models.rna.RNA;



/**
 * This class contains all functions that are specific to trees
 * (class Tree) of RNA.
 * 
 * @author Raphael Champeimont
 *
 */
public class RNATree {	
	/**
	 * Convert an RNA object into a RNA tree.
	 * The root node will have a null value because it is a fake
	 * node added to have a tree (otherwise we would have a forest).
	 */
	public static Tree<RNANodeValue> RNATreeFromRNA(RNA rna) {
		ConvertToTree converter = new ConvertToTree(rna);
		return converter.toTreeAux(0);
	}
	
	private static class ConvertToTree {
		private RNA rna;
		
		private int i = 0;
		
		/** Starts at the current position i in the sequence and converts the sequence
		 *  to a tree.
		 * @return the created tree
		 */
		public Tree<RNANodeValue> toTreeAux(int depth) {
			Tree<RNANodeValue> tree = new Tree<RNANodeValue>();
			List<Tree<RNANodeValue>> children = tree.getChildren();
			// No value because it is a fake root
			tree.setValue(null);
			
			int length = rna.getSize();
			while (i < length) {
				ModeleBase base = rna.getBaseAt(i);
				int indexOfAssociatedBase = base.getElementStructure();
				if (indexOfAssociatedBase >= 0) {
					if (indexOfAssociatedBase > i) {
						// left parenthesis, we must analyze the children
						RNANodeValue childValue = new RNANodeValue();
						childValue.setLeftBasePosition(i);
						childValue.setRightBasePosition(indexOfAssociatedBase);
						childValue.setOrigin(RNANodeValue.Origin.BASE_PAIR_FROM_HELIX);
						
						if (base instanceof ModeleBaseNucleotide) {
							childValue.setLeftNucleotide(((ModeleBaseNucleotide) base).getBase());
							childValue.setRightNucleotide(((ModeleBaseNucleotide) rna.getBaseAt(indexOfAssociatedBase)).getBase());
						}
						i++;
						Tree<RNANodeValue> child = toTreeAux(depth+1);
						child.setValue(childValue);
						children.add(child);
					} else {
						// right parenthesis, we have finished analyzing the children
						i++;
						break;
					}
				} else {
					// we have a non-paired base
					Tree<RNANodeValue> child = new Tree<RNANodeValue>();
					RNANodeValue childValue = new RNANodeValue();
					childValue.setLeftBasePosition(i);
					if (base instanceof ModeleBaseNucleotide) {
						childValue.setLeftNucleotide(((ModeleBaseNucleotide) base).getBase());
					}
					
					// Even in this case (getElementStructure() < 0)
					// this base may still come from an helix which may have
					// been broken to remove a pseudoknot.
					childValue.setOrigin(RNANodeValue.Origin.BASE_FROM_UNPAIRED_REGION);
					ArrayList<ModeleBP> auxBasePairs = rna.getAuxBPs(i);
					for (ModeleBP auxBasePair: auxBasePairs) {
						if (auxBasePair.isCanonical()) {
							int partner5 = ((ModeleBaseNucleotide) auxBasePair.getPartner5()).getIndex();
							int partner3 = ((ModeleBaseNucleotide) auxBasePair.getPartner3()).getIndex();
							if (i == partner5) {
								childValue.setOrigin(RNANodeValue.Origin.BASE_FROM_HELIX_STRAND5);
							} else if (i == partner3) {
								childValue.setOrigin(RNANodeValue.Origin.BASE_FROM_HELIX_STRAND3);
							} else {
								System.err.println("Warning: Base index is " + i + " but neither endpoint matches it (edge endpoints are " + partner5 + " and " + partner3 + ").");
							}
							
						}
					}
					
					child.setValue(childValue);
					children.add(child);
					i++;
				}
			}
			
			return tree;
		}
		
		public ConvertToTree(RNA rna) {
			this.rna = rna;
		}
	}
	

	/**
	 * Convert an RNA tree alignment into a Mapping.
	 */
	public static Mapping mappingFromAlignment(Tree<AlignedNode<RNANodeValue,RNANodeValue>> alignment) throws MappingException {
		ConvertToMapping converter = new ConvertToMapping();
		return converter.convert(alignment);
	}
	
	private static class ConvertToMapping {
		private Mapping m;
		
		public Mapping convert(Tree<AlignedNode<RNANodeValue,RNANodeValue>> tree) throws MappingException {
			m = new Mapping();
			convertSubTree(tree);
			return m;
		}
		
		private void convertSubTree(Tree<AlignedNode<RNANodeValue,RNANodeValue>> tree) throws MappingException {
			AlignedNode<RNANodeValue,RNANodeValue> alignedNode = tree.getValue();
			Tree<RNANodeValue> leftNode  = alignedNode.getLeftNode();
			Tree<RNANodeValue> rightNode = alignedNode.getRightNode();
			if (leftNode != null && rightNode != null) {
				RNANodeValue v1 = leftNode.getValue();
				RNANodeValue v2 = rightNode.getValue();
				int l1 = v1.getLeftBasePosition();
				int r1 = v1.getRightBasePosition();
				int l2 = v2.getLeftBasePosition();
				int r2 = v2.getRightBasePosition();
				if (l1 >= 0 && l2 >= 0) {
					m.addCouple(l1, l2);
				}
				if (r1 >= 0 && r2 >= 0) {
					m.addCouple(r1, r2);
				}
			}
			for (Tree<AlignedNode<RNANodeValue,RNANodeValue>> child: tree.getChildren()) {
				convertSubTree(child);
			}
		}
	}
	
}

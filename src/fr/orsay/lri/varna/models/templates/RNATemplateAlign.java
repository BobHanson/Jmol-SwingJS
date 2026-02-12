package fr.orsay.lri.varna.models.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import fr.orsay.lri.varna.exceptions.ExceptionInvalidRNATemplate;
import fr.orsay.lri.varna.models.rna.ModeleBaseNucleotide;
import fr.orsay.lri.varna.models.rna.ModeleBP;
import fr.orsay.lri.varna.models.rna.RNA;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateElement;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateHelix;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateUnpairedSequence;
import fr.orsay.lri.varna.models.treealign.AlignedNode;
import fr.orsay.lri.varna.models.treealign.RNANodeValue;
import fr.orsay.lri.varna.models.treealign.RNANodeValue2;
import fr.orsay.lri.varna.models.treealign.RNATree2;
import fr.orsay.lri.varna.models.treealign.RNATree2Exception;
import fr.orsay.lri.varna.models.treealign.Tree;
import fr.orsay.lri.varna.models.treealign.TreeAlign;
import fr.orsay.lri.varna.models.treealign.TreeAlignException;
import fr.orsay.lri.varna.models.treealign.TreeAlignResult;

/**
 * This class is about the alignment between a tree of RNANodeValue2
 * and a tree of RNANodeValueTemplate.
 * 
 * @author Raphael Champeimont
 */
public class RNATemplateAlign {
	
	// We check this node can be part of a non-broken helix.
	private static boolean canBePartOfAnHelix(RNANodeValue2 leftNodeValue) {
		return (leftNodeValue != null) && leftNodeValue.isSingleNode() && leftNodeValue.getNode().getRightBasePosition() > 0;
	}
	
	private static boolean canBePartOfASequence(RNANodeValue2 leftNodeValue) {
		return (leftNodeValue != null) && !leftNodeValue.isSingleNode();
	}
	
	private static boolean canBePartOfABrokenHelix(RNANodeValue2 leftNodeValue) {
		return (leftNodeValue != null) && leftNodeValue.isSingleNode() && leftNodeValue.getNode().getRightBasePosition() < 0;
	}
	
	
	/**
	 * This method takes an alignment between a tree of RNANodeValue2
	 * of RNANodeValue and a tree of RNANodeValue2 of RNANodeValueTemplate,
	 * and the original RNA object that was used to create the first tree
	 * in the alignment.
	 * It returns the corresponding RNATemplateMapping.
	 */
	public static RNATemplateMapping makeTemplateMapping(TreeAlignResult<RNANodeValue2,RNANodeValueTemplate> alignResult, RNA rna) throws RNATemplateMappingException {
		RNATemplateMapping mapping = new RNATemplateMapping();
		Tree<AlignedNode<RNANodeValue2,RNANodeValueTemplate>> alignment = alignResult.getAlignment();
		mapping.setDistance(alignResult.getDistance());
		
		// Map sequences and helices together, without managing pseudoknots
		{
			// We will go through the tree using a DFS
			// The reason why this algorithm is not trivial is that we may have
			// a longer helix on the RNA side than on the template side, in which
			// case some nodes on the RNA side are going to be alone while we
			// would want them to be part of the helix.
			RNATemplateHelix currentHelix = null;
			LinkedList<Tree<AlignedNode<RNANodeValue2,RNANodeValueTemplate>>> remainingNodes = new LinkedList<Tree<AlignedNode<RNANodeValue2,RNANodeValueTemplate>>>();
			List<RNANodeValue2> nodesInSameHelix = new LinkedList<RNANodeValue2>();
			remainingNodes.add(alignment);
			while (!remainingNodes.isEmpty()) {
				Tree<AlignedNode<RNANodeValue2,RNANodeValueTemplate>> node = remainingNodes.getLast();
				remainingNodes.removeLast();
				
				Tree<RNANodeValue2> leftNode = node.getValue().getLeftNode();
				Tree<RNANodeValueTemplate> rightNode = node.getValue().getRightNode();
				
				// Do we have something on RNA side?
				if (leftNode != null && leftNode.getValue() != null) {
					RNANodeValue2 leftNodeValue = leftNode.getValue();
					
					// Do we have something on template side?
					if (rightNode != null && rightNode.getValue() != null) {
						RNANodeValueTemplate rightNodeValue = rightNode.getValue();
						
						if (rightNodeValue instanceof RNANodeValueTemplateBasePair
								&& canBePartOfAnHelix(leftNodeValue)) {
							RNATemplateHelix helix = ((RNANodeValueTemplateBasePair) rightNodeValue).getHelix();
							currentHelix = helix;
							int i = leftNodeValue.getNode().getLeftBasePosition();
							int j = leftNodeValue.getNode().getRightBasePosition();
							mapping.addCouple(i, helix);
							mapping.addCouple(j, helix);
							
							// Maybe we have marked nodes as part of the same helix
							// when we didn't know yet which helix it was.
							if (nodesInSameHelix.size() > 0) {
								for (RNANodeValue2 v: nodesInSameHelix) {
									int k = v.getNode().getLeftBasePosition();
									int l = v.getNode().getRightBasePosition();
									// We want to check nodesInSameHelix is a parent helix and not a sibling.
									boolean validExtension = (k < i) && (j < l);
									if (validExtension) {
										mapping.addCouple(v.getNode().getLeftBasePosition(), helix);
										mapping.addCouple(v.getNode().getRightBasePosition(), helix);
									}	
								}
							}
							nodesInSameHelix.clear();
							
						} else if (rightNodeValue instanceof RNANodeValueTemplateSequence
								&& canBePartOfASequence(leftNodeValue)) {
							currentHelix = null;
							nodesInSameHelix.clear();
							RNATemplateUnpairedSequence sequence = ((RNANodeValueTemplateSequence) rightNodeValue).getSequence();
							for (RNANodeValue nodeValue: leftNode.getValue().getNodes()) {
								mapping.addCouple(nodeValue.getLeftBasePosition(), sequence);
							}
						} else {
							// Pseudoknot in template
							currentHelix = null;
							nodesInSameHelix.clear();
						}
						
					} else {
						// We have nothing on template side
						
						if (canBePartOfAnHelix(leftNodeValue)) {
							if (currentHelix != null) {
								// We may be in this case when the RNA
								// contains a longer helix than in the template	
								int i = leftNodeValue.getNode().getLeftBasePosition();
								int j = leftNodeValue.getNode().getRightBasePosition();
								int k = Integer.MAX_VALUE;
								int l = Integer.MIN_VALUE;
								for (int b: mapping.getAncestor(currentHelix)) {
									k = Math.min(k, b);
									l = Math.max(l, b);
								}
								// We want to check currentHelix is a parent helix and not a sibling.
								boolean validExtension = (k < i) && (j < l);
								if (validExtension) {
									mapping.addCouple(i, currentHelix);
									mapping.addCouple(j, currentHelix);
								}	
							} else {
								// Maybe this left node is part of an helix
								// which is smaller in the template
								nodesInSameHelix.add(leftNodeValue);
							}
						}
					}
				}
				
				
				// If this node has children, add them in the stack
				List<Tree<AlignedNode<RNANodeValue2,RNANodeValueTemplate>>> children = node.getChildren();
				int n = children.size();
				if (n > 0) {
					int helixChildren = 0;
					// For each subtree, we want the sequences (in RNA side) to be treated first
					// and then the helix nodes, because finding an aligned sequence tells
					// us we cannot grow a current helix
					ArrayList<Tree<AlignedNode<RNANodeValue2,RNANodeValueTemplate>>> addToStack1 = new ArrayList<Tree<AlignedNode<RNANodeValue2,RNANodeValueTemplate>>>();
					ArrayList<Tree<AlignedNode<RNANodeValue2,RNANodeValueTemplate>>> addToStack2 = new ArrayList<Tree<AlignedNode<RNANodeValue2,RNANodeValueTemplate>>>();
					for (int i=0; i<n; i++) {
						Tree<AlignedNode<RNANodeValue2,RNANodeValueTemplate>> child = children.get(i);
						Tree<RNANodeValue2> RNAchild = child.getValue().getLeftNode();
						if (RNAchild != null
								&& RNAchild.getValue() != null
								&& (canBePartOfAnHelix(RNAchild.getValue()) || canBePartOfABrokenHelix(RNAchild.getValue()) )) {
							helixChildren++;
							addToStack2.add(child);
						} else {
							addToStack1.add(child);
						}
					}
					// We add the children in their reverse order so they
					// are given in the original order by the iterator
					for (int i=addToStack2.size()-1; i>=0; i--) {
						remainingNodes.add(addToStack2.get(i));
					}
					for (int i=addToStack1.size()-1; i>=0; i--) {
						remainingNodes.add(addToStack1.get(i));
					}
					if (helixChildren >= 2) {
						// We cannot "grow" the current helix, because we have a multiloop
						// in the RNA.
						currentHelix = null;
						//nodesInSameHelix.clear();
					}
				}
			}
		}
		
		
		// Now recover pseudoknots (broken helices)
		{
			// First create a temporary mapping with broken helices
			LinkedList<Tree<AlignedNode<RNANodeValue2,RNANodeValueTemplate>>> remainingNodes = new LinkedList<Tree<AlignedNode<RNANodeValue2,RNANodeValueTemplate>>>();
			remainingNodes.add(alignment);
			RNATemplateMapping tempPKMapping = new RNATemplateMapping();
			while (!remainingNodes.isEmpty()) {
				Tree<AlignedNode<RNANodeValue2,RNANodeValueTemplate>> node = remainingNodes.getLast();
				remainingNodes.removeLast();
				List<Tree<AlignedNode<RNANodeValue2,RNANodeValueTemplate>>> children = node.getChildren();
				int n = children.size();
				if (n > 0) {
					for (int i=n-1; i>=0; i--) {
						// We add the children in their reverse order so they
						// are given in the original order by the iterator
						remainingNodes.add(children.get(i));
					}
					List<RNANodeValue2> nodesInSameHelix = new LinkedList<RNANodeValue2>();
					RNATemplateHelix currentHelix = null;
					for (Tree<AlignedNode<RNANodeValue2,RNANodeValueTemplate>> child: node.getChildren()) {
						Tree<RNANodeValue2> leftNode = child.getValue().getLeftNode();
						Tree<RNANodeValueTemplate> rightNode = child.getValue().getRightNode();
						
						if (leftNode != null && leftNode.getValue() != null) {
							RNANodeValue2 leftNodeValue = leftNode.getValue();
							// We have a real left (RNA side) node
							
							if (rightNode != null && rightNode.getValue() != null) {
								// We have a real right (template side) node
								RNANodeValueTemplate rightNodeValue = rightNode.getValue();
								
								if (rightNodeValue instanceof RNANodeValueTemplateBrokenBasePair
										&& canBePartOfABrokenHelix(leftNodeValue)) {
									RNATemplateHelix helix = ((RNANodeValueTemplateBrokenBasePair) rightNodeValue).getHelix();
									currentHelix = helix;
									tempPKMapping.addCouple(leftNodeValue.getNode().getLeftBasePosition(), helix);
									
									// Maybe we have marked nodes as part of the same helix
									// when we didn't know yet which helix it was.
									for (RNANodeValue2 v: nodesInSameHelix) {
										tempPKMapping.addCouple(v.getNode().getLeftBasePosition(), helix);
									}
									nodesInSameHelix.clear();
								} else {
									currentHelix = null;
									nodesInSameHelix.clear();
								}
							} else {
								// We have no right (template side) node
								if (canBePartOfABrokenHelix(leftNodeValue)) {
									if (currentHelix != null) {
										// We may be in this case if the RNA sequence
										// contains a longer helix than in the template
										tempPKMapping.addCouple(leftNodeValue.getNode().getLeftBasePosition(), currentHelix);
									} else {
										// Maybe this left node is part of an helix
										// which is smaller in the template
										nodesInSameHelix.add(leftNodeValue);
									}
								} else {
									currentHelix = null;
									nodesInSameHelix.clear();
								}
							}
						} else {
							currentHelix = null;
							nodesInSameHelix.clear();
						}
					}
				}
			}
			
			
			// As parts of broken helices were aligned independently,
			// we need to check for consistency, ie. keep only bases for
			// which the associated base is also aligned with the same helix.
			for (RNATemplateElement element: tempPKMapping.getTargetElemsAsSet()) {
				RNATemplateHelix helix = (RNATemplateHelix) element;
				HashSet<Integer> basesInHelix = new HashSet<Integer>(tempPKMapping.getAncestor(helix));
				for (int baseIndex: basesInHelix) {
					System.out.println("PK: " + helix + " aligned with " + baseIndex);
					boolean baseOK = false;
					// Search for an associated base aligned with the same helix
					ArrayList<ModeleBP> auxBasePairs = rna.getAuxBPs(baseIndex);
					for (ModeleBP auxBasePair: auxBasePairs) {
						int partner5 = ((ModeleBaseNucleotide) auxBasePair.getPartner5()).getIndex();
						int partner3 = ((ModeleBaseNucleotide) auxBasePair.getPartner3()).getIndex();
						if (baseIndex == partner5) {
							if (basesInHelix.contains(partner3)) {
								baseOK = true;
								break;
							}
						} else if (baseIndex == partner3) {
							if (basesInHelix.contains(partner5)) {
								baseOK = true;
								break;
							}
						}
					}
					if (baseOK) {
						// Add it to the real mapping
						mapping.addCouple(baseIndex, helix);
					}
				}
			}
		}
		
		
		return mapping;
	}
	
	
	
	public static void printMapping(RNATemplateMapping mapping, RNATemplate template, String sequence) {
		Iterator<RNATemplateElement> iter = template.rnaIterator();
		while (iter.hasNext()) {
			RNATemplateElement element = iter.next();
			System.out.println(element.toString());
			ArrayList<Integer> A = mapping.getAncestor(element);
			if (A != null) {
				RNATemplateAlign.printIntArrayList(A);
				for (int n=A.size(), i=0; i<n; i++) {
					System.out.print("\t" + sequence.charAt(A.get(i)));
				}
				System.out.println("");
			} else {
				System.out.println("\tno match");
			}
		}
	}
	
	
	/**
	 * Align the given RNA with the given RNA template.
	 */
	public static TreeAlignResult<RNANodeValue2,RNANodeValueTemplate> alignRNAWithTemplate(RNA rna, RNATemplate template) throws RNATemplateDrawingAlgorithmException {
		try {
			Tree<RNANodeValue2> rnaAsTree = RNATree2.RNATree2FromRNA(rna);
			Tree<RNANodeValueTemplate> templateAsTree = template.toTree();
			TreeAlign<RNANodeValue2,RNANodeValueTemplate> treeAlign = new TreeAlign<RNANodeValue2,RNANodeValueTemplate>(new RNANodeValue2TemplateDistance());
			TreeAlignResult<RNANodeValue2,RNANodeValueTemplate> result = treeAlign.align(rnaAsTree, templateAsTree);
			return result;
		} catch (RNATree2Exception e) {
			throw (new RNATemplateDrawingAlgorithmException("RNATree2Exception: " + e.getMessage()));
		} catch (ExceptionInvalidRNATemplate e) {
			throw (new RNATemplateDrawingAlgorithmException("ExceptionInvalidRNATemplate: " + e.getMessage()));
		} catch (TreeAlignException e) {
			throw (new RNATemplateDrawingAlgorithmException("TreeAlignException: " + e.getMessage()));
		}
	}
	
	/**
	 * Map an RNA with an RNATemplate using tree alignment.
	 */
	public static RNATemplateMapping mapRNAWithTemplate(RNA rna, RNATemplate template) throws RNATemplateDrawingAlgorithmException {
		try {
			TreeAlignResult<RNANodeValue2,RNANodeValueTemplate> alignResult = RNATemplateAlign.alignRNAWithTemplate(rna, template);	
			RNATemplateMapping mapping = RNATemplateAlign.makeTemplateMapping(alignResult, rna);
			return mapping;
		} catch (RNATemplateMappingException e) {
			e.printStackTrace();
			throw (new RNATemplateDrawingAlgorithmException("RNATemplateMappingException: " + e.getMessage()));
		}
	}
	
	

	/**
	 * Print an integer array.
	 */
	public static void printIntArray(int[] A) {
		for (int i=0; i<A.length; i++) {
			System.out.print("\t" + A[i]);
		}
		System.out.println("");
	}

	/**
	 * Print an integer ArrayList.
	 */
	public static void printIntArrayList(ArrayList<Integer> A) {
		for (int i=0; i<A.size(); i++) {
			System.out.print("\t" + A.get(i));
		}
		System.out.println("");
	}

	/**
	 * Print an matrix of shorts.
	 */
	public static void printShortMatrix(short[][] M) {
		System.out.println("Begin matrix");
		for (int i=0; i<M.length; i++) {
			for (int j=0; j<M[i].length; j++) {
				System.out.print("\t" + M[i][j]);
			}
			System.out.println("");
		}
		System.out.println("End matrix");
	}
	
	/**
	 * Convert a list of integers into an array of integers.
	 * The returned arrays is freshly allocated.
	 * Returns null if given null.
	 */
	public static int[] intArrayFromList(List<Integer> l) {
		if (l != null) {
			int n = l.size();
			int[] result = new int[n];
			for (int i=0; i<n; i++) {
				result[i] = l.get(i);
			}
			return result;
		} else {
			return null;
		}
	}
	
}

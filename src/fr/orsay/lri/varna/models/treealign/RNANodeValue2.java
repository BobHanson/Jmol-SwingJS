package fr.orsay.lri.varna.models.treealign;
import java.util.ArrayList;
import java.util.List;



/**
 * In this model, nodes are either:
 * 1. a couple of paired bases, and in that case they may have children,
 *    in this case singleNode is true 
 * 2. a single base that comes from a broken base pair
 *    (broken during planarization), without children,
 *    in this case singleNode is true
 * 3. a list of consecutive non-paired bases, without children.
 *    in this case singleNode is false
 * Note that case 2 happens only if original sequences contained
 * pseudoknots, otherwise this case can be ignored.
 * 
 * @author Raphael Champeimont
 *
 */
public class RNANodeValue2 implements GraphvizDrawableNodeValue {
	/**
	 * Says whether we have a single node or a list of nodes.
	 */
	private boolean singleNode = true;
	
	/**
	 * Defined if singleNode is true. 
	 */
	private RNANodeValue node;
	
	/**
	 * Defined if singleNode is false; 
	 */
	private List<RNANodeValue> nodes;
	
	public RNANodeValue2(boolean singleNode) {
		this.singleNode = singleNode;
		if (singleNode) {
			node = new RNANodeValue();
		} else {
			nodes = new ArrayList<RNANodeValue>();
		}
	}

	/**
	 * In case of a single node, return it.
	 * Will throw RNANodeValue2WrongTypeException if singleNode = false.
	 */
	public RNANodeValue getNode() {
		if (singleNode) {
			return node;
		} else {
			throw (new RNANodeValue2WrongTypeException());
		}
	}
	
	public void setNode(RNANodeValue node) {
		if (singleNode) {
			this.node = node;
		} else {
			throw (new RNANodeValue2WrongTypeException());
		}
	}

	/**
	 * In case of multiple nodes, return them.
	 * Will throw RNANodeValue2WrongTypeException if singleNode = true.
	 */
	public List<RNANodeValue> getNodes() {
		if (!singleNode) {
			return nodes;
		} else {
			throw (new RNANodeValue2WrongTypeException());
		}
	}
	/**
	 * In case of multiple nodes, return the sequence of nucleotides.
	 */
	public char[] computeSequence() {
		if (!singleNode) {
			final int n = nodes.size();
			char[] sequence = new char[n];
			for (int i=0; i<n; i++) {
				sequence[i] = nodes.get(i).getLeftNucleotide().charAt(0);
			}
			return sequence;
		} else {
			throw (new RNANodeValue2WrongTypeException());
		}
	}
	
	public void setNodes(List<RNANodeValue> nodes) {
		if (!singleNode) {
			this.nodes = nodes;
		} else {
			throw (new RNANodeValue2WrongTypeException());
		}
	}

	public boolean isSingleNode() {
		return singleNode;
	}
	
	public String toString() {
		if (singleNode) {
			return node.toString();
		} else {
			String s = "";
			for (RNANodeValue node: nodes) {
				if (s != "") {
					s += " ";
				}
				s += node.toString();
			}
			return s;
		}
	}
	
	public String toGraphvizNodeName() {
		if (singleNode) {
			return node.toGraphvizNodeName();
		} else {
			String s = "";
			for (RNANodeValue node: nodes) {
				if (s != "") {
					s += " ";
				}
				s += node.toGraphvizNodeName();
			}
			return s;
		}
	}

}

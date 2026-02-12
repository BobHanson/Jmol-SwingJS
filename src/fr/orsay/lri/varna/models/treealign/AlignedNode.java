package fr.orsay.lri.varna.models.treealign;


/**
 * The type of node values in an alignment.
 * Contains a reference to both original nodes.
 * This class implements GraphvizDrawableNodeValue but it will only work
 * if the original nodes implement it.
 * @author Raphael Champeimont
 * @param <OriginalNodeValueType1> The type of values in the original first tree.
 * @param <OriginalNodeValueType2> The type of values in the original second tree.
 */
public class AlignedNode<OriginalNodeValueType1, OriginalNodeValueType2> implements GraphvizDrawableNodeValue {
	private Tree<OriginalNodeValueType1> leftNode;
	private Tree<OriginalNodeValueType2> rightNode;

	public Tree<OriginalNodeValueType1> getLeftNode() {
		return leftNode;
	}

	public void setLeftNode(Tree<OriginalNodeValueType1> leftNode) {
		this.leftNode = leftNode;
	}

	public Tree<OriginalNodeValueType2> getRightNode() {
		return rightNode;
	}

	public void setRightNode(Tree<OriginalNodeValueType2> rightNode) {
		this.rightNode = rightNode;
	}
	
	private String maybeNodeToGraphvizNodeName(Tree <? extends GraphvizDrawableNodeValue> tree) {
		return (tree != null && tree.getValue() != null) ? tree.getValue().toGraphvizNodeName() : "_";
	}

	/**
	 * This method will work only if the left and right node
	 * already implement GraphvizDrawableNodeValue.
	 */
	@SuppressWarnings("unchecked")
	public String toGraphvizNodeName() {
		return "(" + maybeNodeToGraphvizNodeName((Tree) leftNode)
		+ "," + maybeNodeToGraphvizNodeName((Tree) rightNode) + ")";
	}


}

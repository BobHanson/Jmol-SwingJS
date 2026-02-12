package fr.orsay.lri.varna.models.treealign;


/**
 * The result of aligning a tree T1 with a tree T2.
 * On the resulting tree, each node has a value
 * of type AlignedNode<original value type>.
 * @author Raphael Champeimont
 */
public class TreeAlignResult<ValueType1, ValueType2> {
	private Tree<AlignedNode<ValueType1, ValueType2>> alignment;
	private double distance;
	
	public Tree<AlignedNode<ValueType1, ValueType2>> getAlignment() {
		return alignment;
	}
	public void setAlignment(Tree<AlignedNode<ValueType1, ValueType2>> alignment) {
		this.alignment = alignment;
	}
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}

}

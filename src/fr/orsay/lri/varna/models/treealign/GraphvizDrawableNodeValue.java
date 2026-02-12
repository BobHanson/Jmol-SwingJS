package fr.orsay.lri.varna.models.treealign;

/**
 * A tree can be displayed using graphviz (using class TreeGraphviz)
 * if the node values in the tree implement this interface.
 * 
 * @author Raphael Champeimont
 */
public interface GraphvizDrawableNodeValue {
	/**
	 * Returns a string that will be displayed on the node by graphviz.
	 */
	public String toGraphvizNodeName();
}

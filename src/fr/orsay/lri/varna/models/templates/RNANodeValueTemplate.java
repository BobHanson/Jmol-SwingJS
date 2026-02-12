package fr.orsay.lri.varna.models.templates;

import fr.orsay.lri.varna.models.treealign.GraphvizDrawableNodeValue;


/**
 * An node from an RNA template is either a sequence of non-paired bases,
 * a base pair originally belonging to an helix,
 * or a single base originally belonging to an helix but which was broken
 * in order to remove pseudoknots.
 * 
 * @author Raphael Champeimont
 */
public abstract class RNANodeValueTemplate implements GraphvizDrawableNodeValue {

	public String toString() {
		return toGraphvizNodeName();
	}
	
	public abstract String toGraphvizNodeName();
	

}

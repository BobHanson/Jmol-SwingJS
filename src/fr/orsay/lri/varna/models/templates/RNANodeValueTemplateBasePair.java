package fr.orsay.lri.varna.models.templates;

import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateHelix;


/**
 * See RNANodeValueTemplate.
 * 
 * @author Raphael Champeimont
 */
public class RNANodeValueTemplateBasePair extends RNANodeValueTemplate {
	
	/**
	 * The original template element this node came from.
	 */
	private RNATemplateHelix helix;

	/**
	 * The position (in the 5' to 3' order)
	 * of this base pair in the original helix.
	 */
	private int positionInHelix;
	
	
	
	public String toGraphvizNodeName() {
		return "H[" + positionInHelix + "]";
	}
	
	
	
	public RNATemplateHelix getHelix() {
		return helix;
	}

	public void setHelix(RNATemplateHelix helix) {
		this.helix = helix;
	}

	public int getPositionInHelix() {
		return positionInHelix;
	}

	public void setPositionInHelix(int positionInHelix) {
		this.positionInHelix = positionInHelix;
	}
	
}

package fr.orsay.lri.varna.models.templates;

import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateUnpairedSequence;


/**
 * See RNANodeValueTemplate.
 * 
 * @author Raphael Champeimont
 */
public class RNANodeValueTemplateSequence extends RNANodeValueTemplate {

	/**
	 * The sequence this node came from.
	 */
	private RNATemplateUnpairedSequence sequence;
	

	
	public String toGraphvizNodeName() {
		return "S(len=" + sequence.getLength() + ")";
	}
	
	
	

	public RNATemplateUnpairedSequence getSequence() {
		return sequence;
	}

	public void setSequence(RNATemplateUnpairedSequence sequence) {
		this.sequence = sequence;
	}
}

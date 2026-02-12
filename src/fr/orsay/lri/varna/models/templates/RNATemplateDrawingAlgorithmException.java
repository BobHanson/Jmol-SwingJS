package fr.orsay.lri.varna.models.templates;

import fr.orsay.lri.varna.exceptions.ExceptionDrawingAlgorithm;

/**
 * Exception thrown in case of failure of the template-based
 * RNA drawing algorithm.
 * 
 * @author Raphael Champeimont
 */
public class RNATemplateDrawingAlgorithmException extends ExceptionDrawingAlgorithm {

	private static final long serialVersionUID = 1701307036024533400L;

	public RNATemplateDrawingAlgorithmException(String message) {
		super(message);
	}
}

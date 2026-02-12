package fr.orsay.lri.varna.models.templates;

/**
 * This exception is thrown when we discover that a template is invalid
 * (it contains impossible connections between elements).
 * 
 * @author Raphael Champeimont
 */
public class RNATemplateMappingException extends Exception {
	
	private static final long serialVersionUID = -201638492590138354L;

	public RNATemplateMappingException(String message) {
		super(message);
	}
	
	public RNATemplateMappingException() {
	}
}

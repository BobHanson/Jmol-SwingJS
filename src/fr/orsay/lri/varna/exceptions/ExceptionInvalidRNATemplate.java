package fr.orsay.lri.varna.exceptions;

/**
 * This exception is thrown when we discover that a template is invalid
 * (it contains impossible connections between elements).
 * 
 * @author Raphael Champeimont
 */
public class ExceptionInvalidRNATemplate extends Exception {
	private static final long serialVersionUID = 3866618355319087333L;
	
	public ExceptionInvalidRNATemplate(String message) {
		super(message);
	}
	
	public ExceptionInvalidRNATemplate() {
	}
}

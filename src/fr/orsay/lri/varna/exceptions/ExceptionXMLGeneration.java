package fr.orsay.lri.varna.exceptions;

/**
 * Thrown by XML-generating algorithms when they fail.
 * 
 * @author Raphael Champeimont
 */
public class ExceptionXMLGeneration extends Exception {
	private static final long serialVersionUID = 8867910395701431387L;

	public ExceptionXMLGeneration(String message) {
		super(message);
	}
	
	public ExceptionXMLGeneration() {
	}
}

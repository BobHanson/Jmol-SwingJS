package fr.orsay.lri.varna.exceptions;

/**
 * Exceptions of this class, or of a derived class,
 * are thrown by the RNA drawing algorithms.
 * 
 * @author Raphael Champeimont
 */
public class ExceptionDrawingAlgorithm extends Exception {
	private static final long serialVersionUID = -8705033963886770829L;

	public ExceptionDrawingAlgorithm(String message) {
		super(message);
	}
	
	public ExceptionDrawingAlgorithm() {
	}
}

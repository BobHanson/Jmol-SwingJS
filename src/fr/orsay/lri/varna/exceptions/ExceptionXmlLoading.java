package fr.orsay.lri.varna.exceptions;

/**
 * Thrown by algorithms that load data from XML when they fail.
 * 
 * @author Raphael Champeimont
 */
public class ExceptionXmlLoading extends Exception {
	private static final long serialVersionUID = -6267373620339074008L;

	public ExceptionXmlLoading(String message) {
		super(message);
	}
	
	public ExceptionXmlLoading() {
	}
}
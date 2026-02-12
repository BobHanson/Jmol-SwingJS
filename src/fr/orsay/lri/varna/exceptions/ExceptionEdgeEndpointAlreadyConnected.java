package fr.orsay.lri.varna.exceptions;


/**
 * Thrown by an EdgeEndPoint when it is already connected and you try
 * to connect it.
 * 
 * @author Raphael Champeimont
 */
public class ExceptionEdgeEndpointAlreadyConnected extends ExceptionInvalidRNATemplate {
	private static final long serialVersionUID = 3978166870034913842L;

	public ExceptionEdgeEndpointAlreadyConnected(String message) {
		super(message);
	}
	
	public ExceptionEdgeEndpointAlreadyConnected() {
		super("Edge endpoint is already connected");
	}
	
}

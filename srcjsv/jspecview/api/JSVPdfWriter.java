package jspecview.api;

import java.io.OutputStream;

import jspecview.common.PrintLayout;

public interface JSVPdfWriter {
	
	void createPdfDocument(JSVPanel panel, PrintLayout pl, OutputStream os);

}

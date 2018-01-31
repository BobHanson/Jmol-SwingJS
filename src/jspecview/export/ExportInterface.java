package jspecview.export;

import javajs.util.Lst;
import jspecview.api.JSVPanel;
import jspecview.common.Spectrum;
import jspecview.common.JSViewer;


public interface ExportInterface {

	/**
	 * from EXPORT command
	 * @param jsvp 
	 * @param tokens
	 * @param forInkscape 
	 * 
	 * @return message for status line
	 */
	String exportCmd(JSVPanel jsvp, Lst<String> tokens,
			boolean forInkscape);

	void exportSpectrum(JSViewer viewer, String type);

	/**
	 * returns message if path is not null, otherwise full string of text (unsigned applet)
	 * @param type 
	 * @param path
	 * @param spec
	 * @param startIndex
	 * @param endIndex
	 * @return message or text
	 * @throws Exception
	 */
	String exportTheSpectrum(String type, String path,
			Spectrum spec, int startIndex, int endIndex) throws Exception;

	String printPDF(JSViewer viewer, String pdfFileName);

}
package jspecview.api;

import javajs.util.Lst;
import jspecview.common.JSViewer;


public interface ExportInterface extends JSVExporter {

	/**
	 * from EXPORT command
	 * 
	 * @param viewer
	 * @param tokens
	 * @param forInkscape 
	 * @return message for status line
	 */
	String write(JSViewer viewer, Lst<String> tokens,
			boolean forInkscape);

}
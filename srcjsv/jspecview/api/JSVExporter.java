package jspecview.api;

import javajs.util.OC;

import jspecview.common.ExportType;
import jspecview.common.Spectrum;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;

public interface JSVExporter {

	/**
	 * 
	 * @param viewer
	 * @param type 
	 * @param out
	 * @param spec  not relevant for PDF, JPG, PNG
	 * @param startIndex  not relevant for PDF, JPG, PNG
	 * @param endIndex  not relevant for PDF, JPG, PNG
	 * @param pd only for SVG/SVGI
	 * @param asBase64 TODO
	 * @return message or text
	 * @throws Exception
	 */
	String exportTheSpectrum(JSViewer viewer, ExportType type,
			OC out, Spectrum spec, int startIndex, int endIndex, PanelData pd, boolean asBase64) throws Exception;

}

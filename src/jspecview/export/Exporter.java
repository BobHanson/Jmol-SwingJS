package jspecview.export;

import java.io.BufferedReader;

import org.jmol.api.GenericFileInterface;

import javajs.util.OC;
import javajs.util.Lst;
import javajs.util.PT;


import jspecview.api.ExportInterface;
import jspecview.api.JSVExporter;
import jspecview.api.JSVFileHelper;
import jspecview.api.JSVPanel;
import jspecview.common.ExportType;
import jspecview.common.Spectrum;
import jspecview.common.JSVFileManager;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;
import jspecview.common.PrintLayout;
import jspecview.common.Annotation.AType;

public class Exporter implements ExportInterface {

	static final String newLine = System.getProperty("line.separator");

	public Exporter() {
		// for reflection; called directly only from MainFrame
	}

	@Override
	public String write(JSViewer viewer, Lst<String> tokens, boolean forInkscape) {
		// MainFrame or applet WRITE command
		if (tokens == null)
			return printPDF(viewer, null, false);
		
		String type = null;
		String fileName = null;
		ExportType eType;
		OC out;
		JSVPanel jsvp = viewer.selectedPanel;
		try {
			switch (tokens.size()) {
			default:
				return "WRITE what?";
			case 1:
				fileName = PT.trimQuotes(tokens.get(0));
				if (fileName.indexOf(".") >= 0)
					type = "XY";
				if (jsvp == null)
					return null;
				eType = ExportType.getType(fileName);
				switch (eType) {
				case PDF:
				case PNG:
				case JPG:
					return exportTheSpectrum(viewer, eType, null, null, -1, -1, null, false);
				default:
					// select a spectrum
					viewer.fileHelper.setFileChooser(eType);
					String[] items = getExportableItems(viewer, eType.equals(ExportType.SOURCE));
					int index = (items == null ? -1 : viewer.getOptionFromDialog(items, "Export", "Choose a spectrum to export"));
					if (index == Integer.MIN_VALUE)
						return null;
					GenericFileInterface file = viewer.fileHelper.getFile(getSuggestedFileName(viewer, eType), jsvp, true);
					if (file == null)
						return null;
					out = viewer.getOutputChannel(file.getFullPath(), false);
			    String msg = exportSpectrumOrImage(viewer, eType, index, out);
			    boolean isOK = msg.startsWith("OK");
			    if (isOK)
			    	viewer.si.siUpdateRecentMenus(file.getFullPath());
			    out.closeChannel();
			    return msg;
				}
			case 2:
				type = tokens.get(0).toUpperCase();
				fileName = PT.trimQuotes(tokens.get(1));
				break;
			}
			String ext = fileName.substring(fileName.lastIndexOf(".") + 1)
					.toUpperCase();
			if (ext.equals("BASE64")) {
				fileName = ";base64,";
			} else if (ext.equals("JDX")) {
				if (type == null)
					type = "XY";
			} else if (ExportType.isExportMode(ext)) {
				type = ext;
			} else if (ExportType.isExportMode(type)) {
				fileName += "." + type;
			}
			eType = ExportType.getType(type);
			if (forInkscape && eType == ExportType.SVG)
				eType = ExportType.SVGI;
			
			out = viewer.getOutputChannel(fileName, false);
			return exportSpectrumOrImage(viewer, eType, -1, out);
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}

  /**
   * 
   * This export method will clip the data based on the current display
   * 
   * @param viewer 
   * @param eType
   * @param index
   * @param out
   * @return  status line message
   */
  private String exportSpectrumOrImage(JSViewer viewer, ExportType eType,
                                              int index, OC out) {
    Spectrum spec;
    PanelData pd = viewer.pd();    
    if (index < 0 && (index = pd.getCurrentSpectrumIndex()) < 0)
      return "Error exporting spectrum: No spectrum selected";
    spec = pd.getSpectrumAt(index);
    int startIndex = pd.getStartingPointIndex(index);
    int endIndex = pd.getEndingPointIndex(index);
    String msg = null;
    try {
    	boolean asBase64 = out.isBase64();
    	msg = exportTheSpectrum(viewer, eType, out, spec, startIndex, endIndex, pd, asBase64);
    	if (asBase64)
    		return msg;
    	if (msg.startsWith("OK"))
    		return "OK - Exported " + eType.name() + ": " + out.getFileName() + msg.substring(2);
    } catch (Exception ioe) {
      msg = ioe.toString();
    }
    return "Error exporting " + out.getFileName() + ": " + msg;
  }
  
	@Override
	public String exportTheSpectrum(JSViewer viewer, ExportType mode,
			OC out, Spectrum spec, int startIndex, int endIndex,
			PanelData pd, boolean asBase64) throws Exception {
		JSVPanel jsvp = viewer.selectedPanel;
		String type = mode.name();
		switch (mode) {
		case AML:
		case CML:
		case SVG:
		case SVGI:
			break;
		case DIF:
		case DIFDUP:
		case FIX:
		case PAC:
		case SQZ:
		case XY:
			type = "JDX";
			break;
		case JPG:
		case PNG:
			if (jsvp == null)
				return null;
			viewer.fileHelper.setFileChooser(mode);
			String name = getSuggestedFileName(viewer, mode);
			GenericFileInterface file = viewer.fileHelper.getFile(name, jsvp, true);
			if (file == null)
				return null;
			return jsvp.saveImage(type.toLowerCase(), file, out);
		case PDF:
			return printPDF(viewer, "PDF", asBase64);
		case SOURCE:
			if (jsvp == null)
				return null;
		  String data = jsvp.getPanelData().getSpectrum().getInlineData();
		  if (data != null) {
			  out.append(data);
			  out.closeChannel();
  	    return "OK " + out.getByteCount() + " bytes";
		  }
			String path = jsvp.getPanelData().getSpectrum().getFilePath();
			return fileCopy(path, out);
		case UNK:
			return null;
		}
		return ((JSVExporter) JSViewer.getInterface("jspecview.export."
				+ type.toUpperCase() + "Exporter")).exportTheSpectrum(viewer, mode,
				out, spec, startIndex, endIndex, null, false);
	}

	@SuppressWarnings("resource")
	private String printPDF(JSViewer viewer, String pdfFileName, boolean isBase64) {

		boolean isJob = (pdfFileName == null || pdfFileName.length() == 0);
		if (!isBase64 && !viewer.si.isSigned())
			return "Error: Applet must be signed for the PRINT command.";
		PanelData pd = viewer.pd();
		if (pd == null)
			return null;
		boolean useDialog = false;
		PrintLayout pl;
		/**
		 * @j2sNative 
		 * 
		 * useDialog = false;
		 * 
		 */
		{
			pd.closeAllDialogsExcept(AType.NONE);
			useDialog = true;
		}
    pl = viewer.getDialogPrint(isJob);
		if (pl == null)
			return null;
    if (!useDialog)
			pl.asPDF = true; // JavaScript only
		if (isJob && pl.asPDF) {
			isJob = false;
			pdfFileName = "PDF";
		}
		JSVPanel jsvp = viewer.selectedPanel;
		if (!isBase64 && !isJob) {
			JSVFileHelper helper = viewer.fileHelper;
			helper.setFileChooser(ExportType.PDF);
			if (pdfFileName.equals("?") || pdfFileName.equalsIgnoreCase("PDF"))
				pdfFileName = getSuggestedFileName(viewer, ExportType.PDF);
			GenericFileInterface file = helper.getFile(pdfFileName, jsvp, true);
			if (file == null)
				return null;
			if (!JSViewer.isJS)
				viewer.setProperty("directoryLastExportedFile",
						helper.setDirLastExported(file.getParentAsFile().getFullPath()));
			pdfFileName = file.getFullPath();
		}
		String s = null;
		try {
			OC out = (isJob ? null : 
				isBase64 ? new OC().setParams(null,  ";base64,", false, null)
						: viewer.getOutputChannel(pdfFileName, true));
			String printJobTitle = pd.getPrintJobTitle(true);
			if (pl.showTitle) {
				printJobTitle = jsvp.getInput("Title?", "Title for Printing",
						printJobTitle);
				if (printJobTitle == null)
					return null;
			}
			jsvp.printPanel(pl, out, printJobTitle);
			s = out.toString();
		} catch (Exception e) {
			jsvp.showMessage(e.toString(), "File Error");
		}
		return s;
	}
	
	private String[] getExportableItems(JSViewer viewer,
			boolean isSameType) {
		PanelData pd = viewer.pd();
		boolean isView = viewer.currentSource.isView;
		// From popup menu click SaveAs or Export
		// if JSVPanel has more than one spectrum...Choose which one to export
		int nSpectra = pd.getNumberOfSpectraInCurrentSet();
		if (nSpectra == 1 || !isView && isSameType
				|| pd.getCurrentSpectrumIndex() >= 0)
			return null;
		String[] items = new String[nSpectra];
		for (int i = 0; i < nSpectra; i++)
			items[i] = pd.getSpectrumAt(i).getTitle();
		return items;
	}

	private String getSuggestedFileName(JSViewer viewer, ExportType imode) {
		PanelData pd = viewer.pd();
    String sourcePath = pd.getSpectrum().getFilePath();
    String newName = JSVFileManager.getTagName(sourcePath);
    if (newName.startsWith("$"))
    	newName = newName.substring(1);
    int pt = newName.lastIndexOf(".");
    String name = (pt < 0 ? newName : newName.substring(0, pt));
    String ext = ".jdx";
    boolean isPrint = false;
    switch (imode) {
    case XY:
    case FIX:
    case PAC:
    case SQZ:
    case DIF:
    case DIFDUP:
    	if (!(name.endsWith("_" + imode)))
    		name += "_" + imode;    		
      ext = ".jdx";
      break;
    case AML:
    	ext = ".xml";
    	break;
    case SOURCE:
      if (!(name.endsWith("_" + imode)))
        name += "_" + imode;
      String lc = (sourcePath == null ? "jspecview" : sourcePath.toLowerCase());
      ext = (lc.endsWith(".zip") ? ".zip" : lc.endsWith(".jdx") ? ".jdx" : "");
      break;
    case JPG:
    case PNG:
    case PDF:
    	isPrint = true;
			//$FALL-THROUGH$
		default:
      ext = "." + imode.toString().toLowerCase();
    }
    if (viewer.currentSource.isView)
    	name = pd.getPrintJobTitle(isPrint);
    name += ext;
    return name;
	}

  private static String fileCopy(String name, OC out) {
    try {
      BufferedReader br = JSVFileManager.getBufferedReaderFromName(name);
      String line = null;
      while ((line = br.readLine()) != null) {
        out.append(line);
        out.append(newLine);
      }
      out.closeChannel();
      return "OK " + out.getByteCount() + " bytes";
    } catch (Exception e) {
      return e.toString();
    }
  }


}

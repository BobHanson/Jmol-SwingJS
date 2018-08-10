package jspecview.java;

import java.awt.Component;
import java.io.File;

import javajs.api.GenericFileInterface;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;


import jspecview.api.JSVFileHelper;
import jspecview.common.ExportType;
import jspecview.common.JSViewer;

public class AwtFileHelper implements JSVFileHelper {
	
	public String dirLastOpened;
	public boolean useDirLastOpened;
	public boolean useDirLastExported;
	public String dirLastExported;

	private JFileChooser fc;
	private JSViewer vwr;

	@Override
	public AwtFileHelper set(JSViewer viewer) {
		this.vwr = viewer;
		return this;
	}
	
	@Override
	public void setFileChooser(ExportType imode) {
		if (fc == null)
		  fc = new JFileChooser();
    AwtDialogFileFilter filter = new AwtDialogFileFilter();
    fc.resetChoosableFileFilters();
    switch (imode) {
    case UNK:
  		filter = new AwtDialogFileFilter();
  		filter.addExtension("xml");
  		filter.addExtension("aml");
  		filter.addExtension("cml");
  		filter.setDescription("CML/XML Files");
  		fc.setFileFilter(filter);
  		filter = new AwtDialogFileFilter();
  		filter.addExtension("jdx");
  		filter.addExtension("dx");
  		filter.setDescription("JCAMP-DX Files");
  		fc.setFileFilter(filter);
    	break;
    case XY:
    case FIX:
    case PAC:
    case SQZ:
    case DIF:
    case DIFDUP:
    case SOURCE:
      filter.addExtension("jdx");
      filter.addExtension("dx");
      filter.setDescription("JCAMP-DX Files");
      break;
    default:
      filter.addExtension(imode.toString().toLowerCase());
      filter.setDescription(imode + " Files");
    }
    fc.setFileFilter(filter);    
	}

	@Override
	public GenericFileInterface showFileOpenDialog(Object panelOrFrame, Object[] userData) {
		setFileChooser(ExportType.UNK);
		return getFile("", panelOrFrame, false);
	}

	@Override
	public GenericFileInterface getFile(String name, Object panelOrFrame, boolean isSave) {
		Component c = (Component) panelOrFrame;
		fc.setSelectedFile(new File(name));
		if (isSave) {
			if (useDirLastExported)
				fc.setCurrentDirectory(new File(dirLastExported));
		} else {
			if (useDirLastOpened)
				fc.setCurrentDirectory(new File(dirLastOpened));
		}
		int returnVal = (isSave ? fc.showSaveDialog(c) : fc.showOpenDialog(c));
		if (returnVal != JFileChooser.APPROVE_OPTION)
			return null;
		AwtFile file = new AwtFile(fc.getSelectedFile().getAbsolutePath());
		if (isSave) {
			vwr.setProperty("directoryLastExportedFile", dirLastExported = file.getParent());
	    if (file.exists()) {
	      int option = JOptionPane.showConfirmDialog(c,
	          "Overwrite " + file.getName() + "?", "Confirm Overwrite Existing File",
	          JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
	      if (option == JOptionPane.NO_OPTION)
	        return null;
	    }
		} else {
			vwr.setProperty("directoryLastOpenedFile", dirLastOpened = file.getParent());
		}
		return file;
	}

	@Override
	public String setDirLastExported(String name) {
		return dirLastExported = name;
	}

	@Override
	public String getUrlFromDialog(String info, String msg) {
		return (String) JOptionPane.showInputDialog(null,
				info, "Open URL",
				JOptionPane.PLAIN_MESSAGE, null, null, msg);
	}


}

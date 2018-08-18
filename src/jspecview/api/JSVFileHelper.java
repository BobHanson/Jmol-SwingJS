package jspecview.api;

import org.jmol.api.GenericFileInterface;

import jspecview.common.ExportType;
import jspecview.common.JSViewer;

public interface JSVFileHelper {

	void setFileChooser(ExportType pdf);

	GenericFileInterface getFile(String fileName, Object panelOrFrame, boolean isAppend);

	String setDirLastExported(String name);

	JSVFileHelper set(JSViewer jsViewer);

	GenericFileInterface showFileOpenDialog(Object panelOrFrame, Object[] userData);

	String getUrlFromDialog(String info, String msg);

}

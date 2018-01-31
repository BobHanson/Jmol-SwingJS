package jspecview.api;

import java.net.URL;

import javajs.util.Lst;


import jspecview.app.JSVApp;
import jspecview.common.Spectrum;
import jspecview.common.JSViewer;

public interface AppletFrame {

	void callToJavaScript(String callbackName, Object[] data);

	void createMainPanel(JSViewer viewer);

	void doExitJmol();

	JSVApp getApp();

	String getAppletInfo();

	URL getDocumentBase();

	JSVPanel getJSVPanel(JSViewer viewer, Lst<Spectrum> specs);

	String getParameter(String name);

	void newWindow(boolean isSelected);

	void repaint();

	void setDropTargetListener(boolean isSigned, JSViewer viewer);

	void setPanelVisible(boolean b);

	void validate();

	void validateContent(int mode);

}

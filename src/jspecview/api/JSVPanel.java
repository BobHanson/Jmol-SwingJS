package jspecview.api;

import java.io.OutputStream;

import org.jmol.api.GenericFileInterface;
import org.jmol.api.GenericPlatform;

import javajs.api.GenericColor;
import javajs.util.OC;


import jspecview.common.PanelData;
import jspecview.common.PrintLayout;

/**
 * JSVPanel class represents a View combining one or more GraphSets, each with one or more JDXSpectra.
 */

public interface JSVPanel extends JSVViewPanel {
  
	void repaint();

	void doRepaint(boolean andTaintAll);
  
  void getFocusNow(boolean asThread);
  String getInput(String message, String title, String sval);
  PanelData getPanelData();

  boolean hasFocus();

  void setToolTipText(String s);

  void showMessage(String msg, String title);

	GenericPlatform getApiPlatform();

	void setBackgroundColor(GenericColor color);

	int getFontFaceID(String name);

	String saveImage(String type, GenericFileInterface file, OC out, int width, int height);

	void printPanel(PrintLayout pl, OutputStream os, String printJobTitle);

	boolean processMouseEvent(int id, int x, int y, int modifiers, long time);

	void processTwoPointGesture(double[][][] touches);
	
	void processKeyEvent(Object event);

	void showMenu(int x, int y);

	void paintComponent(Object display);

}

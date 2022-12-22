package jspecview.api;

import java.io.OutputStream;

import org.jmol.api.GenericFileInterface;
import org.jmol.api.GenericPlatform;

import javajs.api.GenericColor;
import javajs.util.OC;


import jspecview.common.PanelData;
import jspecview.common.PrintLayout;

public interface JSVPanel extends JSVViewPanel {

	public void repaint();

	public void doRepaint(boolean andTaintAll);
  
  void getFocusNow(boolean asThread);
  String getInput(String message, String title, String sval);
  PanelData getPanelData();

  boolean hasFocus();

  void setToolTipText(String s);

  void showMessage(String msg, String title);

	GenericPlatform getApiPlatform();

	void setBackgroundColor(GenericColor color);

	int getFontFaceID(String name);

	String saveImage(String type, GenericFileInterface file, OC out);

	public void printPanel(PrintLayout pl, OutputStream os, String printJobTitle);

	public boolean processMouseEvent(int id, int x, int y, int modifiers, long time);

	public void processTwoPointGesture(double[][][] touches);
	
	public void processKeyEvent(Object event);

	public void showMenu(int x, int y);

	public void paintComponent(Object display);

}

/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2021-01-02 08:18:17 -0600 (Sat, 02 Jan 2021) $
 * $Revision: 22069 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.api;

import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javajs.util.BS;

import org.jmol.script.SV;
import org.jmol.util.BoxInfo;

import javajs.util.P3d;
import javajs.util.V3d;
import org.jmol.viewer.Viewer;

/**
 * JmolViewer is the main API for the Viewer class.
 * (Mosty) we try to not change this (much), whereas the varius 
 * "public" methods of Viewer, ModelSet, and other classes are public
 * only for internal cross-package access, not for external use.
 * 
 * Note, this interface was changed to all double
 * 
 *
 **/

abstract public class JmolViewer {

  
  
  static {
    /**
     *  @j2sNative
     *  
     *  self.Jmol || (Jmol = self.J2S); 
     *  Jmol._isSwingJS = true; Jmol._isAWTjs = true;
     */
    
  }
  
  static {
//    /**
//     * allows customization of Viewer -- not implemented in JSmol.
//     * 
//     * @j2sNative
//     * 
//     *            self.Jmol && Jmol.extend && Jmol.extend("vwr",
//     *            org.jmol.viewer.Viewer.prototype);
//     * 
//     */
//    {
//    }
  }


  //removed in Jmol 14.3.11 streamlining:
  //  most are internal to Jmol communication.
  //  others are accessible via public fields ((Viewer) viewer).foo
  
  //abstract public Object getDisplay(); foo=display
  //abstract public String getModelProperty(int modelIndex, String propertyName);
  //abstract public Map<String, Object> getModelAuxiliaryInfo(int modelIndex);
  
  //abstract public int getAtomCount(); foo=ms.ac
  //abstract public String getAltLocListInModel(int modelIndex);

  //abstract public String getModelFileName(int modelIndex); foo=ms.getModelFileName(modelIndex)
  //abstract public int getGroupCount();foo=ms.getGroupCountInModel(-1)
  //abstract public int getPolymerCount();foo=ms.getBioPolymerCount()
  //abstract public int getAtomCountInModel(int modelIndex);foo=ms.am[modelIndex].
  //abstract public int getBondCountInModel(int modelIndex);  // use -1 here for "all"
  //abstract public int getChainCount(); foo=ms.getChainCountInModelWater(-1, true);
  //abstract public int getChainCountInModel(int modelIindex); foo=ms.getChainCountInModelWater(modelIndex, false);
  //abstract public int getGroupCountInModel(int modelIndex); foo=ms.getGroupCountInModel(modelIndex);
  //abstract public int getPolymerCountInModel(int modelIndex); foo=ms.getPolymerCountInModel(modelIndex);
  //abstract public int getSelectionCount(); foo=slm.getSelectionCount()

  //abstract public BS getSelectedAtoms(); foo=slm.getSelectedAtoms()
  //abstract public boolean isApplet(); foo=isApplet
  //abstract public int modelGetLastVibrationIndex(int i, int tok); not really public

  //abstract public Map<String, String> getHeteroList(int modelIndex);
  //abstract public boolean getPerspectiveDepth();

  //abstract public int getAtomNumber(int atomIndex); foo=ms.at[atomIndex].getAtomNumber()
  //abstract public String getAtomName(int atomIndex); foo=ms.at[atomIndex].getAtomName()
  //abstract public P3 getAtomPoint3f(int atomIndex); foo=ms.at[atomIndex]
  //abstract public int getAtomModelIndex(int atomIndex); foo=ms.at[atomIndex].mi
  //abstract public int getModelCount(); foo=ms.mc
  //abstract public int getDisplayModelIndex(); foo=am.cmi
  //abstract public boolean haveFrame(); // foo=true
  //abstract public String getModelSetName(); foo=ms.modelSetName
  //abstract public float getZoomPercentFloat(); foo=tm.zmPct

  //abstract public Object getModelAuxiliaryInfoValue(int modelIndex, String keyName);
  //abstract public boolean modelHasVibrationVectors(int modelIndex);



  // not really public. There are other, more general, ways of getting these
    
  //abstract public String getAtomInfo(int atomIndex);
  //abstract public float getAtomRadius(int atomIndex);
  //abstract public void setShowAxes(boolean showAxes);
  //abstract public void setShowBbcage(boolean showBbcage);
  //abstract public int getAtomArgb(int atomIndex);
  //abstract public float getBondRadius(int bondIndex);
  //abstract public P3 getBondPoint3f1(int bondIndex);
  //abstract public P3 getBondPoint3f2(int bondIndex);
  //abstract public int getBondArgb1(int bondIndex);
  //abstract public int getBondArgb2(int bondIndex);
  //abstract public int getBondOrder(int bondIndex);
  //abstract public int getBondModelIndex(int bondIndex);
  //abstract public P3[] getPolymerLeadMidPoints(int modelIndex, int polymerIndex);
  //abstract public boolean havePartialCharges(); foo=(ms.getPartialCharges() != null)
  //abstract public int getBondCount(); // NOT THE REAL BOND COUNT -- just an array maximum
  //abstract public void setSelectionHalos(boolean haloEnabled);
  //abstract public Object getFileAsBytes(String fullPathName, OC out); foo=fm.getFileAsBytes(pathName, out, true)
  //abstract public void processMultitouchEvent(int groupID, int eventType, int touchID, int iData, P3 pt, long time);




  
  // several methods were deprecated and removed in 13.1.15. All are accessible via "getXxxx" methods:

  //abstract public int getZoomPercent(); //deprecated

  abstract public double getDouble(int tok);
  abstract public int getInt(int tok);
  abstract public boolean getBoolean(int tok);

  //abstract public int getAnimationFps();  see getInt(T.animationFps)
  //abstract public boolean getShowHydrogens(); see getBoolean(T.showhydrogens)
  //abstract public boolean getShowMeasurements(); see getBoolean(T.showmeasurements)
  //abstract public boolean getAxesOrientationRasmol(); see getBoolean(T.axesorientationrasmol)
  //abstract public int getPercentVdwAtom(); see getInt(T.percentvdwatom)
  //abstract public boolean getAutoBond(); see getBoolean(T.autobond))
  //abstract public boolean showModelSetDownload(); deprecated -- was just "true"
  
  /**
   * This is the older main access point for creating an application or applet vwr.
   * 
   * You can also use That is necessary when
   * compiled into JavaScript using Java2Script
   * 
   * In Jmol 11.6 it was manditory that one of the next commands is either
   * 
   * vwr.evalString("ZAP");
   * 
   * or at least:
   * 
   * vwr.setAppletContext("",null,null,"")
   * 
   * One or the other of these was necessary to establish the first modelset,
   * which might be required by one or more later evaluated commands or file
   * loadings.
   * 
   * Starting with Jmol 11.7, setAppletContext is rolled into allocateViewer so
   * that the full initialization is done all at once.
   * 
   * Starting with Jmol 12.3.13, we allow for preconstructed ApiPlatform
   * 
   * 
   * @param display
   * @param modelAdapter
   * @param fullName
   * @param documentBase
   * @param codeBase
   * @param commandOptions
   * @param statusListener
   * @param implementedPlatform
   * @return a JmolViewer object
   */
  protected static JmolViewer allocateViewer(Object display,
                                          JmolAdapter modelAdapter,
                                          String fullName, URL documentBase,
                                          URL codeBase, String commandOptions,
                                          JmolStatusListener statusListener,
                                          GenericPlatform implementedPlatform) {
    
    
    
    Map<String, Object> info = new Hashtable<String, Object>();
    if (display != null)
      info.put("display", display);
    if (modelAdapter != null)
      info.put("adapter", modelAdapter);
    if (statusListener != null)
      info.put("statuslistener", statusListener);
    if (implementedPlatform != null)
      info.put("platform", implementedPlatform);
    if (commandOptions != null)
       info.put("options", commandOptions);
    if (fullName != null)
      info.put("fullname", fullName);
    if (documentBase != null)
      info.put("documentbase", documentBase);
    if (codeBase != null)
      info.put("codebase", codeBase); 
    info.put("isApp",Boolean.TRUE);
    return new Viewer(info);
  }
  
  /**
   * a simpler option
   * 
   * @param container
   * @param jmolAdapter
   * @return JmolViewer object
   */
  public static JmolViewer allocateViewer(Object container, JmolAdapter jmolAdapter) {
    return allocateViewer(container, jmolAdapter, null, null, null, null, null, null);
  }
  
  /**
   * legacy only
   * 
   * @param display
   * @param modelAdapter
   * @param fullName
   * @param documentBase
   * @param codeBase
   * @param commandOptions
   * @param statusListener
   * @return JmolViewer object
   */
  public static JmolViewer allocateViewer(Object display,
                                          JmolAdapter modelAdapter,
                                          String fullName, URL documentBase,
                                          URL codeBase, String commandOptions,
                                          JmolStatusListener statusListener) {
    return allocateViewer(display, modelAdapter, fullName, documentBase,
        codeBase, commandOptions, statusListener, null);
  }

  /**
   * sets a custom console -- should be called IMMEDIATELY following allocateViewer
   * 
   * create your console with, perhaps:
   * 
   * new org.openscience.jmol.app.jmolPanel.AppConsole(vwr, displayFrame, 
   *                               externalJPanel, buttonsEnabled);
   * 
   * (see examples/basic/org/jmol/Integration.java
   * 
   * @param console        the console to use  
   * 
   */
  public void setConsole(JmolAppConsoleInterface console) {
    getProperty("DATA_API", "getAppConsole", console); 
  }

  abstract public void setInMotion(boolean isInMotion);

  abstract public BS getSmartsMatch(String smarts, BS bsSelected) throws Exception;
  
  static public String getJmolVersion() {
    return Viewer.getJmolVersion();
  }

  /**
   * for POV-Ray 
   * @param params
   * @return INI file
   * 
   */
  abstract public String generateOutputForExport(Map<String, Object> params); 
  
  abstract public void setJmolCallbackListener(JmolCallbackListener jmolCallbackListener);

  abstract public void setJmolStatusListener(JmolStatusListener jmolStatusListener);

  abstract public boolean checkHalt(String strCommand, boolean isInterrupt);
  abstract public void haltScriptExecution();

  abstract public void pushHoldRepaint();
  abstract public void popHoldRepaint(String why);

  abstract public String getData(String atomExpression, String type);

  abstract public String getSmiles(BS atoms) throws Exception;
  
  abstract public String getOpenSmiles(BS atoms) throws Exception;
  
  abstract public void setScreenDimension(int width, int height);
  abstract public int getScreenWidth();
  abstract public int getScreenHeight();

  abstract public Object getScreenImageBuffer(Object g, boolean isImageWrite);
  abstract public void releaseScreenImage();
  
  abstract public String writeTextFile(String string, String data);
  
  /**
   * 
   * @param params include type, fileName, text, bytes, quality, width, height 
   * @return          null (canceled) or a message starting with OK or an error message
   */
  abstract public String outputToFile(Map<String, Object> params);

  /**
   * @param type 
   * @param width 
   * @param height 
   * @param quality 
   * @param errMsg TODO
   * @return base64-encoded or binary version of the image
   */
  abstract public byte[] getImageAsBytes(String type, int width, int height, int quality, String[] errMsg);

  abstract public int getMotionEventNumber();


  /**
   * Opens the file and creates the model set, given the reader.
   * 
   * not used in Jmol
   * 
   * @param fullPathName full path name or null
   * @param reader a Reader, byte[], or BufferedInputStream
   * 
   * @return       null or error message
   */
   
  public String openReader(String fullPathName, Object reader) {
    return openReader(fullPathName == null ? "String" : fullPathName, null, reader);
  }
  

  /**
   * Opens the file and creates the model set, given the reader.
   * 
   * name is a text name of the file ... to be displayed in the window no need
   * to pass a BufferedReader ... ... the FileManager will wrap a buffer around
   * it
   *
   * not used in Jmol
   * 
   * @param fullPathName or null
   * @param fileName (no path) or null
   * @param reader Reader, byte[], or BufferedInputStream
   * 
   * @return       null or error message
   */
   
  abstract public String openReader(String fullPathName, String fileName, Object reader);
  
  /*
   * REMOVED -- this method does not actually open the file
   * 
   * @param fullPathName
   * @param fileName
   * @param clientFile
   * @deprecated
   */
//  abstract public void openClientFile(String fullPathName, String fileName,
  //                           Object clientFile);

  abstract public void showUrl(String urlString);

  abstract public void calcAtomsMinMax(BS bs, BoxInfo boxInfo);
  abstract public P3d getBoundBoxCenter();
  abstract public V3d getBoundBoxCornerVector();
  abstract public int getMeasurementCount();
  abstract public String getMeasurementStringValue(int i);
  abstract public int[] getMeasurementCountPlusIndices(int i);

  abstract public BS getElementsPresentBitSet(int modelIndex);

  abstract public int findNearestAtomIndex(int x, int y);

  abstract public String script(String script);
  abstract public Object scriptCheck(String script);
  abstract public String scriptWait(String script);
  abstract public Object scriptWaitStatus(String script, String statusList);
  abstract public String loadInline(String strModel);
  abstract public String loadInlineAppend(String strModel, boolean isAppend);
  abstract public String loadInline(String strModel, char newLine);
  abstract public String loadInline(String[] arrayModels);
  /**
   * 
   * @param arrayModels and array of models, each of which is a String
   * @param isAppend
   * @return null or error message
   */
  abstract public String loadInline(String[] arrayModels, boolean isAppend);
  /**
   * 
   * NOTE: THIS METHOD DOES NOT PRESERVE THE STATE
   * 
   * @param arrayData a Vector of models, where each model is either a String
   *                  or a String[] or a Vector<String>
   * @param isAppend TRUE to append models (no ZAP)
   * @return null or error message
   */
  abstract public String loadInline(List<Object> arrayData, boolean isAppend);

  abstract public String evalStringQuiet(String script);
  abstract public boolean isScriptExecuting();

  abstract public String getModelSetFileName();
  abstract public String getModelSetPathName();
  abstract public Properties getModelSetProperties();
  abstract public Map<String, Object> getModelSetAuxiliaryInfo();
  abstract public Properties getModelProperties(int modelIndex);
  abstract public int getModelNumber(int modelIndex);
  abstract public String getModelName(int modelIndex);
  abstract public String getModelNumberDotted(int modelIndex);

  abstract public BS getVisibleFramesBitSet();
  
  
  abstract public void addSelectionListener(JmolSelectionListener listener);
  abstract public void removeSelectionListener(JmolSelectionListener listener);
  
  abstract public void homePosition();

  abstract public int getBackgroundArgb();
  
  abstract public short getMadBond();

  abstract public void rebond();

  abstract public void refresh(int isOrientationChange, String strWhy);

  abstract public void notifyViewerRepaintDone();

  abstract public boolean getBooleanProperty(String propertyName);

  abstract public Object getParameter(String name);

  abstract public String getSetHistory(int howFarBack);
  
  abstract public String getStateInfo();
  
  abstract public void syncScript(String script, String applet, int port);  

  abstract public void setColorBackground(String colorName);
  
  abstract public void setJmolDefaults();
  abstract public void setRasmolDefaults();

  abstract public void setBooleanProperty(String propertyName, boolean value);
  abstract public void setIntProperty(String propertyName, int value);
  abstract public void setFloatProperty(String propertyName, double value);
  abstract public void setStringProperty(String propertyName, String value);

  abstract public void setShowHydrogens(boolean showHydrogens);
  abstract public void setShowMeasurements(boolean showMeasurements);
  abstract public void setPerspectiveDepth(boolean perspectiveDepth);
  abstract public void setAutoBond(boolean autoBond);
  abstract public void setMarBond(short marBond);
  abstract public void setBondTolerance(double bondTolerance);
  abstract public void setMinBondDistance(double minBondDistance);
  abstract public void setAxesOrientationRasmol(boolean axesMessedUp);
  abstract public void setPercentVdwAtom(int percentVdwAtom);
  
  //for each of these the script equivalent is shown  
  abstract public void setAnimationFps(int framesPerSecond);
  //vwr.script("animation fps x.x")
  abstract public void setFrankOn(boolean frankOn);
  //vwr.script("frank on")
  abstract public void setDebugScript(boolean debugScript);
  //vwr.script("set logLevel 5/4")
  //vwr.script("set debugScript on/off")
  abstract public void deleteMeasurement(int i);
  //vwr.script("measures delete " + (i + 1));
  abstract public void clearMeasurements();
  //vwr.script("measures delete");
  abstract public void setVectorScale(double vectorScaleValue);
  //vwr.script("vector scale " + vectorScaleValue);
  abstract public void setVibrationScale(double vibrationScaleValue);
  //vwr.script("vibration scale " + vibrationScaleValue);
  abstract public void setVibrationPeriod(double vibrationPeriod);
  //vwr.script("vibration " + vibrationPeriod);
  abstract public void selectAll();
  //vwr.script("select all");
  abstract public void clearSelection();
  //vwr.script("select none");
  //vwr.script("select ({2 3:6})");
  abstract public void setSelectionSet(BS newSelection);
  //vwr.script("selectionHalos ON"); //or OFF
  //vwr.script("center (selected)");
  abstract public void setCenterSelected(); 

  abstract public void rotateFront();

  abstract public JmolAdapter getModelAdapter();

  abstract public void openFileAsyncSpecial(String fileName, int flags);
  
  public void openFileAsync(String fileName) {
    openFileAsyncSpecial(fileName, 0);    
  }
  
  abstract public String getErrorMessage();
  abstract public String getErrorMessageUn();

  public String menuStructure;

  public GenericPlatform apiPlatform; // used in Viewer and JmolViewer

  abstract public void renderScreenImage(Object g, int width, int height);
  abstract public String evalFile(String strFilename);
  abstract public String evalString(String strScript);

  abstract public String openStringInline(String strModel);
  abstract public String openDOM(Object DOMNode);
  abstract public String openFile(String fileName);
  abstract public String openFiles(String[] fileNames);
  // File reading now returns the error directly.
  // The following was NOT what you think it was:
  //   abstract public String getOpenFileError();
  // Somewhere way back when, "openFile" became a method that did not create
  // the model set, but just an intermediary AtomSetCollection called the "clientFile"
  // (and did not necessarily close the file)
  // then "getOpenFileError()" actually created the model set, deallocated the file open thread,
  // and closed the file.
  //
  // For Jmol 11.7.14, the openXXX methods in this interface do everything --
  // open the file, create the intermediary atomSetCollection, close the file,
  // deallocate the file open thread, create the ModelSet, and return any error message.
  // so there is no longer any need for getOpenFileError().
  
  /**
   * @param returnType "JSON", "string", "readable", and anything else returns the Java object.
   * @param infoType 
   * @param paramInfo  
   * @return            property data -- see org.jmol.viewer.PropertyManager.java
   */
  abstract public Object getProperty(String returnType, String infoType, Object paramInfo);

  /**
   * @param expression
   * @return a String representation of the evaluated expression
   */
  abstract public Object evaluateExpression(Object expression);

  /**
   * @param expression
   * @return a String representation of the evaluated expression as a script variable (org.jmol.script.SV)
   */
  abstract public SV evaluateExpressionAsVariable(Object expression);

  abstract public int[] resizeInnerPanel(int width, int height);

  /**
   * starting with 14.8.2-beta-2017-02-06, uses script(xxxx) function;
   * see runScriptCautiously for the older version
   * 
   * @param script
   * @return string from ScriptEvaluator#outputBuffer
   */
  abstract public String runScript(String script);

  /**
   * Formerly runScript(script). 
   * 
   * run a script immediately and return output buffer string
   * Jmol 13.1.15
   * 
   * @param script
   * @return string from ScriptEvaluator#outputBuffer
   */
  public String runScriptCautiously(String script){return null;}

  abstract public String extractMolData(String what);
  
  abstract public String getClipboardText();
  
  abstract public String clipImageOrPasteText(String text);
  abstract public void notifyStatusReady(boolean isReady);

  /**
   * 
   * @param id
   *     some_id,
   *     filename#id, or
   *     ~fileNo.modelNo
   *     
   *     added ".basemodel" indicates to get the first model in a 
   *     series from a JDX-MOL file.
   *     
   * @return model index if found; 
   *       -2 if file found but model not found 
   *       -2 if no file indicated and no model found 
   *       -1 if no such file
   */
  abstract public int getModelIndexFromId(String id);
  
  abstract public void setMenu(String menuFile, boolean isFile);
  public void dispose() {
    // TODO
  }
  

}


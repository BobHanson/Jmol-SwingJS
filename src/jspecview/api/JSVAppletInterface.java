package jspecview.api;

import java.util.Map;

public interface JSVAppletInterface {

  /**
   * Returns the calculated colour of a visible spectrum (Transmittance)
   * 
   * @return Color
   */

  String getSolnColour();

  /**
   * Method that can be called from another applet or from javascript to return
   * the coordinate of clicked point in the plot area of the <code>
   * JSVPanel</code>
   * 
   * @return A String representation of the coordinate
   */
  String getCoordinate();

  /**
   * Loads in-line JCAMP-DX data into the existing applet window
   * 
   * @param data
   *        String
   */
  void loadInline(String data);

  /*
   * Deprecated -- fails in MSIE object JavaScript
   * 
   * @param type
   * @param n
   * @return data
   * 
  @Deprecated
  String export(String type, int n);
   */

  /**
   * Delivers spectrum coded as desired: XY, SQZ, PAC, DIF, DIFDUP, FIX, AML, CML, PDF(base64-encoded)
   * 
   * @param type
   * @param n
   * @return data
   * 
   */
  String exportSpectrum(String type, int n);

	void setFilePath(String tmpFilePath);

  /**
   * Sets the spectrum to the specified block number
   * 
   * @param i
   */
  void setSpectrumNumber(int i);

  /**
   * Method that can be called from another applet or from javascript that
   * toggles the grid on a <code>JSVPanel</code>
   */
  void toggleGrid();

  /**
   * Method that can be called from another applet or from javascript that
   * toggles the coordinate on a <code>JSVPanel</code>
   */
  void toggleCoordinate();

  /**
   * Method that can be called from another applet or from javascript that
   * toggles the flag for points only on a <code>JSVPanel</code>
   */
  void togglePointsOnly();

  /**
   * Method that can be called from another applet or from javascript that
   * toggles the integration graph of a <code>JSVPanel</code>.
   */
  void toggleIntegration();

  /**
   * Method that can be called from another applet or from javascript that adds
   * a highlight to a portion of the plot area of a <code>JSVPanel</code>
   * 
   * @param x1
   *        the starting x value
   * @param x2
   *        the ending x value
   * @param r
   *        the red portion of the highlight color
   * @param g
   *        the green portion of the highlight color
   * @param b
   *        the blue portion of the highlight color
   * @param a
   *        the alpha portion of the highlight color
   */
  void addHighlight(double x1, double x2, int r, int g, int b,
                                    int a);

  /**
   * Method that can be called from another applet or from javascript that
   * removes all highlights from the plot area of a <code>JSVPanel</code>
   */
  void removeAllHighlights();

  /**
   * Method that can be called from another applet or from javascript that
   * removes a highlight from the plot area of a <code>JSVPanel</code>
   * 
   * @param x1
   *        the starting x value
   * @param x2
   *        the ending x value
   */
  void removeHighlight(double x1, double x2);

  /**
   * Method that can be called from another applet or from javascript that
   * toggles reversing the plot on a <code>JSVPanel</code>
   */
  void reversePlot();

  /**
   * runs a script in proper order as listed
   *   
   * @param script
   */

  void runScript(String script);

  /**
   * precede <Peaks here with full name of Jmol applet (including syncID)
   * @param peakScript 
   * 
   */
  void syncScript(String peakScript);

  /**
   * Writes a message to the status label
   * 
   * @param msg
   *        the message
   */
  void writeStatus(String msg);

  Map<String, Object> getPropertyAsJavaObject(String key);

  String getPropertyAsJSON(String key);

  boolean isSigned();

  boolean isPro();

  boolean runScriptNow(String script);
  
  String print(String fileName);

	String checkScript(String script);

}

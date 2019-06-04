package jspecview.api;

import java.util.Map;

public interface JSVAppletInterface {

  /**
   * Returns the calculated colour of a visible spectrum (Transmittance)
   * 
   * @return Color
   */

  public abstract String getSolnColour();

  /**
   * Method that can be called from another applet or from javascript to return
   * the coordinate of clicked point in the plot area of the <code>
   * JSVPanel</code>
   * 
   * @return A String representation of the coordinate
   */
  public abstract String getCoordinate();

  /**
   * Loads in-line JCAMP-DX data into the existing applet window
   * 
   * @param data
   *        String
   */
  public abstract void loadInline(String data);

  /*
   * Deprecated -- fails in MSIE object JavaScript
   * 
   * @param type
   * @param n
   * @return data
   * 
  @Deprecated
  public abstract String export(String type, int n);
   */

  /**
   * Delivers spectrum coded as desired: XY, SQZ, PAC, DIF, DIFDUP, FIX, AML, CML, PDF(base64-encoded)
   * 
   * @param type
   * @param n
   * @return data
   * 
   */
  public abstract String exportSpectrum(String type, int n);

	public abstract void setFilePath(String tmpFilePath);

  /**
   * Sets the spectrum to the specified block number
   * 
   * @param i
   */
  public abstract void setSpectrumNumber(int i);

  /**
   * Method that can be called from another applet or from javascript that
   * toggles the grid on a <code>JSVPanel</code>
   */
  public abstract void toggleGrid();

  /**
   * Method that can be called from another applet or from javascript that
   * toggles the coordinate on a <code>JSVPanel</code>
   */
  public abstract void toggleCoordinate();

  /**
   * Method that can be called from another applet or from javascript that
   * toggles the flag for points only on a <code>JSVPanel</code>
   */
  public abstract void togglePointsOnly();

  /**
   * Method that can be called from another applet or from javascript that
   * toggles the integration graph of a <code>JSVPanel</code>.
   */
  public abstract void toggleIntegration();

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
  public abstract void addHighlight(double x1, double x2, int r, int g, int b,
                                    int a);

  /**
   * Method that can be called from another applet or from javascript that
   * removes all highlights from the plot area of a <code>JSVPanel</code>
   */
  public abstract void removeAllHighlights();

  /**
   * Method that can be called from another applet or from javascript that
   * removes a highlight from the plot area of a <code>JSVPanel</code>
   * 
   * @param x1
   *        the starting x value
   * @param x2
   *        the ending x value
   */
  public abstract void removeHighlight(double x1, double x2);

  /**
   * Method that can be called from another applet or from javascript that
   * toggles reversing the plot on a <code>JSVPanel</code>
   */
  public abstract void reversePlot();

  /**
   * runs a script in proper order as listed
   *   
   * @param script
   */

  public abstract void runScript(String script);

  /**
   * precede <Peaks here with full name of Jmol applet (including syncID)
   * @param peakScript 
   * 
   */
  public abstract void syncScript(String peakScript);

  /**
   * Writes a message to the status label
   * 
   * @param msg
   *        the message
   */
  public abstract void writeStatus(String msg);

  public abstract Map<String, Object> getPropertyAsJavaObject(String key);

  public abstract String getPropertyAsJSON(String key);

  public abstract boolean isSigned();

  public abstract boolean isPro();

  public abstract void setVisible(boolean b);

  public abstract boolean runScriptNow(String script);
  
  public abstract String print(String fileName);

	public abstract String checkScript(String script);

}

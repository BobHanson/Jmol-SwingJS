/* Copyright (c) 2002-2012 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
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

package jspecview.common;

import java.util.Hashtable;

import java.util.Map;
import java.util.Map.Entry;

import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;




/**
 * ScriptToken takes care of script command processing
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

public enum ScriptToken {

  // null tip means DON'T SHOW
  UNKNOWN,
  APPLETID, 
  APPLETREADYCALLBACKFUNCTIONNAME, 
  AUTOINTEGRATE("TF", "automatically integrate an NMR spectrum"), 
  BACKGROUNDCOLOR("C", "set the background color"), 
  CLOSE("spectrumId or fileName or ALL or VIEWS or SIMULATIONS", "close one or more views or simulations"), 
  COMPOUNDMENUON(), // not implemented 
  COORDCALLBACKFUNCTIONNAME, 
  COORDINATESCOLOR("C", "set the color of the coordinates shown in the upper right-hand corner"), 
  COORDINATESON("T", "turn on or off the coordinates shown in the upper right-hand corner"), 
  DEBUG("TF", "turn debugging on and off"),
  DEFAULTLOADSCRIPT("\"script...\"", "set the script to be run after each file is loaded"),
  DEFAULTNMRNORMALIZATION("maxYvalue", "set the value to be given the largest peak in an HMR spectrum"),
  DISPLAYFONTNAME(), 
  DISPLAY1D("T", "turn on or off display of 1D spectra when 1D and 2D spectra are loaded"), 
  DISPLAY2D("T", "turn on or off display of the 2D spectrum when 1D and 2D spectra are loaded"), 
  ENABLEZOOM("T", "allow or disallow zooming"), 
  ENDINDEX, 
  FINDX("value", "move the vertical-line cursor to a specific x-axis value"),
  GETPROPERTY("[propertyName] or ALL or NAMES", "get a property value or all property values as key/value pairs, or a list of names"),
  GETSOLUTIONCOLOR(" FILL or FILLNONE or FILLALL or FILLALLNONE","estimate the solution color for UV/VIS spectra"),  
  GRIDCOLOR("C", "color of the grid"), 
  GRIDON("T", "turn the grid lines on or off"), 
  HELP("[command]", "get this listing or help for a specific command"),
  HIDDEN, 
  HIGHLIGHTCOLOR("C", "set the highlight color"), 
  HIGHLIGHT("OFF or X1 X2 [OFF] or X1 X2 r g b [a]", "turns on or off a highlight color, possibily setting its color, where r g b a are 0-255 or 0.0-1.0"),
  INTEGRALOFFSET("percent", "sets the integral offset from baseline"),
  INTEGRALRANGE("percent", "sets the height of the total integration"),
  INTEGRATE("", "see INTEGRATION"), // same as INTEGRATION
  INTEGRATION("ON/OFF/TOGGLE/AUTO/CLEAR/MIN value/MARK ppm1-ppm2:norm,ppm3-ppm4,...", "show/hide integration or set integrals (1D 1H NMR only)"), 
  INTEGRALPLOTCOLOR("C", "color of the integration line"), 
  INTEGRATIONRATIOS("'x:value,x:value,..'","annotate the spectrum with numbers or text at specific x values"), 
  INTERFACE("SINGLE or OVERLAY", "set how multiple spectra are displayed"),
  INVERTY("", "invert the Y axis"),
  IRMODE("A or T or TOGGLE", "set the IR mode to absorption or transmission"), 
  JMOL("...Jmol command...", "send a command to Jmol (if present)"),
  JSV,
  LABEL("x y [color and/or \"text\"]", "add a text label"),
  LINK("AB or ABC or NONE or ALL", "synchronize the crosshair of a 2D spectrum with 1D cursors"),
  LOAD("[APPEND] \"fileName\" [first] [last]; use \"\" for current file; $H1/name or $C13/name for simulation", "load a specturm"),
  LOADFILECALLBACKFUNCTIONNAME,
  LOADIMAGINARY("TF","set TRUE to load imaginary NMR component"),
  MENUON,
  OBSCURE, 
  OVERLAY, // same as "VIEW"
  OVERLAYSTACKED("TF", "whether viewed spectra are shown separately, in a stack"),
  PEAK("[IR,CNMR,HNMR,MS] [#nnn or ID=xxx or text] [ALL], for example: PEAK HNMR #3",
  			"highlights a peak based on its number or title text, optionally checking all loade spectra"), 
  PEAKCALLBACKFUNCTIONNAME,
  PEAKLIST("[THRESHOLD=n] [INTERPOLATE=PARABOLIC or NONE]", "creates a peak list based on a threshold value and parabolic or no interpolation"),
  PEAKOVERCOLOR("C", "sets the color of peak backgrounds when moused over"),
  PEAKTABCOLOR("C", "sets the color of peak marks for a peak listing"),
  PEAKTABSON("T", "show peak tabs for simulated spectra"),
  PLOTAREACOLOR("C", "sets the color of the plot background"), 
  PLOTCOLOR("C", "sets the color of the graph line"), 
  PLOTCOLORS("color,color,color,...", "sets the colors of multiple plots"), 
  POINTSONLY("TF", "show points only for all data"),
  PRINT("", "prints the current spectrum"),
  REVERSEPLOT("T", "reverses the x-axis of a spectrum"), 
  SCALEBY("factor", "multiplies the y-scale of the spectrum by a factor"),
  SCALECOLOR("C", "sets the color of the x-axis and y-axis scales"),
  SCRIPT("filename.jsv", "runs a script from a file"),
  SELECT("spectrumID, spectrumID,...", "selects one or more spectra based on IDs"),
  SETPEAK("xNew, xOld xNew, ?, or NONE", "sets nearest peak to xOld ppm to a new value; NONE resets (1D NMR only)"),
  SETX("xNew, xOld xNew, ?, or NONE", "sets an old ppm position in the spectrum to a new value; NONE resets (1D NMR only)"),
  SHIFTX("dx or NONE", "shifts the x-axis of a 1D NMR spectrum by the given ppm; NONE resets (1D NMR only)"),
  SHOWERRORS("shows recent errors"),
  SHOWINTEGRATION("T", "shows an integration listing"),
  //SHOWKEY("T", "shows a color key when multiple spectra are displayed"),
  SHOWMEASUREMENTS("T", "shows a listing of measurements"),
  SHOWMENU("displays the popup menu"),
  SHOWPEAKLIST("T", "shows a listing for peak picking"),
  SHOWPROPERTIES("displays the header information of a JDX file"),
  SHOWSOURCE("displays the source JDX file associated with the selected data"),
  SPECTRUM("id", "displays a specific spectrum, where id is a number 1, 2, 3... or a file.spectrum number such as 2.1"), 
  SPECTRUMNUMBER("n", "displays the nth spectrum loaded"),
  STACKOFFSETY("percent", "sets the y-axis offset of stacked spectra"),
  STARTINDEX, 
  SYNCCALLBACKFUNCTIONNAME, 
  SYNCID, 
  TEST, 
  TITLEON("T", "turns the title in the bottom left corner on or off"), // default OFF for application, ON for applet
  TITLEBOLDON("T", "makes the title bold"), 
  TITLECOLOR("C", "sets the color of the title"), 
  TITLEFONTNAME("fontName", "sets the title font"), 
  UNITSCOLOR("C", "sets the color of the x-axis and y-axis units"), 
  VERSION, 
  VIEW("spectrumID, spectrumID, ... Example: VIEW 3.1, 3.2  or  VIEW \"acetophenone\"", "creates a view of one or more spectra"),
  XSCALEON("T", "set FALSE to turn off the x-axis scale"), 
  XUNITSON("T", "set FALSE to turn off the x-axis units"), 
  YSCALE("[ALL] lowValue highValue"), 
  YSCALEON("T", "set FALSE to turn off the y-axis scale"), 
  YUNITSON("T", "set FALSE to turn off the y-axis units"),
  WINDOW,
  WRITE("[XY,DIF,DIFDUP,PAC,FIX,SQZ,AML,CML,JPG,PDF,PNG,SVG] \"filename\"", "writes a file in the specified format"),
  ZOOM("OUT or PREVIOUS or NEXT or x1,x2 or x1,y1 x2,y2", "sets the zoom"),
  ZOOMBOXCOLOR, ZOOMBOXCOLOR2; // not implemented

  private String tip, description;

  public String getTip() {
    return "  "
        + (tip == "T" ? "TRUE/FALSE/TOGGLE" 
            : tip == "TF" ? "TRUE or FALSE" 
            : tip == "C" ? "COLOR" 
            : tip);
        		
  }

  private ScriptToken() {
  }

  private ScriptToken(String tip) {
    this.tip = tip;
    this.description = "";
  }

  private ScriptToken(String tip, String description) {
    this.tip = tip;
    this.description = "-- " + description;
  }

  private static Map<String, ScriptToken> htParams;

  private static Map<String, ScriptToken> getParams() {
    if (htParams == null) {
      htParams = new Hashtable<String, ScriptToken>();
      for (ScriptToken item : values())
        htParams.put(item.name(), item);
    }
    return htParams;
  }
  
  public static ScriptToken getScriptToken(String name) {
    ScriptToken st = getParams().get(name.toUpperCase());
    return (st == null ? UNKNOWN : st);
  }

  public static Lst<ScriptToken> getScriptTokenList(String name,
                                                     boolean isExact) {
    if (name != null)
      name = name.toUpperCase();
    Lst<ScriptToken> list = new Lst<ScriptToken>();
    if (isExact) {
      ScriptToken st = getScriptToken(name);
      if (st != null)
        list.addLast(st);
    } else {
      for (Entry<String, ScriptToken> entry : getParams().entrySet())
        if ((name == null || entry.getKey().startsWith(name)) 
            && entry.getValue().tip != null)
          list.addLast(entry.getValue());
    }
    return list;
  }

  /**
   * tweak command options depending upon special cases
   * 
   * @param st
   * @param params
   * @param cmd
   * @return adjusted value
   */
  public static String getValue(ScriptToken st, ScriptTokenizer params,
                                String cmd) {
    if (!params.hasMoreTokens())
      return "";
    switch (st) {
    default:
      return ScriptTokenizer.nextStringToken(params, true);
    case CLOSE:
    case GETPROPERTY:
    case INTEGRATION:
    case INTEGRATE:
    case JMOL:
    case LABEL:
    case LOAD:
    case PEAK:
    case PLOTCOLORS:
    case YSCALE:
    case WRITE:
      // take full command
      return removeCommandName(cmd);
    case SELECT:
    case OVERLAY: // deprecated
    case VIEW:
    case ZOOM:
      // commas to spaces
      return removeCommandName(cmd).replace(',', ' ').trim();
    }
  }

  private static String removeCommandName(String cmd) {
    int pt = cmd.indexOf(" ");
    if (pt < 0)
      return "";
    return cmd.substring(pt).trim();
  }

  public static String getKey(ScriptTokenizer eachParam) {
    String key = eachParam.nextToken();
    if (key.startsWith("#") || key.startsWith("//"))
      return null;
    if (key.equalsIgnoreCase("SET"))
      key = eachParam.nextToken();
    return key.toUpperCase();
  }

  /**
   * read a string for possibly quoted tokens separated by space until // or #
   * is reached.
   * 
   * @param value
   * @return list of tokens
   */
  public static Lst<String> getTokens(String value) {
		if (value.startsWith("'") && value.endsWith("'"))
			value = "\"" + PT.trim(value, "'") + "\"";
    Lst<String> tokens = new Lst<String>();
    ScriptTokenizer st = new ScriptTokenizer(value, false);
    while (st.hasMoreTokens()) {
      String s = ScriptTokenizer.nextStringToken(st, false);
      if (s.startsWith("//") || s.startsWith("#"))
        break;
      tokens.addLast(s);
    }
    return tokens;
  }

  public static String getNameList(Lst<ScriptToken> list) {
    if (list.size() == 0)
      return "";
    SB sb = new SB();
    for (int i = 0; i < list.size(); i++)
      sb.append(",").append(list.get(i).toString());
    return sb.toString().substring(1);
  }

	public String getDescription() {
		return description;
	}

}

/* $RCSfile$
 * $Author jonathan gutow$
 * Updated Dec. 2015 by Angel Herraez
 * valid for JSmol
 *
 * Copyright (C) 2005-2016  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */
package org.openscience.jmol.app.webexport;

import org.jmol.i18n.GT;

import javajs.util.CU;
import javajs.util.P3;

class Widgets { 
  
  // group of javascript widgets to allow user input to Jmol

  Widget[] widgetList = new Widget[5];

  Widgets() {
    // this should just be a list of available widgets
    widgetList[0] = new SpinOnWidget();
    widgetList[1] = new BackgroundColorWidget();
    widgetList[2] = new StereoViewWidget();
    widgetList[3] = new AnimationWidget();
    widgetList[4] = new ConsoleWidget();
    // widgetList[3] = new DownLoadWidget();
  }

  abstract class Widget {
    String name;

    /**
     * 
     * Each Widget must implement this function and make sure to use
     * the appletID number to specify the target applet i.e. "JmolApplet(appletID)".
     * NOTE anything that must be translated in the web page should be wrapped in both a call to
     * GT.escapeHTML and GT._ as in the following: GT.escapeHTML(GT._("text to translate"))
     * @param appletID
     * @param instance
     * @return  the JavaScript and html to implement the widget
     */
    abstract String getJavaScript(int appletID, JmolInstance instance);

    /**
     *  
     *  A COPY OF THIS .JS FILE MUST BE STORED IN THE html PART OF WEBEXPORT
     *  
     * @return  "none" (no file needed) or javascript file necessary to implement the widget
     */
    abstract String getJavaScriptFileName();// returns the name of the

    /**
     * The list of files returned by this function should contain the full path to
     * each file.  The only exception is that files that only contain a filename
     * will be assumed to be in the html section of WebExport.
     * @return string of filenames.
     */
    abstract String[] getSupportFileNames();// returns array of support file names.
    //These file names should include the full path within the Java application jar.
    //files stored in html part of WebExport will be found even if only the filename
    //is given.

  }

  class SpinOnWidget extends Widget {
    SpinOnWidget() {
      name = GT._("Spin on/off");
    }
    @Override
    String[] getSupportFileNames(){
      String[] fileNames = new String[0];
      return(fileNames);
    }
    @Override
    String getJavaScriptFileName() {
      return "none";
    }

    @Override
    String getJavaScript(int appletID, JmolInstance instance) {
      return "<input type=\"checkbox\""
          + " id=\"JmolSpinWidget" + appletID + "\""
          + (instance.spinOn ? " checked=\"\"" : "")
          + " onclick=\"Jmol.scriptWait(window[\'jmolApplet" + appletID + "\'], "
          + "\'spin \' + (this.checked ? \'on\' : \'off\'));\" "
          
          + "title=\"" + GT.escapeHTML(GT._("enable/disable spin")) + "\" />"
          + "<label for=\"JmolSpinWidget" + appletID + "\">" 
          + GT.escapeHTML(GT._("Spin on")) + "</label>";
    }
  }

  class BackgroundColorWidget extends Widget {
    BackgroundColorWidget() {
      name = GT._("Background Color");
    }

    @Override
    String getJavaScriptFileName() {
      return ("JmolColorPicker.js");
    }
    
    @Override
    String[] getSupportFileNames(){
      String[] fileNames = new String[0];
      return(fileNames);
    }
    
    @Override
    String getJavaScript(int appletID, JmolInstance instance) {
      P3 ptRGB = CU.colorPtFromInt(instance.bgColor, null);
      return "<table><tbody><tr><td>"
          + GT.escapeHTML(GT._("background color:"))
          + "</td><td style='min-width:70px;'><script type='text/javascript'>"
          + "var scriptStr = 'color background $COLOR$;';"
          + "jmolColorPickerBox(scriptStr, [" 
          + (int)ptRGB.x + "," + (int)ptRGB.y + "," + (int)ptRGB.z
          + "], 'backbox"
          + appletID + "',  '" + appletID + "');"
          + "</script></td></tr></tbody></table>";
    }
  }

  class StereoViewWidget extends Widget {
    StereoViewWidget() {
      name = GT._("Stereo Viewing");
    }

    @Override
    String getJavaScriptFileName() {
      return "none";
    }
    
    @Override
    String[] getSupportFileNames(){
      String[] fileNames = new String[0];
      return(fileNames);
    }
    
    @Override
    String getJavaScript(int appletID, JmolInstance instance) {
      return "<select id=\"StereoMode" + appletID + "\" title=\""
          + GT.escapeHTML(GT._("select stereo type")) + "\""
          + "onchange=\"void(Jmol.scriptWait(window[\'jmolApplet" + appletID + "\'], "
          + "(this.options[this.selectedIndex]).value));\">"
          + "\n<option selected=\"\" value=\"stereo off\">"
          + GT.escapeHTML(GT._("Stereo Off")) + " </option>"
          + "\n<option value=\"stereo REDBLUE\">" + GT.escapeHTML(GT._("Red/Blue"))
          + "</option>"
          + "\n<option value=\"stereo REDCYAN\">" + GT.escapeHTML(GT._("Red/Cyan"))
          + "</option>"
          + "\n<option value=\"stereo REDGREEN\">" + GT.escapeHTML(GT._("Red/Green"))
          + "</option>"
          + "\n</select>";
    }
  }
  class AnimationWidget extends Widget {
    AnimationWidget() {
      name = GT._("Animation Control");
    }

    @Override
    String getJavaScriptFileName() {
      return ("JmolAnimationCntrl.js");
    }

    @Override
    String[] getSupportFileNames(){
      String[] fileNames = new String[9];
      String imagePath = "org/openscience/jmol/app/images/";
      fileNames[0] = imagePath+"lastButton.png";
      fileNames[1] = imagePath+"playButton.png";
      fileNames[2] = imagePath+"playLoopButton.png";
      fileNames[3] = imagePath+"playOnceButton.png";
      fileNames[4] = imagePath+"playPalindromeButton.png";
      fileNames[5] = imagePath+"prevButton.png";
      fileNames[6] = imagePath+"pauseButton.png";
      fileNames[7] = imagePath+"nextButton.png";
      fileNames[8] = imagePath+"firstButton.png";
      return(fileNames);
    }
    
    @Override
    String getJavaScript(int appletID, JmolInstance instance) {
      String jsString = "<div class=\"AnimBox\">"
       + "<div style=\"text-align:center\">" + GT.escapeHTML(GT._("Animation")) + "</div>"
       + "<div>"
       + "<button title=\"" + GT.escapeHTML(GT._("First Frame"))
       + "\" onclick=\"void(Jmol.scriptWait(window[\'jmolApplet" + appletID + "\'], \'frame rewind\'));\">"
       + "<img src=\"firstButton.png\"></button>"
       + "<button title=\"" + GT.escapeHTML(GT._("Previous Frame"))
       + "\" onclick=\"void(Jmol.scriptWait(window[\'jmolApplet" + appletID + "\'], \'frame previous\'));\">"
       + "<img src=\"prevButton.png\"></button>"   
       + "<button title=\"" + GT.escapeHTML(GT._("Play"))
       + "\" onclick=\"void(Jmol.scriptWait(window[\'jmolApplet" + appletID + "\'], \'frame play\'));\">"
       + "<img src=\"playButton.png\"></button>"  
       + "<button title=\"" + GT.escapeHTML(GT._("Next Frame"))
       + "\" onclick=\"void(Jmol.scriptWait(window[\'jmolApplet" + appletID + "\'], \'frame next\'));\">"
       + "<img src=\"nextButton.png\"></button>"
       + "<button title=\"" + GT.escapeHTML(GT._("Pause"))
       + "\" onclick=\"void(Jmol.scriptWait(window[\'jmolApplet" + appletID + "\'], \'frame pause\'));\">"
       + "<img src=\"pauseButton.png\"></button>"
       + "<button title=\"" + GT.escapeHTML(GT._("Last Frame"))
       + "\" onclick=\"void(Jmol.scriptWait(window[\'jmolApplet" + appletID + "\'], \'frame last\'));\">"
       + "<img src=\"lastButton.png\"></button>"
       + "</div>"
       + "<div>"
       + "<span>" + GT.escapeHTML(GT._("Mode:"))+"</span>"
       + "<span id=\"jmol_loop_" + appletID + "\"><button title=\"" + GT.escapeHTML(GT._("Loop"))
       + "\" onclick=\"jmol_animationmode(\'loop\'," + appletID + ");\">"
       + "<img src=\"playLoopButton.png\"></button></span>"
       + "<span id=\"jmol_palindrome_" + appletID + "\"><button title=\"" + GT.escapeHTML(GT._("Palindrome"))
       + "\" onclick=\"jmol_animationmode(\'palindrome\', " + appletID + ");\">"
       + "<img src=\"playPalindromeButton.png\"></button></span>"
       + "<span class=\"jmol_playDefault\""
       + " id=\"jmol_playOnce_" + appletID + "\"><button title=\"" + GT.escapeHTML(GT._("Play Once"))
       + "\" onclick=\"jmol_animationmode(\'playOnce\', " + appletID + ");\">"
       + "<img src=\"playOnceButton.png\"></button></span>"
       + "</div>"
       + "</div>";
      return (jsString);
    }
    
   }
 
  class ConsoleWidget extends Widget {
    ConsoleWidget() {
      name = GT._("Open Console Button");
    }

    @Override
    String getJavaScriptFileName() {
      return ("none");
    }
    
    @Override
    String[] getSupportFileNames(){
      String[] fileNames = new String[0];
      return(fileNames);
    }

    @Override
    String getJavaScript(int appletID, JmolInstance instance) {
      return ("<button title=\"" + GT.escapeHTML(GT._("launch Jmol console"))
          + "\" onclick=\"Jmol.script(window[\'jmolApplet" + appletID + "\'], \'console\');\">"
          + GT.escapeHTML(GT._("Jmol Console")) + "</button>");
    }
  }
  
  class DownLoadWidget extends Widget { //not yet implemented
    DownLoadWidget() {
      name = GT._("Download view");
    }

    @Override
    String getJavaScriptFileName() {
      // TODO
      return ("none");
    }
    
    @Override
    String[] getSupportFileNames(){
      String[] fileNames = new String[0];
      return(fileNames);
    }

    @Override
    String getJavaScript(int appletID, JmolInstance instance) {
      // TODO
      return (GT._("unimplemented"));
    }
  }

}

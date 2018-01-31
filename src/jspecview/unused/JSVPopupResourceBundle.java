/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2013-10-03 08:00:44 -0500 (Thu, 03 Oct 2013) $
 * $Revision: 18771 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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
package jspecview.unused;



import javajs.util.SB;

public class JSVPopupResourceBundle extends PopupResource {

  @Override
  public String getMenuName() {
    return "appMenu"; 
  }
  
  public JSVPopupResourceBundle() {
    super();
  }

  @Override
  protected void buildStructure() {
    addItems(menuContents);
    addItems(structureContents);
  }
    
  private static String[][] menuContents = {
    
      {  "appMenu", "_SIGNED_FileMenu Toggle_Grid Toggle_X_Axis Toggle_Y_Axis Toggle_Coordinates Reverse_Plot" +
        " - Next_View Previous_View Clear_Views Reset_View Set_Zoom..." +
        " - Views... Overlay_Offset... Script..." +
        " - Properties" },

      { "appletMenu",
          "_SIGNED_FileMenu OptionsMenu ZoomMenu Views... Overlay_Offset..." +
          " - Measurements Peaks Integration Toggle_Trans/Abs Predicted_Solution_Colour" +
          " - Script... - Print... - AboutMenu" },

      {   "_SIGNED_FileMenu", "Open_File... Open_Simulation... Open_URL... - Append_File... Append_Simulation... Append_URL... - Save_AsMenu Export_AsMenu - Close_Views Close_Simulations Close_All" },

      {   "Save_AsMenu", "Original... JDXMenu CML XML(AnIML)" },
              
      {   "JDXMenu", "XY DIF DIFDUP FIX PAC SQZ" },

      {   "Export_AsMenu", "JPG PNG SVG PDF" },
              
      {   "OptionsMenu",
          "Toggle_Grid Toggle_X_Axis Toggle_Y_Axis Toggle_Coordinates Reverse_Plot Show_Header... Show_Overlay_Key..." }, //Window?

      {   "ZoomMenu", "Next_View Previous_View Reset_View Clear_Views Set_Zoom..." },

      {   "AboutMenu", "VERSION" }
};
  
  
  
  private static String[][] structureContents = {
  	{"Open_File...","load ?"},
  	{"Open_URL...","load http://?"},
  	{"Open_Simulation...","load $?"},
  	{"Append_File...","load append ?"},
  	{"Append_URL...","load append http://?"},
  	{"Append_Simulation...","load append $?; view \"1HNMR\""},
  	{"Close_All", "close all"},
  	{"Close_Views", "close views"},
  	{"Close Simulations", "close simulations"},
  	{"Show_Header...", "showProperties"},
  	{"Window","window"},
  	{"Show_Overlay_Key...","showKey"},
  	{"Next_View","zoom next;showMenu"},
  	{"Previous_View","zoom prev;showMenu"},
  	{"Clear_Views","zoom clear"},
  	{"Reset_View","zoom out"},
  	{"Views...","view"},
  	{"Overlay_Offset...","stackOffsetY"},
  	{"Script...","script INLINE"},
  	{"Set_Zoom...","zoom"},
  	{"Properties","showProperties"},
  	{"Toggle_X_Axis","XSCALEON toggle;showMenu"},
  	{"Toggle_Y_Axis","YSCALEON toggle;showMenu"},
  	{"Toggle_Grid","GRIDON toggle;showMenu"},
  	{"Toggle_Coordinates","COORDINATESON toggle;showMenu"},
  	{"Reverse_Plot","REVERSEPLOT toggle;showMenu"},
  	{"Measurements","SHOWMEASUREMENTS"},
  	{"Peaks","SHOWPEAKLIST"},
  	{"Integration","SHOWINTEGRATION"},
  	{"Toggle_Trans/Abs","IRMODE TOGGLE"},
  	{"Predicted_Solution_Colour","GETSOLUTIONCOLOR"},
  	{"Print...","print"},
  	{"Original...","write SOURCE"},
  	{"CML","write CML"},
  	{"XML(AnIML)","write XML"},
  	{"XY","write XY"},
  	{"DIF","write DIF"},
  	{"DIFDUP","write DIFDUP"},
  	{"FIX","write FIX"},
  	{"PAC","write PAC"},
  	{"SQZ","write SQZ"},
  	{"JPG","write JPG"},
  	{"SVG","write SVG"},
  	{"PNG","write PNG"},
  	{"PDF","write PDF"},
  };

  @Override
  protected String[] getWordContents() {
    
    //boolean wasTranslating = GT.setDoTranslate(true);
    String[] words = new String[] {
    };
 
    //GT.setDoTranslate(wasTranslating);
    return words;
  }
  
  @Override
  String getMenuAsText(String title) {
    return "# Jmol.mnu " + title + "\n\n" +
           "# Part I -- Menu Structure\n" +
           "# ------------------------\n\n" +
           dumpStructure(menuContents) + "\n\n" +
           "# Part II -- Key Definitions\n" +
           "# --------------------------\n\n" +
           dumpStructure(structureContents) + "\n\n" +
           "# Part III -- Word Translations\n" +
           "# -----------------------------\n\n" +
           dumpWords();
  }

  private String dumpWords() {
    String[] wordContents = getWordContents();
    SB s = new SB();
    for (int i = 0; i < wordContents.length; i++) {
      String key = wordContents[i++];
      if (structure.getProperty(key) == null)
        s.append(key).append(" | ").append(wordContents[i]).appendC('\n');
    }
    return s.toString();
  }
  
  private String dumpStructure(String[][] items) {
    String previous = "";
    SB s = new SB();
    for (int i = 0; i < items.length; i++) {
      String key = items[i][0];
      String label = words.getProperty(key);
      if (label != null)
        key += " | " + label;
      s.append(key).append(" = ")
       .append(items[i][1] == null ? previous : (previous = items[i][1]))
       .appendC('\n');
    }
    return s.toString();
  }
 

  
}

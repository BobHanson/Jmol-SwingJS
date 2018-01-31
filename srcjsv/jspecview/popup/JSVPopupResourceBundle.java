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
package jspecview.popup;

import org.jmol.popup.PopupResource;

public class JSVPopupResourceBundle extends PopupResource {

  @Override
  public String getMenuName() {
    return "appMenu"; 
  }
  
  public JSVPopupResourceBundle() {
    super(null, null);
  }

  @Override
  protected void buildStructure(String menuStructure) {
    addItems(menuContents);
    addItems(structureContents);
    if (menuStructure != null)
      setStructure(menuStructure, null);
  }
    
  private static String[][] menuContents = {
    
      {  "appMenu", "_SIGNED_FileMenu Spectra... ShowMenu OptionsMenu ZoomMenu - " +
      		"Integration Peaks Measurements - Script... Properties" },
      		
      { "appletMenu",
          "_SIGNED_FileMenu Spectra... - OptionsMenu ZoomMenu" +
          " - Integration Peaks Measurements" +
          " - Script... - Print... - AboutMenu" },

      {   "_SIGNED_FileMenu", "Open_File... Open_Simulation... Open_URL... - Add_File... Add_Simulation... Add_URL... - Save_AsMenu Export_AsMenu - Close_Views Close_Simulations Close_All" },

      {   "Save_AsMenu", "Original... JDXMenu CML XML(AnIML)" },
              
      {   "JDXMenu", "XY DIF DIFDUP FIX PAC SQZ" },

      {   "Export_AsMenu", "PDF - JAVAJPG PNG" },
              
      {   "ShowMenu", "Show_Header Show_Source Show_Overlay_Key" }, //Window?

      {   "OptionsMenu",
          "Toggle_Grid Toggle_X_Axis Toggle_Y_Axis Toggle_Coordinates Toggle_Trans/Abs Reverse_Plot - Predicted_Solution_Colour Fill_Solution_Colour_(all)  Fill_Solution_Colour_(none)" }, //Window?

      {   "ZoomMenu", "Next_Zoom Previous_Zoom Reset_Zoom - Set_X_Scale... Reset_X_Scale" },

      {   "AboutMenu", "VERSION" }
  };

  private static String[][] structureContents = {
  	{"Open_File...","load ?"},
  	{"Open_URL...","load http://?"},
  	{"Open_Simulation...","load $?"},
  	{"Add_File...","load append ?"},
  	{"Add_URL...","load append http://?"},
  	{"Add_Simulation...","load append $?; view \"1HNMR\""},
  	{"Close_All", "close all"},
  	{"Close_Views", "close views"},
  	{"Close Simulations", "close simulations"},
  	{"Show_Header", "showProperties"},
//  	{"Window","window"},
  	{"Show_Source","showSource"},
  	{"Show_Overlay_Key...","showKey"},
  	{"Next_Zoom","zoom next;showMenu"},
  	{"Previous_Zoom","zoom prev;showMenu"},
  	{"Reset_Zoom","zoom clear"},
  	{"Reset_X_Scale","zoom out"},
  	{"Set_X_Scale...","zoom"},
  	{"Spectra...","view"},
  	{"Overlay_Offset...","stackOffsetY"},
  	{"Script...","script INLINE"},
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
  	{"Fill_Solution_Colour_(all)","GETSOLUTIONCOLOR fillall"},
  	{"Fill_Solution_Colour_(none)","GETSOLUTIONCOLOR fillallnone"},
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
		return new String[] {};
	}

  @Override
  public String getMenuAsText(String title) {
    return getStuctureAsText(title, menuContents, structureContents);
  }


}

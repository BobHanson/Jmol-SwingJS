/* Copyright (c) 2002-2012 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General private
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General private License for more details.
 *
 *  You should have received a copy of the GNU Lesser General private
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

// CHANGES to 'JSVApplet.java' - Web Application GUI
// University of the West Indies, Mona Campus
//
// 09-10-2007 commented out calls for exporting data
//            this was causing security issues with JRE 1.6.0_02 and 03
// 13-01-2008 in-line load JCAMP-DX file routine added
// 22-07-2008 reinstated calls for exporting since Ok with JRE 1.6.0_05
// 25-07-2008 added module to predict colour of solution
// 08-01-2010 need bugfix for protected static reverseplot
// 17-03-2010 fix for NMRShiftDB CML files
// 11-06-2011 fix for LINK files and reverseplot 
// 23-07-2011 jak - Added parameters for the visibility of x units, y units,
//            x scale, and y scale.  Added parameteres for the font,
//            title font, and integral plot color.  Added a method
//            to reset view from a javascript call.
// 24-09-2011 jak - Added parameter for integration ratio annotations.
// 08-10-2011 jak - Add a method to toggle integration from a javascript
//          call. Changed behaviour to remove integration after reset
//          view.

package jspecview.app;

import java.util.Properties;

import jspecview.api.AppletFrame;

import org.jmol.api.JSVInterface;

/**
 * @author Bob Hanson
 */

public class JSVAppPro extends JSVApp implements
    JSVInterface {

  public JSVAppPro(AppletFrame appletFrame, boolean isJS) {
		super(appletFrame, isJS);
	}

	@Override
  public boolean isSigned() {
    return true;
  }
  
  @Override
  public boolean isPro() {
    return true;
  }
  
//  @Override
//  public void doAdvanced(String filePath) {
//    ((JSVAppletPro) appletFrame).doAdvanced(filePath);
//  }

  @Override
	public void exitJSpecView(boolean withDialog, Object frame) {
    appletFrame.doExitJmol();
  }

  @Override
  public void siProcessCommand(String script) {
    appletFrame.getApp().runScriptNow(script);
  }

  @Override
	public void saveProperties(Properties properties) {
  }

  @Override
	public void setProperties(Properties properties) {
  }

}

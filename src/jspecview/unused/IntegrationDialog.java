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

package jspecview.unused;
//
//import jspecview.dialog.DialogManager;
//import jspecview.dialog.JSVDialog;


/**
 * Dialog for managing the integral listing for a Spectrum within a GraphSet
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class IntegrationDialog {//extends JSVDialog {

//	private static final long serialVersionUID = 1L;
//
//	private static int[] posXY = new int[] { Integer.MIN_VALUE, 0 };
//
//	public IntegrationDialog() {
//		// called by reflection in JSViewer
//		this.type = AType.Integration;
//	}
//
//	@Override
//	public int[] getPosXY() {
//		return posXY;
//	}
//
//	@Override
//	public void addUniqueControls() {
//		txt1 = dialog.addTextField("Baseline Offset", "BaselineOffset", null, "%", ""
//				+ dialogParams.viewer.parameters.integralOffset, true);
//		txt2 = dialog.addTextField("Scale", "Scale", null, "%", ""
//				+ dialogParams.viewer.parameters.integralRange, true);
//
//		dialog.addButton("btnAuto", "Auto");
//		dialog.addButton("btnDelete", "Delete");
//		dialog.addButton("btnNormalize", "Normalize");
//	}
//
//	@Override
//	public boolean callback(String id, String msg) {
//		if (id.equals("btnAuto")) {
//			dialogParams.runScript("integrate auto");
//		} else if (id.equals("BaselineOffset")) {
//		} else if (id.equals("btnScale")) {
//			// TODO
//		} else if (id.equals("btnDelete")) {
//			dialogParams.deleteIntegral();
//		} else if (id.equals("btnNormalize")) {
//			try {
//				if (!dialogParams.checkSelectedIntegral())
//					return true;
//				String ret = manager.getDialogInput(dialog, "Enter a normalization factor", "Normalize",
//						DialogManager.QUESTION_MESSAGE, null, null, "" + dialogParams.lastNorm);
//				dialogParams.processEvent("int", Double.parseDouble(ret));
//			} catch (Exception ex) {
//				// for parseDouble
//			}
//		} else if (id.equals("Minimum")) {
//			try {
//				String ret = manager.getDialogInput(dialog, "Minimum value?",
//						"Set Minimum Value", DialogManager.QUESTION_MESSAGE, null, null, ""
//								+ dialogParams.lastMin);
//				dialogParams.processEvent("min", Double.parseDouble(ret));
//			} catch (Exception ex) {
//				// for parseDouble
//			}
//		} else {
//			return callbackAD(id, msg);
//		}
//		return true;
//	}
//
//	// from DialogParams:
//
//	@Override
//	public void applyFromFields() {
//		dialogParams.apply(new Object[] {dialog.getText(txt1), dialog.getText(txt2)});
//	}

}
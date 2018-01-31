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

package jspecview.dialog;

import javajs.util.DF;
import jspecview.common.IntegralData;


/**
 * Dialog for managing the integral listing for a Spectrum within a GraphSet
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class IntegrationDialog extends JSVDialog {

	private static int[] posXY = new int[] { Integer.MIN_VALUE, 0 };

	public IntegrationDialog() {
		// called by reflection in JSViewer
		this.type = AType.Integration;
	}

	@Override
	public int[] getPosXY() {
		return posXY;
	}

	@Override
	public void addUniqueControls() {
		txt1 = dialog.addTextField("txtBaselineOffset", "Baseline Offset", null, "%", ""
				+ vwr.parameters.integralOffset, true);
		txt2 = dialog.addTextField("txtScale", "Scale", null, "%", ""
				+ vwr.parameters.integralRange, true);
		// no luck getting this to work.
		//txt3 = dialog.addTextField("txtMinimum", "Minimum", null, "", "" + viewer.parameters.integralMinY, true);
		dialog.addButton("btnApply", "Apply");
		addApplyBtn = false;
		dialog.addButton("btnAuto", "Auto");
		dialog.addButton("btnDelete", "Delete");
		dialog.addButton("btnNormalize", "Normalize");
	}

	// from DialogParams:

	@Override
	public void applyFromFields() {
		apply(new Object[] {dialog.getText(txt1), dialog.getText(txt2)});//, dialog.getText(txt3)});
	}


	@Override
	public boolean callback(String id, String msg) {
		double val;
		try {
			if (id.equals("SHOWSELECTION")) {
				for (int i = 0; i < xyData.size(); i++)
					if (DF.formatDecimalDbl(xyData.get(i).getXVal(), 2).equals(msg)) {
						iSelected = i;
						jsvp.getPanelData().setXPointers(spec, xyData.get(i).getXVal(),
								spec, xyData.get(i).getXVal2());
						jsvp.doRepaint(true);
						break;
					}
				return true;
			}
			if (!id.equals("windowClosing") && !id.equals("FOCUS")) {
				if (id.equals("btnAuto") || xyData == null || xyData.size() == 0) {
					vwr.runScript("integrate auto");
					eventApply();
					return true;
				}
				setFocus(true);
			}
			if (id.equals("btnDelete")) {
				deleteIntegral();
			} else if (id.equals("btnNormalize")) {
				if (!checkSelectedIntegral())
					return true;
				String ret = manager.getDialogInput(dialog,
						"Enter a normalization factor", "Normalize",
						DialogManager.QUESTION_MESSAGE, null, null, "" + lastNorm);
				val = Double.parseDouble(ret);
				if (val > 0)
					((IntegralData) xyData).setSelectedIntegral(xyData.get(iSelected),
							lastNorm = val);
				eventApply();
			} else {
				return callbackAD(id, msg);
			}
		} catch (Exception ex) {
			// for parseDouble
		}
		return true;
	}

	
	private boolean checkSelectedIntegral() {
		if (iSelected < 0) {
			showMessage(
					"Select a line on the table first, then click this button.",
					"Integration", DialogManager.INFORMATION_MESSAGE);
			return false;
		}
		return true;
	}

	private void deleteIntegral() {
		if (!checkSelectedIntegral())
			return;
		xyData.removeItemAt(iSelected);
		iSelected = -1;
		iRowColSelected = -1;
		applyFromFields();
	}


}
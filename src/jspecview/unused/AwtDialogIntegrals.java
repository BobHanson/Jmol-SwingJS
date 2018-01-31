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

//import java.awt.Component;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//
//import javax.swing.JOptionPane;
//
//import jspecview.awt.DialogManager;
//import jspecview.common.JDXSpectrum;
//import jspecview.common.JSViewer;
//import jspecview.common.Annotation.AType;

/**
 * Dialog for managing the integral listing 
 * for a Spectrum within a GraphSet
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class AwtDialogIntegrals {//extends AwtDialogAnnotation {
//
//	private static final long serialVersionUID = 1L;
//	
//	private static int[] posXY = new int[] {Integer.MIN_VALUE, 0};
//	
//	public AwtDialogIntegrals(String title, JSViewer viewer, JDXSpectrum spec) {
//		super(title, viewer, spec, AType.Integration);
//	}
//
//	@Override
//	public void addUniqueControls(DialogManager dialogHelper) {
//		txt1 = dialogHelper.addInputOption("BaselineOffset", "Baseline Offset", null, "%",
//				"" + params.viewer.parameters.integralOffset, true);
//		txt2 = dialogHelper.addInputOption("Scale", "Scale", null, "%", ""
//				+ params.viewer.parameters.integralRange, true);
//		//chkResets = dialogHelper.addCheckBoxOption("BaselineResets", "Baseline Resets", true);
//		
//		dialogHelper.addButton("Auto", new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        params.viewer.runScript("integrate auto");
//      }
//    });
//    
//		dialogHelper.addButton("Delete", new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				params.deleteIntegral();
//			}
//    });
//
//		final Component me = this; 
//		dialogHelper.addButton("Normalize", new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				try {
//					if (!params.checkSelectedIntegral())
//						return;
//					String ret = (String) JOptionPane.showInputDialog(me,
//							"Enter a normalization factor", "Normalize",
//							JOptionPane.QUESTION_MESSAGE, null, null, "" + params.lastNorm);
//					params.processEvent("int", Double.parseDouble(ret));
//				} catch (Exception ex) {
//					// for parseDouble
//				}
//			}
//		});
//		
//		dialogHelper.addButton("Minimum", new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				try{
//					String ret = (String) JOptionPane.showInputDialog(me, "Minimum value?",
//							"Set Minimum Value", JOptionPane.QUESTION_MESSAGE, null, null, ""
//									+ params.lastMin);
//					params.processEvent("min", Double.parseDouble(ret));
//					} catch (Exception ex) {
//						// for parseDouble
//					}
//			}
//		});
//	}
//
//	// from DialogParams:
//
//	@Override
//	public void applyFromFields() {
//			params.apply(new Object[] { txt1.getText(), txt2.getText() });
//	}
	
}
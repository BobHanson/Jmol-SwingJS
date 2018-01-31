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

//import java.util.Enumeration;
//
//import jspecview.api.JSVTreeNode;
//import jspecview.common.PanelNode;
//import jspecview.dialog.JSVDialog;
//
//import org.jmol.util.JmolList;
//import javajs.util.Parser;
//import org.jmol.util.SB;
//
//

/**
 * Dialog for managing overlaying spectra and closing files
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */
public class ViewsDialog {//extends JSVDialog {

//	private static final long serialVersionUID = 1L;
//	private JmolList<JSVTreeNode> treeNodes;	
//	private JmolList<Object> checkBoxes;
//	private Object closeSelectedButton;
//	private Object combineSelectedButton;
//	private Object viewSelectedButton;
//  
//	private static int[] posXY = new int[] {Integer.MIN_VALUE, 0};
//  
//	public ViewsDialog() {
//		// called by reflection in JSViewer
//		type = AType.Views;
//	}
//
//	@Override
//	public int[] getPosXY() {
//		return posXY;
//	}
//
//	@Override
//	protected void addUniqueControls() {
//    checkBoxes = new JmolList<Object>();
//    treeNodes = new JmolList<JSVTreeNode>();
//    dialog.addButton("btnSelectAll", "Select All");
//    dialog.addButton("btnSelectNone", "Select None");
//    viewSelectedButton = dialog.addButton("btnViewSelected", "View Selected");
//    combineSelectedButton = dialog.addButton("btnCombineSelected", "Combine Selected");
//    closeSelectedButton = dialog.addButton("btnCloseSelected", "Close Selected");
//    dialog.addButton("btnDone", "Done");
//    dialog.setPreferredSize(500, 350);
//    dialog.addCheckBox(null, null, 0, false); // resets iRow; sets to rightPanel
//    addCheckBoxes(viewer.spectraTree.getRootNode(), 0, true);
//    addCheckBoxes(viewer.spectraTree.getRootNode(), 0, false);
//  }
//
//	private void addCheckBoxes(JSVTreeNode rootNode, int level, boolean isViews) {
//		Enumeration<JSVTreeNode> enume = rootNode.children();
//    while (enume.hasMoreElements()) {
//      JSVTreeNode treeNode = enume.nextElement();
//    	PanelNode node = treeNode.getPanelNode();
//    	if (node.isView != isViews)
//    		continue;
//    	String title = node.toString();
//    	if (title.indexOf("\n") >= 0)
//    		title = title.substring(0, title.indexOf('\n'));
//    	String name = "chkBox" + treeNodes.size();
//    	Object cb = dialog.addCheckBox(name, title, level, node.isSelected);
//      treeNode.setIndex(treeNodes.size());
//    	treeNodes.addLast(treeNode);
//    	checkBoxes.addLast(cb);
//    	addCheckBoxes(treeNode, level + 1, isViews);
//    }
//	}
//
//	@Override
//	public void checkEnables() {
//		int n = 0;
//		for (int i = 0; i < checkBoxes.size(); i++) {
//			if (dialog.isSelected(checkBoxes.get(i)) && treeNodes.get(i).getPanelNode().jsvp != null) {
//				n++;
//			}
//		}
//		dialog.setEnabled(closeSelectedButton, n > 0);
//		dialog.setEnabled(combineSelectedButton, n > 1);
//		dialog.setEnabled(viewSelectedButton, n == 1);
//	}
//	
//	private boolean checking = false; 
//	
//	protected void check(String name) {
//		int i = Parser.parseInt(name.substring(name.indexOf("_") + 1));
//		JSVTreeNode node = treeNodes.get(i);
//		Object cb = checkBoxes.get(i);
//		boolean isSelected = dialog.isSelected(cb);
//		if (node.getPanelNode().jsvp == null) {
//			if (!checking && isSelected && dialog.getText(cb).startsWith("Overlay")) {
//				checking = true;
//				selectAll(false);
//				dialog.setSelected(cb, true);
//				node.getPanelNode().isSelected = true;
//				checking = false;
//			}
//			Enumeration<JSVTreeNode> enume = node.children();
//			while (enume.hasMoreElements()) {
//				JSVTreeNode treeNode = enume.nextElement();
//				dialog.setSelected(checkBoxes.get(treeNode.getIndex()), isSelected);
//				treeNode.getPanelNode().isSelected = isSelected;
//				node.getPanelNode().isSelected = isSelected;
//			}
//		} else {
//			// uncheck all Overlays
//			node.getPanelNode().isSelected = isSelected;
//		}
//		if (isSelected)
//			for (i = treeNodes.size(); --i >= 0;)
//				if (treeNodes.get(i).getPanelNode().isView != node.getPanelNode().isView) {
//					dialog.setSelected(checkBoxes.get(treeNodes.get(i).getIndex()), false);
//					treeNodes.get(i).getPanelNode().isSelected = false;
//				}
//		checkEnables();
//	}
//
//	protected void selectAll(boolean mode) {
//		for (int i = checkBoxes.size(); --i >= 0;) {
//			dialog.setSelected(checkBoxes.get(i), mode);
//			treeNodes.get(i).getPanelNode().isSelected = mode;
//		}
//		checkEnables();
//	}
//	
//	protected void combineSelected() {
//		SB sb = new SB();
//		for (int i = 0; i < checkBoxes.size(); i++) {
//			Object cb = checkBoxes.get(i);
//			PanelNode node = treeNodes.get(i).getPanelNode();
//			if (dialog.isSelected(cb) && node.jsvp != null) {
//				if (node.isView) {
//					viewer.setNode(node, true);
//					return;
//				}
//				String label = dialog.getText(cb);
//				sb.append(" ").append(label.substring(0, label.indexOf(":")));
//			}
//		}
//		viewer.execView(sb.toString().trim(), false);
//		layoutDialog();
//	}
//
//	protected void viewSelected() {
//		SB sb = new SB();
//		for (int i = 0; i < checkBoxes.size(); i++) {
//			Object cb = checkBoxes.get(i);
//			PanelNode node = treeNodes.get(i).getPanelNode();
//			if (dialog.isSelected(cb) && node.jsvp != null) {
//				if (node.isView) {
//					viewer.setNode(node, true);
//					return;
//				}
//				String label = dialog.getText(cb);
//				sb.append(" ").append(label.substring(0, label.indexOf(":")));
//			}
//		}
//		viewer.execView(sb.toString().trim(), false);
//		layoutDialog();
//	}
//
//	protected void closeSelected() {
//		viewer.runScript("close !selected");
//    layoutDialog();
//	}
//
//	@Override
//	public boolean callback(String id, String msg) {
//		if (id.equals("btnSelectAll")) {
//      selectAll(true);
//		} else if (id.equals("btnSelectNone")) {
//      selectAll(false);
//		} else if (id.equals("btnViewSelected")) {
//      viewSelected();
//		} else if (id.equals("btnCombineSelected")) {
//      combineSelected();
//		} else if (id.equals("btnCloseSelected")) {
//      closeSelected();
//		} else if (id.equals("btnDone")) {
//			dispose();
//			dialogParams.done();
//		} else {
//			return callbackAD(id, msg);
//		}
//		return true;
//	}
//
//	@Override
//	public void applyFromFields() {
//		// n/a
//	}

}
